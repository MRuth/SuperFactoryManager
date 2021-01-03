/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ca.teamdman.sfm.common.net.packet.manager.patch;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.SFMUtil;
import ca.teamdman.sfm.common.flow.data.core.SelectionHolder;
import ca.teamdman.sfm.common.net.packet.manager.C2SManagerPacket;
import ca.teamdman.sfm.common.tile.ManagerTileEntity;
import java.util.UUID;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

public class ManagerToggleBlockPosSelectedC2S extends C2SManagerPacket {

	private final UUID DATA_ID;
	private final BlockPos BLOCK_POS;
	private final boolean SELECTED;

	public ManagerToggleBlockPosSelectedC2S(
		int WINDOW_ID, BlockPos TILE_POSITION, UUID DATA_ID, BlockPos BLOCK_POS, boolean SELECTED
	) {
		super(WINDOW_ID, TILE_POSITION);
		this.DATA_ID = DATA_ID;
		this.BLOCK_POS = BLOCK_POS;
		this.SELECTED = SELECTED;
	}

	public static class Handler extends C2SHandler<ManagerToggleBlockPosSelectedC2S> {

		@Override
		public void finishEncode(ManagerToggleBlockPosSelectedC2S msg, PacketBuffer buf) {
			SFMUtil.writeUUID(msg.DATA_ID, buf);
			buf.writeBlockPos(msg.BLOCK_POS);
			buf.writeBoolean(msg.SELECTED);
		}

		@Override
		public ManagerToggleBlockPosSelectedC2S finishDecode(
			int windowId, BlockPos tilePos,
			PacketBuffer buf
		) {
			return new ManagerToggleBlockPosSelectedC2S(windowId, tilePos,
				SFMUtil.readUUID(buf),
				buf.readBlockPos(),
				buf.readBoolean()
			);
		}

		@Override
		public void handleDetailed(
			ManagerToggleBlockPosSelectedC2S msg,
			ManagerTileEntity manager
		) {
			SFM.LOGGER.debug(
				SFMUtil.getMarker(getClass()),
				"C2S Received, setting selected for data {} pos {} to {}",
				msg.DATA_ID,
				msg.BLOCK_POS,
				msg.SELECTED
			);
			manager.getFlowDataContainer().getData(msg.DATA_ID)
				.filter(data -> data instanceof SelectionHolder)
				.filter(data -> ((SelectionHolder<?>) data).getSelectionType() == BlockPos.class)
				.map(data -> ((SelectionHolder<BlockPos>) data))
				.ifPresent(data -> {
					data.setSelected(msg.BLOCK_POS, msg.SELECTED);
					manager.sendPacketToListeners(new ManagerToggleBlockPosSelectedS2C(
						msg.WINDOW_ID,
						msg.DATA_ID,
						msg.BLOCK_POS,
						msg.SELECTED
					));
				});
		}
	}
}
