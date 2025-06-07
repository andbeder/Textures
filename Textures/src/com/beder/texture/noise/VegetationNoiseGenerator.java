package com.beder.texture.noise;

import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.util.Random;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.beder.texture.Parameters;
import com.beder.texture.Redrawable;

public class VegetationNoiseGenerator extends NoiseOperation {
	
	private final static String PARAM_SEED_COUNT = "Seeds";
	private final static String PARAM_GROWTH = "Growth";
	private final static String PARAM_DEATH = "Death";
	private final static String PARAM_ITER = "Iterations";
	
	public VegetationNoiseGenerator(Redrawable redraw) {
		super(redraw);
		addParameter(PARAM_SEED_COUNT, CONTROL_TYPE.INT, 100);
		addParameter(PARAM_GROWTH, CONTROL_TYPE.DOUBLE, 0.5);
		addParameter(PARAM_DEATH, CONTROL_TYPE.DOUBLE, 0.2);
		addParameter(PARAM_ITER, CONTROL_TYPE.INT, 50);
	}


	@Override
	public String getDescription() {
	    return "Simulates vegetation growth over fertility map using CA";
	} // in VegetationNoiseGenerator.java

	@Override
	public String getTitle() {
		return "Vegetation";
	}

	@Override
    public BufferedImage generateNoise(Parameters param, long seed) {
        Random rand = new Random(seed);
        BufferedImage fertility = getInput().left;
        int res = getRedraw().getRes();
        int seedCount = (int) param.get(PARAM_SEED_COUNT, 100);
        double growth = param.get(PARAM_GROWTH, 0.5);
        double deathRate = param.get(PARAM_DEATH, 0.2);
        int duration = (int) param.get(PARAM_ITER, 50);
        
        int[][] current = new int[res][res];
        int[][] next = new int[res][res];

        // Seed initial vegetation
        for (int i = 0; i < seedCount; i++) {
            int x = rand.nextInt(res);
            int y = rand.nextInt(res);
            current[y][x] = 1;
        }

        // Run CA cycles
        for (int cycle = 0; cycle < duration; cycle++) {
            for (int y = 0; y < res; y++) {
                for (int x = 0; x < res; x++) {
                    int rgb = fertility.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >>  8) & 0xFF;
                    int b = (rgb      ) & 0xFF;
                    double fert = ((r + g + b) / 3.0) / 255.0;

                    if (current[y][x] == 1) {
                        // Alive cell: survival probability = 1 - deathRate * (1 - fert)
                        double survivalProb = 1 - deathRate * (1 - fert);
                        next[y][x] = (rand.nextDouble() < survivalProb) ? 1 : 0;
                    } else {
                        // Dead cell: may sprout if neighbors exist
                        int aliveNeighbors = countAliveNeighbors(current, x, y);
                        if (aliveNeighbors > 0 && rand.nextDouble() < fert * growth) {
                            next[y][x] = 1;
                        } else {
                            next[y][x] = 0;
                        }
                    }
                }
            }
            // Swap
            int[][] temp = current;
            current = next;
            next = temp;
        }

        // Render output
        BufferedImage output = new BufferedImage(res, res, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < res; y++) {
            for (int x = 0; x < res; x++) {
                int intensity = current[y][x] == 1 ? 255 : 0;
                int gray = (intensity << 16) | (intensity << 8) | intensity;
                output.setRGB(x, y, 0xFF000000 | gray);
            }
        }
        return output;
    }

    /**
     * Counts alive neighbors around (x, y) in a toroidal grid.
     */
    private int countAliveNeighbors(int[][] grid, int x, int y) {
        int res = getRedraw().getRes();
        int count = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = (x + dx + res) % res;
                int ny = (y + dy + res) % res;
                count += grid[ny][nx];
            }
        }
        return count;
    }
}

