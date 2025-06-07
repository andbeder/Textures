package com.beder.texture.mask;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.beder.texture.ImagePair;
import com.beder.texture.Operation;
import com.beder.texture.Parameters;
import com.beder.texture.Redrawable;

public class CopyMask extends Operation {
	private JPanel optionsPanel;
	private JPanel tilePanel;

	public CopyMask(Redrawable redraw) {
		super(redraw);
		tilePanel = new JPanel(new FlowLayout());
        optionsPanel = new JPanel(new FlowLayout());
	}


	@Override
	public ImagePair executeOperation(ImagePair pair, Parameters par) {
        pair.right =  copyOf(pair.left);
        return pair;
	}


	@Override
	public String getDescription() {
		return "Copy: from left image";
	}

	@Override
	public String getTitle() {
		// TODO Auto-generated method stub
		return "Copy";
	}

	
}
