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

/**
 * Generates a true Voronoi noise image with toroidal wrapping.
 * The number of seed points and seed are user-configurable.
 */
public class VoronoiNoiseGenerator extends NoiseOperation {
    private final static String PARAM_POINTS = "Points";

    public VoronoiNoiseGenerator(Redrawable redraw) {
        super(redraw);
        addParameter(PARAM_POINTS, CONTROL_TYPE.DOUBLE, 20);
    }

    @Override
    public BufferedImage generateNoise(Parameters param, long seed) {
        int res = getRedraw().getRes();
        int points = (int) param.get(PARAM_POINTS, 20);
        return generateVoronoi(res, points, new Random(seed));
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getTitle() {
        return "Voronoi";
    }

    public static BufferedImage generateVoronoi(int res, int numPoints, Random rand) {
        List<Coordinate> seedCoords = new ArrayList<>();
        Map<Coordinate, VoronoiNode> coordToNode = new HashMap<>();

        // Generate random seed points over the image.
        for (int i = 0; i < numPoints; i++) {
            float x = rand.nextFloat() * res;
            float y = rand.nextFloat() * res;
            VoronoiNode node = new VoronoiNode(x, y);
            Coordinate coord = new Coordinate(x, y);
            seedCoords.add(coord);
            coordToNode.put(coord, node);
        }

        // Compute the Delaunay triangulation.
        DelaunayTriangulationBuilder builder = new DelaunayTriangulationBuilder();
        builder.setSites(seedCoords);
        GeometryFactory gf = new GeometryFactory();
        Geometry edges = builder.getEdges(gf);

        // Build neighbor graph.
        for (int i = 0; i < edges.getNumGeometries(); i++) {
            LineString edge = (LineString) edges.getGeometryN(i);
            Coordinate[] coords = edge.getCoordinates();
            if (coords.length >= 2) {
                VoronoiNode a = coordToNode.get(coords[0]);
                VoronoiNode b = coordToNode.get(coords[1]);
                if (a != null && b != null) {
                    a.neighbors.add(b);
                    b.neighbors.add(a);
                }
            }
        }

        // Collect all nodes.
        List<VoronoiNode> nodes = new ArrayList<>(coordToNode.values());

        // Color the nodes using dynamic ordering and forward checking.
        boolean success = VoronoiColoring.colorGraphBacktracking(nodes);
        if (!success) {
            System.out.println("❌ Could not find valid 4-coloring for Voronoi.");
        }

        // Rasterize the diagram using toroidal (wrapped) distance.
        BufferedImage img = new BufferedImage(res, res, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < res; y++) {
            for (int x = 0; x < res; x++) {
                VoronoiNode closest = null;
                double bestDist = Double.MAX_VALUE;
                for (VoronoiNode node : nodes) {
                    double d = wrappedDistance(node, x, y, res);
                    if (d < bestDist) {
                        bestDist = d;
                        closest = node;
                    }
                }
                int color;
                switch (closest.color) {
                    case 0:
                        color = 0xFF000000; // Black
                        break;
                    case 1:
                        color = 0xFFFF0000; // Red
                        break;
                    case 2:
                        color = 0xFF00FFFF; // Cyan
                        break;
                    case 3:
                        color = 0xFF0000FF; // Blue
                        break;
                    default:
                        color = 0xFFFFFFFF; // Fallback (white)
                        break;
                }
                img.setRGB(x, y, color);
            }
        }
        return img;
    }

    /**
     * Computes the toroidal (wrapped) Euclidean distance between a Voronoi node and a point (x, y).
     *
     * @param node The Voronoi node.
     * @param x    The x-coordinate of the point.
     * @param y    The y-coordinate of the point.
     * @param res  The resolution (width/height) of the image.
     * @return The wrapped distance.
     */
    private static double wrappedDistance(VoronoiNode node, int x, int y, int res) {
        double dx = Math.abs(node.x - x);
        if (dx > res / 2.0) {
            dx = res - dx;
        }
        double dy = Math.abs(node.y - y);
        if (dy > res / 2.0) {
            dy = res - dy;
        }
        return Math.sqrt(dx * dx + dy * dy);
    }

    // --- Voronoi Node (similar to Cell Noise's Node) ---
    public static class VoronoiNode {
        public float x, y;
        public int color = -1;
        public List<VoronoiNode> neighbors = new ArrayList<>();
        public List<Integer> domain;
        public VoronoiNode(float x, float y) {
            this.x = x;
            this.y = y;
        }
        public boolean isSafeColor(int c) {
            for (VoronoiNode n : neighbors) {
                if (n.color == c) return false;
            }
            return true;
        }
    }

    // --- Voronoi Coloring Algorithm ---
    public static class VoronoiColoring {

        public static boolean colorGraphBacktracking(List<VoronoiNode> nodes) {
            for (VoronoiNode node : nodes) {
                node.domain = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
                node.color = -1;
            }
            return backtrack(nodes);
        }

        private static boolean backtrack(List<VoronoiNode> nodes) {
            boolean complete = true;
            for (VoronoiNode node : nodes) {
                if (node.color == -1) {
                    complete = false;
                    break;
                }
            }
            if (complete) return true;
            VoronoiNode node = selectUnassignedNode(nodes);
            if (node == null) return false;
            List<Integer> domainCopy = new ArrayList<>(node.domain);
            for (int candidate : domainCopy) {
                if (!isConsistent(node, candidate)) continue;
                node.color = candidate;
                List<NeighborChange> changes = new ArrayList<>();
                boolean failure = false;
                for (VoronoiNode neighbor : node.neighbors) {
                    if (neighbor.color == -1 && neighbor.domain.contains(candidate)) {
                        neighbor.domain.remove((Integer) candidate);
                        changes.add(new NeighborChange(neighbor, candidate));
                        if (neighbor.domain.isEmpty()) {
                            failure = true;
                            break;
                        }
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

        private static boolean isConsistent(VoronoiNode node, int color) {
            for (VoronoiNode neighbor : node.neighbors) {
                if (neighbor.color == color) return false;
            }
            return true;
        }

        private static VoronoiNode selectUnassignedNode(List<VoronoiNode> nodes) {
            VoronoiNode best = null;
            int minDomainSize = Integer.MAX_VALUE;
            for (VoronoiNode node : nodes) {
                if (node.color == -1 && node.domain.size() < minDomainSize) {
                    minDomainSize = node.domain.size();
                    best = node;
                }
            }
            return best;
        }

        private static class NeighborChange {
            VoronoiNode node;
            int removedColor;
            NeighborChange(VoronoiNode node, int removedColor) {
                this.node = node;
                this.removedColor = removedColor;
            }
        }
    }
}
