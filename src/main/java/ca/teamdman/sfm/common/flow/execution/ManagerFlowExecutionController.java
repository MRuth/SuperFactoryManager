/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ca.teamdman.sfm.common.flow.execution;

import ca.teamdman.sfm.common.flow.data.core.FlowData;
import ca.teamdman.sfm.common.flow.data.impl.TimerTriggerFlowData;
import ca.teamdman.sfm.common.tile.ManagerTileEntity;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Collectors;
import net.minecraft.tileentity.ITickableTileEntity;

public class ManagerFlowExecutionController implements ITickableTileEntity {
	public final ManagerTileEntity MANAGER;

	public ManagerFlowExecutionController(ManagerTileEntity MANAGER) {
		this.MANAGER = MANAGER;
	}

	@Override
	public void tick() {
		Deque<FlowData> toVisit = MANAGER.getData()
			.filter(data -> data instanceof TimerTriggerFlowData)
			.collect(Collectors.toCollection(ArrayDeque::new));

		while (toVisit.size() > 0) {
			FlowData current = toVisit.pop();
			MANAGER.graph.getDescendants(current.getId())
				.map(node -> node.NODE_DATA)
				.forEach(toVisit::add);
		}
	}
}