/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ca.teamdman.sfm.client.gui.flow.impl.manager.flowdataholder;

import ca.teamdman.sfm.client.gui.flow.core.IFlowCloneable;
import ca.teamdman.sfm.client.gui.flow.impl.manager.core.ManagerFlowController;
import ca.teamdman.sfm.client.gui.flow.impl.util.FlowIconButton;
import ca.teamdman.sfm.common.flow.core.FlowDataHolder;
import ca.teamdman.sfm.common.flow.core.Position;
import ca.teamdman.sfm.common.flow.data.TimerTriggerFlowData;
import java.util.UUID;

public class FlowTimerTrigger extends FlowIconButton implements IFlowCloneable,
	FlowDataHolder<TimerTriggerFlowData> {

	public final ManagerFlowController CONTROLLER;
	private TimerTriggerFlowData data;

	public FlowTimerTrigger(ManagerFlowController controller, TimerTriggerFlowData data) {
		super(ButtonLabel.TRIGGER, data.getPosition().copy());
		this.CONTROLLER = controller;
		this.data = data;
		setDraggable(true);
	}

	@Override
	public TimerTriggerFlowData getData() {
		return data;
	}

	@Override
	public void setData(TimerTriggerFlowData data) {
		this.data = data;
		getPosition().setXY(data.getPosition());
	}

	@Override
	public void cloneWithPosition(int x, int y) {
		CONTROLLER.SCREEN.sendFlowDataToServer(
			new TimerTriggerFlowData(
				UUID.randomUUID(),
				new Position(x, y),
				data.interval
			)
		);
	}

	@Override
	public boolean isDeletable() {
		return true;
	}

	@Override
	public void onDragFinished(int dx, int dy, int mx, int my) {
		data.position = getPosition();
		CONTROLLER.SCREEN.sendFlowDataToServer(data);
	}

	@Override
	public void onClicked(int mx, int my, int button) {

	}
}
