package org.chesmapper.view.gui;

import javax.vecmath.Vector3f;

public interface Zoomable
{
	public Vector3f getCenter(boolean superimposed);

	public float getDiameter(boolean superimposed);

	public boolean isSuperimposed();
}
