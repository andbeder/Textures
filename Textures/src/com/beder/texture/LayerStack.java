package com.beder.texture;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

public class LayerStack {
	private ArrayList<Layer> stack;
	int curPtr;
	private JPanel stackPanel;
	private JPanel thisPanel;
	private TextureGenius genius;
	
	public LayerStack(TextureGenius genius){
		this.genius = genius;
		stack = new ArrayList<Layer>();
		curPtr = -1;
		
		thisPanel = new JPanel();
		thisPanel.setLayout(new BoxLayout(thisPanel, BoxLayout.Y_AXIS));
		thisPanel.add(new JLabel("Operation Stack"));
		stackPanel = new JPanel(new MigLayout("wrap 1, fillx", "[grow]"));
		thisPanel.add(stackPanel);
	}
	
	public JPanel getStackPanel() {
		return thisPanel;
	}
	
	/***
	 *  Rebuilds all of the stack tiles on the UI
	 */
	public void buildStackPanel() {
		stackPanel.removeAll();
		for (Layer l : stack) {
	    	final Layer clickLayer = l;
			JPanel panel = l.getTilePanel();
			panel.setPreferredSize(new Dimension(200, 50));
			panel.addMouseListener(new MouseListener() {
			    @Override public void mouseClicked(MouseEvent e) {
			    	curPtr = stack.indexOf(clickLayer);
			    	//JOptionPane.showMessageDialog(panel, "Clicked on index " + curPtr);
			    	genius.newCurrent();
			    }
			    @Override public void mousePressed(MouseEvent e) {}
			    @Override public void mouseReleased(MouseEvent e) {}
			    @Override public void mouseEntered(MouseEvent e) {}
			    @Override public void mouseExited(MouseEvent e) {}
			});
			stackPanel.add(panel, "growx");
		}
		genius.getGUI().frame.pack();
	}

	/*****
	 * Adds a new operation AFTER the currentPtr.
	 *   Rebuilds the stackPanel
	 *   DOES NOT "apply"
	 * @param op
	 */

	public void add(Layer l) {
	    stack.add(++curPtr, l);
	    buildStackPanel(); // FIX: refresh panel
	    genius.getGUI().applyImage(l.getInput()); // FIX: show image
	}

	public Layer getCurrent() {
		if (curPtr < 0) {
			return null;
		} else {
			return stack.get(curPtr);
		}
	}
}
