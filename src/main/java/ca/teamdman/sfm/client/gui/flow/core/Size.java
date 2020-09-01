package ca.teamdman.sfm.client.gui.flow.core;

import ca.teamdman.sfm.common.flow.data.core.Position;

public class Size {
	private int width, height;
	public Size(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public Size(Size copy) {
		this(copy.width, copy.height);
	}

	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	/**
	 * Checks if a coordinate is contained in this element
	 *
	 * @param x Scaled x coordinate
	 * @param y Scaled y coordinate
	 * @return true if coordinate is contained in this element's area, false otherwise
	 */
	public boolean contains(Position myPosition, int x, int y) {
		return x >= myPosition.getX() && x <= myPosition.getX() + width && y >= myPosition.getY() && y <= myPosition.getY() + height;
	}

	public int getWidth() {
		return this.width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return this.height;
	}

	public void setHeight(int height) {
		this.height = height;
	}
}
