/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ca.teamdman.sfm.client.gui.flow.impl.util;

import ca.teamdman.sfm.client.gui.flow.core.IFlowController;
import ca.teamdman.sfm.client.gui.flow.core.IFlowTangible;
import ca.teamdman.sfm.client.gui.flow.core.IFlowView;
import ca.teamdman.sfm.client.gui.flow.core.Size;
import ca.teamdman.sfm.common.flow.data.core.Position;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;

public class FlowPanel implements IFlowController, IFlowTangible {

	private final Position pos;
	private final Size size;
	private boolean isSelected = false;
	private int dx, dy;
	private int startMouseX, startMouseY;
	private boolean moved = false;
	private boolean moveable = false;

	public FlowPanel(Position pos, Size size) {
		this.pos = pos;
		this.size = size;
	}

	public FlowPanel setMovable(boolean movable) {
		this.moveable = movable;
		return this;
	}

	public boolean isSelected() {
		return this.isSelected;
	}

	public void setSelected(boolean selected) {
		this.isSelected = selected;
	}

	@Override
	public boolean mousePressed(int mx, int my, int button) {
		if (!moveable || !Screen.hasAltDown() || !size.contains(pos, mx, my)) {
			return false;
		}
		isSelected = true;
		startMouseX = mx;
		startMouseY = my;
		dx = mx - pos.getX();
		dy = my - pos.getY();
		moved = false;
		return true;
	}

	@Override
	public boolean mouseReleased(int mx, int my, int button) {
		if (!isSelected) {
			return false;
		}
		isSelected = false;
		if (moved) {
			onMoveFinished(startMouseX, startMouseY, mx, my, button);
		}
		return true;
	}

	public void onMoveFinished(int startMouseX, int startMouseY,
		int finishMouseX, int finishMouseY, int button) {

	}

	public void onMove(int startMouseX, int startMouseY, int finishMouseX,
		int finishMouseY, int button) {

	}

	@Override
	public IFlowView getView() {
		return IFlowView.NO_VIEW;
	}

	@Override
	public boolean mouseDragged(int mx, int my, int button, int dmx, int dmy) {
		if (!isSelected) {
			return false;
		}
		int newX = MathHelper.clamp(mx - getDragXOffset(), 0, 512 - size.getWidth());
		int newY = MathHelper.clamp(my - getDragYOffset(), 0, 256 - size.getHeight());

		if (Screen.hasShiftDown()) {
			newX = newX - newX % 5;
			newY = newY - newY % 5;
		}
		pos.setX(newX);
		pos.setY(newY);
		moved = true;
		onMove(startMouseX, startMouseY, mx, my, button);
		return true;
	}

	public int getDragXOffset() {
		return this.dx;
	}

	public void setDragXOffset(int dx) {
		this.dx = dx;
	}

	public int getDragYOffset() {
		return this.dy;
	}

	public void setDragYOffset(int dy) {
		this.dy = dy;
	}

	@Override
	public Position getPosition() {
		return pos;
	}

	@Override
	public Size getSize() {
		return size;
	}
}
