package com.beder.texture;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class ImagePair {
	public BufferedImage left, right;
	
	private ImagePair() {
		
	} 

	public ImagePair(int res) {
	    left = new BufferedImage(res, res, BufferedImage.TYPE_INT_ARGB);
	    right = new BufferedImage(res, res, BufferedImage.TYPE_INT_ARGB);
	    Graphics2D g = left.createGraphics();
	    g.setColor(Color.BLACK);
	    g.fillRect(0, 0, left.getWidth(), left.getHeight());
	    g.dispose();
	    g = right.createGraphics();
	    g.setColor(Color.BLACK);
	    g.fillRect(0, 0, right.getWidth(), right.getHeight());
	    g.dispose();
	}
	
	public ImagePair(ImagePair old) {
		left = copyImage(old.left);
		right = copyImage(old.right);
	}
	
	public ImagePair copy() {
		ImagePair pair = new ImagePair();
		pair.left = copyImage(left);
		pair.right = copyImage(right);
		return pair;
	}
	
    public static BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        Graphics g = copy.getGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return copy;
    }

}
