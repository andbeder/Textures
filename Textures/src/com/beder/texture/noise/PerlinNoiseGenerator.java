package com.beder.texture.noise;

import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.beder.texture.Parameters;
import com.beder.texture.Redrawable;

/**
 * Generates a grayscale noise image using Perlin noise with multiple octaves.
 * Frequency and iteration count are configurable, and a seed ensures reproducibility.
 */
public class PerlinNoiseGenerator extends NoiseOperation {
    private static final String PARAM_FREQ = "Frequency";
    private static final String PARAM_ITER = "Iterations";

    public PerlinNoiseGenerator(Redrawable redraw) {
        super(redraw);
        addParameter(PARAM_FREQ, CONTROL_TYPE.INT, 4);
        addParameter(PARAM_ITER, CONTROL_TYPE.INT, 4);
    }


    @Override
    public BufferedImage generateNoise(Parameters par, long seed) {
        int res = getRedraw().getRes();
        double baseFreq = par.get(PARAM_FREQ, 4);
        int iterations = (int) par.get(PARAM_ITER, 4);
 
        // Build permutation table
        int[] perm = new int[256];
        for (int i = 0; i < 256; i++) perm[i] = i;
        Random rnd = new Random(seed);
        for (int i = 255; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int tmp = perm[i];
            perm[i] = perm[j];
            perm[j] = tmp;
        }
        // Duplicate
        int[] p = new int[512];
        for (int i = 0; i < 512; i++) p[i] = perm[i & 255];

        // Generate multi‑octave Perlin noise
        BufferedImage img = new BufferedImage(res, res, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < res; y++) {
            for (int x = 0; x < res; x++) {
                double amplitude = 1.0, frequency = baseFreq;
                double sum = 0, max = 0;
                for (int o = 0; o < iterations; o++) {
                    double nx = x * frequency / res, ny = y * frequency / res;
                    double n = perlin(nx, ny, p);
                    sum += n * amplitude;
                    max += amplitude;
                    amplitude *= 0.5;
                    frequency *= 2.0;
                }
                // Normalize to [0,255]
                double v = (sum / max + 1) * 0.5;
                int gray = (int)(v * 255);
                int color = 0xFF000000 | (gray<<16) | (gray<<8) | gray;
                img.setRGB(x, y, color);
            }
        }
        return img;
    }

    private double perlin(double x, double y, int[] p) {
        int X = (int)Math.floor(x) & 255, Y = (int)Math.floor(y) & 255;
        x -= Math.floor(x); y -= Math.floor(y);
        double u = fade(x), v = fade(y);

        int aa = p[p[X] + Y], ab = p[p[X] + Y + 1],
            ba = p[p[X + 1] + Y], bb = p[p[X + 1] + Y + 1];

        double gradAA = grad(aa, x, y);
        double gradBA = grad(ba, x - 1, y);
        double gradAB = grad(ab, x, y - 1);
        double gradBB = grad(bb, x - 1, y - 1);

        double lerpX1 = lerp(u, gradAA, gradBA);
        double lerpX2 = lerp(u, gradAB, gradBB);
        return lerp(v, lerpX1, lerpX2);
    }

    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private double grad(int hash, double x, double y) {
        int h = hash & 7;
        double u = (h < 4) ? x : y;
        double v = (h < 4) ? y : x;
        return (((h & 1) == 0) ? u : -u) + (((h & 2) == 0) ? v : -v);
    }

    @Override
    public String getDescription() {
        return "Generates Perlin noise with configurable frequency, octaves, and seed";
    }

    @Override
    public String getTitle() {
        return "Perlin";
    }
}
