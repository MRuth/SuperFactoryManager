/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ca.teamdman.sfm.common.net.packet.manager.patch;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.SFMUtil;
import ca.teamdman.sfm.common.flow.core.Position;
import ca.teamdman.sfm.common.flow.core.PositionHolder;
import ca.teamdman.sfm.common.net.packet.manager.C2SManagerPacket;
import ca.teamdman.sfm.common.tile.ManagerTileEntity;
import java.util.UUID;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

public class ManagerPositionPacketC2S extends C2SManagerPacket {

	private final Position POSITION;
	private final UUID ELEMENT_ID;

	public ManagerPositionPacketC2S(
		int windowId, BlockPos tilePos, UUID elementId,
		Position elementPos
	) {
		super(windowId, tilePos);
		this.ELEMENT_ID = elementId;
		this.POSITION = elementPos;
	}

	public static class Handler extends C2SHandler<ManagerPositionPacketC2S> {

		@Override
		public void finishEncode(ManagerPositionPacketC2S msg, PacketBuffer buf) {
			SFMUtil.writeUUID(msg.ELEMENT_ID, buf);
			buf.writeLong(msg.POSITION.toLong());
		}

		@Override
		public ManagerPositionPacketC2S finishDecode(
			int windowId, BlockPos tilePos,
			PacketBuffer buf
		) {
			return new ManagerPositionPacketC2S(
				windowId,
				tilePos,
				SFMUtil.readUUID(buf),
				Position.fromLong(buf.readLong())
			);
		}

		@Override
		public void handleDetailed(
			ManagerPositionPacketC2S msg,
			ManagerTileEntity manager
		) {
			SFM.LOGGER.debug(
				SFMUtil.getMarker(getClass()),
				"C2S Received, setting pos to {} for element {}",
				msg.POSITION,
				msg.ELEMENT_ID
			);
			manager.mutateManagerData(msg.ELEMENT_ID, data -> {
				if (data instanceof PositionHolder) {
					((PositionHolder) data).getPosition().setXY(msg.POSITION);
				}
			}, () -> manager.sendPacketToListeners(new ManagerPositionPacketS2C(
				msg.WINDOW_ID,
				msg.ELEMENT_ID,
				msg.POSITION
			)));
		}
	}
}
