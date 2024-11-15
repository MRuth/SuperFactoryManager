package ca.teamdman.sfm.common.command;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.cablenetwork.CableNetworkManager;
import ca.teamdman.sfm.common.config.ConfigExporter;
import ca.teamdman.sfm.common.config.SFMConfig;
import ca.teamdman.sfm.common.net.ClientboundConfigResponsePacket;
import ca.teamdman.sfm.common.registry.SFMPackets;
import ca.teamdman.sfm.common.watertanknetwork.WaterNetworkManager;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = SFM.MOD_ID)
public class SFMCommand {
    @SubscribeEvent
    public static void onRegisterCommand(final RegisterCommandsEvent event) {
        var command = Commands.literal("sfm");
        command.then(Commands.literal("bust_cable_network_cache")
                             .requires(source -> source.hasPermission(Commands.LEVEL_ALL))
                             .executes(ctx -> {
                                 SFM.LOGGER.info(
                                         "Busting cable networks - slash command used by {}",
                                         ctx.getSource().getTextName()
                                 );
                                 CableNetworkManager.clear();
                                 return SINGLE_SUCCESS;
                             }));
        command.then(Commands.literal("bust_water_network_cache")
                             .requires(source -> source.hasPermission(Commands.LEVEL_ALL))
                             .executes(ctx -> {
                                 SFM.LOGGER.info(
                                         "Busting water networks - slash command used by {}",
                                         ctx.getSource().getTextName()
                                 );
                                 WaterNetworkManager.clear();
                                 return SINGLE_SUCCESS;
                             }));
        command.then(Commands.literal("show_bad_cable_cache_entries")
                             .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                             .then(Commands.argument("block", BlockStateArgument.block(event.getBuildContext()))
                                           .executes(ctx -> {
                                               ServerLevel level = ctx.getSource().getLevel();
                                               CableNetworkManager.getBadCableCachePositions(level).forEach(pos -> {
                                                   BlockInput block = BlockStateArgument
                                                           .getBlock(
                                                                   ctx,
                                                                   "block"
                                                           );
                                                   block.place(
                                                           level,
                                                           pos,
                                                           Block.UPDATE_ALL
                                                   );
                                               });
                                               return SINGLE_SUCCESS;
                                           })));
        command.then(
                Commands.literal("config")
                        .then(Commands.literal("show")
                                      .requires(source -> source.hasPermission(Commands.LEVEL_ALL))
                                      .executes(ctx -> {
                                          SFM.LOGGER.info(
                                                  "Showing config - slash command used by {}",
                                                  ctx.getSource().getTextName()
                                          );
                                          ServerPlayer player = ctx.getSource().getPlayer();
                                          if (player == null) {
                                              SFM.LOGGER.error(
                                                      "Received ServerboundConfigRequestPacket (show) from null player"
                                              );
                                              return SINGLE_SUCCESS;
                                          }
                                          SFMPackets.sendToPlayer(
                                                  player,
                                                  new ClientboundConfigResponsePacket(
                                                          ConfigExporter.getConfigToml(SFMConfig.SERVER_SPEC),
                                                          ClientboundConfigResponsePacket.ConfigResponseUsage.SHOW
                                                  )
                                          );
                                          return SINGLE_SUCCESS;
                                      })
                        )
                        .then(Commands.literal("edit")
                                      .requires(source -> source.hasPermission(Commands.LEVEL_OWNERS))
                                      .executes(ctx -> {
                                          SFM.LOGGER.info(
                                                  "Editing config - slash command used by {}",
                                                  ctx.getSource().getTextName()
                                          );
                                          ServerPlayer player = ctx.getSource().getPlayer();
                                          if (player == null) {
                                              SFM.LOGGER.error(
                                                      "Received ServerboundConfigRequestPacket (edit) from null player"
                                              );
                                              return SINGLE_SUCCESS;
                                          }
                                          SFMPackets.sendToPlayer(
                                                  player,
                                                  new ClientboundConfigResponsePacket(
                                                          ConfigExporter.getConfigToml(SFMConfig.SERVER_SPEC),
                                                          ClientboundConfigResponsePacket.ConfigResponseUsage.EDIT
                                                  )
                                          );
                                          return SINGLE_SUCCESS;
                                      })
                        )
        );
        event.getDispatcher().register(command);
    }
}
