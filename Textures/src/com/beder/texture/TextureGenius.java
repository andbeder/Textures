package com.beder.texture;

import javax.swing.JPanel;

/**
 * TextureGenius handles all control and logic: managing the operation stack,
 * current image state, and dirty/save/apply workflows.
 * It delegates UI rendering to TextureGUI.
 */
public class TextureGenius {
    private final int res;
    private final LayerStack stack;
    private ImagePair curImage;
    private boolean isDirty;
	private TextureGUI gui;

    public static void main(String[] args) {
        // Initialize logic and launch GUI
        TextureGenius genius = new TextureGenius(1024);
        genius.gui = new TextureGUI(genius);
        genius.gui.init();
    }

    public TextureGenius(int res) {
        this.res = res;
        this.stack = new LayerStack(this);
        this.curImage = new ImagePair(res);
        this.isDirty = false;
    }

    /**
     * Returns the configured resolution.
     */
    public int getRes() {
        return res;
    }

    /**
     * Returns true if there are no unapplied changes.
     */
    public boolean isClean() {
        return !isDirty;
    }

    /**
     * Provides the operations stack panel for embedding in the GUI.
     */
    public JPanel getStackPanel() {
        return stack.getStackPanel();
    }

    /**
     * Retrieves the currently selected operation for configuring its UI.
     */
    public Operation getCurrentOperation() {
        return stack.getCurrent().getOperation();
    }

    /**
     * Adds a new operation to the stack and marks the state as dirty.
     */
    public ImagePair addOperation(Operation op) {
    	Layer current = stack.getCurrent();
		ImagePair input = current == null ? new ImagePair(res) : current.getOutput();
		Layer l = new Layer(op, input);
		stack.add(l);
		stack.buildStackPanel();
		gui.applyImage(input);
        this.curImage = input;
        this.isDirty = true;
        return input;
    }

    /**
     * Applies the current operation (without saving), marking the state dirty.
     */
    public ImagePair applyCurrent() {
	    Layer l = stack.getCurrent();
	    // ← grab the sliders/textfields before we execute
	    Parameters p = l.getOperation().getUIParameters();
	    l.setParam(p);
	    ImagePair output = l.apply(l.getInput());        
	    this.curImage = output;
        this.isDirty = true;
        return output;
    }

    /**
     * Saves (applies permanently) the current operation and clears the dirty flag.
     */
    public ImagePair saveCurrent() {
        Layer l = stack.getCurrent();
        Parameters p = l.getOperation().getUIParameters();
        l.setParam(p);
        stack.buildStackPanel();
        ImagePair output = l.apply(l.getInput());
        this.curImage = output;
        this.isDirty = false;
        return output;
    }
    
    public void newCurrent() {
        Layer l = stack.getCurrent();
        if (l != null && l.getOutput() != null) {
            gui.applyImage(l.getOutput());
            gui.showOptions();
        }
    }

    /**
     * Returns the latest ImagePair to display.
     */
    public ImagePair getCurrentImage() {
        return curImage;
    }

	public TextureGUI getGUI() {
		return gui;
	}
}
