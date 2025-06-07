package com.beder.texture;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;

public abstract class Operation implements Comparable<Operation> {
	private Parameters param;
	private Redrawable redraw;
	private JPanel controlPanel;
	private Map<String, Component> controls;
	protected enum CONTROL_TYPE {INT, DOUBLE, SLIDER, SEED};
	
	public Operation(Redrawable redraw){
		this.redraw = redraw;
		controlPanel = new JPanel();
		controls = new TreeMap<String, Component>();
	}
	
	public void addParameter(String name, CONTROL_TYPE type, double def) {
		controlPanel.add(new JLabel(name));
		switch (type) {
		case INT:
			JTextField intField = new JTextField(String.format("%d",(int)def), 4);
			controls.put(name, intField);
			controlPanel.add(intField);
			break;
		case DOUBLE:
			JTextField doubleField = new JTextField(String.format("%.2f", def), 5);
			controls.put(name, doubleField);
			controlPanel.add(doubleField);
			break;
	    case SLIDER:
	        JSlider slider = new JSlider(0, 100, (int) def);
	        slider.setMajorTickSpacing(20);
	        slider.setMinorTickSpacing(5);
	        slider.setPaintTicks(true);
	        slider.setPaintLabels(true);
	        controls.put(name, slider);
	        controlPanel.add(slider);
	        break;
		case SEED:
			JTextField seedField = new JTextField(String.format("%d", (long)def), 8); // FIX: store to seedField
		    controls.put(name, seedField);
		    controlPanel.add(seedField);
		    JButton randomSeedButton = new JButton("Random");
		    randomSeedButton.addActionListener(e -> {
		        String newSeed = String.valueOf(new Random().nextInt(Integer.MAX_VALUE));
		        seedField.setText(newSeed);
		    });
		    controlPanel.add(randomSeedButton);
		    break;
		default:
			break;
		}
	}

	/****
	 * @return the current set of values chosen as parameters for this operation
	 */
	public final Parameters getUIParameters() {
	    Parameters param = new Parameters();
	    for (Map.Entry<String,Component> entry : controls.entrySet()) {
	        String name = entry.getKey();
	        Component c = entry.getValue();
	        try {
	            if (c instanceof JTextField) {
	                JTextField tf = (JTextField)c;
	                param.put(name, Double.parseDouble(tf.getText()));
	            } else if (c instanceof JSlider) {
	                JSlider s = (JSlider)c;
	                param.put(name, (double)s.getValue());
	            }
	        } catch (NumberFormatException e) {
	            System.err.println("Invalid input for parameter: " + name);
	        }
	    }
	    return param;
	}
	
	/**
     * Applies this operation to the given input image pair and returns same pair for further operations to be applied to it.
     */
	public abstract ImagePair executeOperation(ImagePair input, Parameters par);


	/**
     * Returns a textual description of the operation and its parameters.
     */
	public abstract String getDescription();
    
    /**
     * Applies this operation to the given input image and returns a new image.
     */
	public final JPanel getConfig() {
		return controlPanel;
	}

	
	public abstract String getTitle();
    
    /**
     * Called by parent GUI to show Swing controls for configuration parameters for that specific operation
     */

    /**
     * Applies this operation to the given input image and returns a new image.
     */
    public BufferedImage copyOf(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        java.awt.Graphics g = copy.getGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }

	@Override
	public int compareTo(Operation o) {
		int hash1 = System.identityHashCode(this);
		int hash2 = System.identityHashCode(o);
		return hash1 - hash2;
	}

	public Redrawable getRedraw() {
		return redraw;
	}
	
	
}
