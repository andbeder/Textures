package com.beder.texture.noise;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.beder.texture.ImagePair;
import com.beder.texture.Operation;
import com.beder.texture.Parameters;
import com.beder.texture.Redrawable;
import com.beder.util.OpenSimplex2S;

/**
 * Generates a grayscale noise image using the OpenSimplex algorithm, using the
 * provided seed for reproducibility.
 *
 * @param res   The width and height of the output square image.
 * @param scale A scale factor for the noise (larger values => larger features).
 * @param seed  The user-provided seed for consistent results.
 */

public class SimplexNoiseGenerator extends NoiseOperation {

	private final OpenSimplex2S noise;
	private final static String PARAM_SCALE = "Scale";

	public SimplexNoiseGenerator(Redrawable r) {
		super(r);
		this.noise = new OpenSimplex2S();
		
		addParameter(PARAM_SCALE, Operation.CONTROL_TYPE.INT, 200);
	}


	@Override
	public BufferedImage generateNoise(Parameters par, long seed) {
		double scale = par.get(PARAM_SCALE, 200);
		int res = getRedraw().getRes();
		
		BufferedImage img = new BufferedImage(res, res, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < res; y++) {
			for (int x = 0; x < res; x++) {
				double val = noise(x / scale, y / scale, seed);
				// Normalize from [-1,1] -> [0,1]
				val = (val + 1) / 2.0;
				int gray = (int) (val * 255);
				int color = 0xFF000000 | (gray << 16) | (gray << 8) | gray;
				img.setRGB(x, y, color);
			}
		}
		return img;
	}

	
	
	public double noise(double x, double y, long seed) {
		return OpenSimplex2S.noise2(seed, x, y);
	}

	@Override
	public String getDescription() {
		return "Generates a grayscale noise image using the OpenSimplex algorithm";
	}

	@Override
	public String getTitle() {
		return "Simplex";
	}
}
