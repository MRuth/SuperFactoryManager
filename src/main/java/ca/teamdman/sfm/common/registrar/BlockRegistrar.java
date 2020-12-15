/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ca.teamdman.sfm.common.registrar;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.block.CableBlock;
import ca.teamdman.sfm.common.block.CrafterBlock;
import ca.teamdman.sfm.common.block.ManagerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ObjectHolder;
import org.apache.logging.log4j.LogManager;

@Mod.EventBusSubscriber(modid = SFM.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BlockRegistrar {
	private static final Block WAITING = null;

	@SubscribeEvent
	public static void onRegisterBlocks(RegistryEvent.Register<Block> e) {
		e.getRegistry().registerAll(
				new ManagerBlock(Block.Properties.create(Material.IRON).hardnessAndResistance(5F, 6F).sound(SoundType.METAL)).setRegistryName(SFM.MOD_ID, "manager"),
				new CableBlock(Block.Properties.create(Material.IRON).hardnessAndResistance(3F, 6F).sound(SoundType.METAL)).setRegistryName(SFM.MOD_ID, "cable"),
				new CrafterBlock(Block.Properties.create(Material.IRON).hardnessAndResistance(3F, 6F).sound(SoundType.METAL)).setRegistryName(SFM.MOD_ID, "crafter")
		);
		LogManager.getLogger(SFM.MOD_NAME + " Blocks Registrar").debug("Registered blocks");
	}

	@ObjectHolder(SFM.MOD_ID)
	public static final class Blocks {
		public static final Block MANAGER = WAITING;
		public static final Block CABLE = WAITING;
		public static final Block CRAFTER = WAITING;
	}
}
