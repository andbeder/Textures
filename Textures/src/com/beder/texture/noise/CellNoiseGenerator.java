package com.beder.texture.noise;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;

import com.beder.texture.Parameters;
import com.beder.texture.Redrawable;

public class CellNoiseGenerator extends NoiseOperation {
    private final static String PARAM_FREQ = "Frequency";
    private final static String PARAM_GUAS = "Guassian";

    public CellNoiseGenerator(Redrawable redraw) {
        super(redraw);

        addParameter(PARAM_FREQ, CONTROL_TYPE.INT, 10);
        addParameter(PARAM_GUAS, CONTROL_TYPE.SLIDER, 40);
    }

    @Override
    public BufferedImage generateNoise(Parameters param, long seed) {
        int res = getRedraw().getRes();
        int cells = (int) param.get(PARAM_FREQ, 10);
        double mix = param.get(PARAM_GUAS, 40) / 100.0;
        return generateCellNoise(res, cells, mix, new Random(seed));
    }

     @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getTitle() {
        return "Cell Noise";
    }
    
    public static BufferedImage generateCellNoise(int res, int cells, double mix, Random rand) {
        int[][] noise = generateNoise(res, cells, mix, rand);
        BufferedImage img = new BufferedImage(res, res, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < res; y++) {
            for (int x = 0; x < res; x++) {
                int color;
                switch (noise[y][x]) {
                    case 0: color = 0xFF000000; break; // Black
                    case 1: color = 0xFFFF0000; break; // Red
                    case 2: color = 0xFF00FFFF; break; // Cyan
                    case 3: color = 0xFF0000FF; break; // Blue
                    default: color = 0xFFFFFFFF; break; // White fallback
                }
                img.setRGB(x, y, color);
            }
        }
        return img;
    }
    
    private static int[][] generateNoise(int res, int cells, double mix, Random rand) {
        float cellWidth = (float) res / cells;
        List<Coordinate> seedCoords = new ArrayList<>();
        Map<Coordinate, Node> coordToNode = new HashMap<>();
        
        for (int j = 0; j < cells; j++) {
            for (int i = 0; i < cells; i++) {
                float uniformX = i * cellWidth + rand.nextFloat() * cellWidth;
                float uniformY = j * cellWidth + rand.nextFloat() * cellWidth;
                float centerX = i * cellWidth + cellWidth / 2.0f;
                float centerY = j * cellWidth + cellWidth / 2.0f;
                float sigma = cellWidth / 6.0f;
                float gaussianX = centerX + (float)(rand.nextGaussian() * sigma);
                float gaussianY = centerY + (float)(rand.nextGaussian() * sigma);
                float x = (float)((1 - mix) * uniformX + mix * gaussianX);
                float y = (float)((1 - mix) * uniformY + mix * gaussianY);
                Node node = new Node(x, y);
                Coordinate coord = new Coordinate(x, y);
                seedCoords.add(coord);
                coordToNode.put(coord, node);
            }
        }
        
        DelaunayTriangulationBuilder builder = new DelaunayTriangulationBuilder();
        builder.setSites(seedCoords);
        GeometryFactory gf = new GeometryFactory();
        Geometry edges = builder.getEdges(gf);
        
        for (int i = 0; i < edges.getNumGeometries(); i++) {
            LineString edge = (LineString) edges.getGeometryN(i);
            Coordinate[] coords = edge.getCoordinates();
            if (coords.length >= 2) {
                Node a = coordToNode.get(coords[0]);
                Node b = coordToNode.get(coords[1]);
                if (a != null && b != null) {
                    a.neighbors.add(b);
                    b.neighbors.add(a);
                }
            }
        }
        
        List<Node> nodes = new ArrayList<>(coordToNode.values());
        boolean success = colorGraphBacktracking(nodes);
        if (!success) {
            System.out.println("❌ Could not find valid 4-coloring.");
        }
        int[][] out = new int[res][res];
        for (int y = 0; y < res; y++) {
            for (int x = 0; x < res; x++) {
                Node closest = null;
                double bestDist = Double.MAX_VALUE;
                for (Node node : nodes) {
                    double d = wrappedDistance(node, x, y, res);
                    if (d < bestDist) {
                        bestDist = d;
                        closest = node;
                    }
                }
                out[y][x] = closest.color;
            }
        }
        return out;
    }
    
   
    private static boolean colorGraphBacktracking(List<Node> nodes) {
        for (Node node : nodes) {
            node.domain = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
            node.color = -1;
        }
        return backtrack(nodes);
    }
    
    private static boolean backtrack(List<Node> nodes) {
        boolean complete = true;
        for (Node node : nodes) {
            if (node.color == -1) { complete = false; break; }
        }
        if (complete) return true;
        Node node = selectUnassignedNode(nodes);
        if (node == null) return false;
        java.util.List<Integer> domainCopy = new java.util.ArrayList<>(node.domain);
        for (int candidate : domainCopy) {
            if (!isConsistent(node, candidate)) continue;
            node.color = candidate;
            java.util.List<NeighborChange> changes = new java.util.ArrayList<>();
            boolean failure = false;
            for (Node neighbor : node.neighbors) {
                if (neighbor.color == -1 && neighbor.domain.contains(candidate)) {
                    neighbor.domain.remove((Integer) candidate);
                    changes.add(new NeighborChange(neighbor, candidate));
                    if (neighbor.domain.isEmpty()) { failure = true; break; }
                }
            }
            if (!failure && backtrack(nodes)) return true;
            for (NeighborChange change : changes) {
                change.node.domain.add(change.removedColor);
            }
            node.color = -1;
        }
        return false;
    }
    
    private static boolean isConsistent(Node node, int color) {
        for (Node neighbor : node.neighbors) {
            if (neighbor.color == color) return false;
        }
        return true;
    }
    
    private static Node selectUnassignedNode(java.util.List<Node> nodes) {
        Node best = null;
        int minDomainSize = Integer.MAX_VALUE;
        for (Node node : nodes) {
            if (node.color == -1 && node.domain.size() < minDomainSize) {
                minDomainSize = node.domain.size();
                best = node;
            }
        }
        return best;
    }
    
    private static class NeighborChange {
        Node node;
        int removedColor;
        NeighborChange(Node node, int removedColor) {
            this.node = node;
            this.removedColor = removedColor;
        }
    }
    
    private static double wrappedDistance(Node node, int x, int y, int res) {
        double dx = Math.abs(node.x - x);
        if (dx > res / 2.0) dx = res - dx;
        double dy = Math.abs(node.y - y);
        if (dy > res / 2.0) dy = res - dy;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    private static class Node {
        float x, y;
        int color = -1;
        java.util.List<Node> neighbors = new java.util.ArrayList<>();
        java.util.List<Integer> domain;
        Node(float x, float y) { this.x = x; this.y = y; }
    }
    
}
