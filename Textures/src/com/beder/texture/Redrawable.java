package com.beder.texture;

import java.awt.event.MouseListener;

public interface Redrawable extends MouseListener {
	public abstract void applyImage(ImagePair pair);
	public abstract int getRes();
}
