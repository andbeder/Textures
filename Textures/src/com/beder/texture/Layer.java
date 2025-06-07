package com.beder.texture;

import java.awt.FlowLayout;
import java.awt.event.MouseListener;

import javax.security.auth.Refreshable;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Layer {
	private ImagePair input;
	private ImagePair output;
	private Operation op;
	private Parameters param;
	private JPanel tilePanel;
	
	public Layer(Operation op) {
		this.op = op;
		
		tilePanel = new JPanel(new FlowLayout());
		param = new Parameters();
		tilePanel.setBorder(BorderFactory.createTitledBorder(op.getTitle()));
	}

	public Layer(Operation op, ImagePair input) {
		this(op);
		this.input = input;
	}
	
	public ImagePair apply(ImagePair input) {
	    this.input = input.copy();
	    ImagePair out = op.executeOperation(this.input, param);
	    output = out.copy();
	    return out;
	}	
	
	public JPanel getTilePanel() {
	    JPanel rebuiltPanel = new JPanel(new FlowLayout());
	    rebuiltPanel.setBorder(BorderFactory.createTitledBorder(op.getTitle()));
	    for (String key : param.keySet()) {
	        if (!key.equals("Seed")) {
	            rebuiltPanel.add(new JLabel(key));
	            rebuiltPanel.add(new JLabel(Double.toString(param.get(key, 0))));
	        }
	    }
	    return rebuiltPanel;
	}

	public ImagePair getInput() {
		return input;
	}

	public ImagePair getOutput() {
		return output;
	}

	public Operation getOperation() {
		return op;
	}

	public Parameters getParam() {
		return param;
	}

	public void setParam(Parameters param) {
		this.param = param;
	}
	
	
}
