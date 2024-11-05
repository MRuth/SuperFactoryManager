package ca.teamdman.sfm.client.handler;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.item.LabelGunItem;
import ca.teamdman.sfm.common.item.NetworkToolItem;
import ca.teamdman.sfm.common.program.LabelPositionHolder;
import ca.teamdman.sfm.common.util.HelpsWithMinecraftVersionIndependence;
import com.google.common.collect.HashMultimap;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Mod.EventBusSubscriber(modid = SFM.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
/*
 * This class uses code from tasgon's "observable" mod, also using MPLv2
 * https://github.com/tasgon/observable/blob/master/common/src/main/kotlin/observable/client/Overlay.kt
 */
public class ItemWorldRenderer {
    private static final int BUFFER_SIZE = 256;
    @SuppressWarnings("deprecation")
    private static final RenderType RENDER_TYPE = RenderType.create(
            "sfm_overlay",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            BUFFER_SIZE,
            false,
            false,
            RenderType.CompositeState
                    .builder()
                    .setTextureState(new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, false))
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("always", 519))
                    .setTransparencyState(
                            new RenderStateShard.TransparencyStateShard(
                                    "src_to_one",
                                    () -> {
                                        RenderSystem.enableBlend();
                                        RenderSystem.blendFunc(
                                                GlStateManager.SourceFactor.SRC_ALPHA,
                                                GlStateManager.DestFactor.ONE
                                        );
                                    },
                                    () -> {
                                        RenderSystem.disableBlend();
                                        RenderSystem.defaultBlendFunc();
                                    }
                            )
                    )
                    .createCompositeState(true)
    );

    private static final int capabilityColor = FastColor.ARGB32.color(100, 100, 0, 255);
    private static final int capabilityColorLimitedView = FastColor.ARGB32.color(100, 0, 100, 255);
    private static final int cableColor = FastColor.ARGB32.color(100, 100, 255, 0);
    private static final VBOCache vboCache = new VBOCache();

    @SubscribeEvent
    public static void renderOverlays(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        PoseStack poseStack = event.getPoseStack();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        ItemStack held;
        boolean rendered = false;
        // Can render both if in main hand and off-hand
        if ((held = getHeldItemOfType(player, NetworkToolItem.class)) != null) {
            handleNetworkTool(event, poseStack, camera, bufferSource, held);
            rendered = true;
        }
        if ((held = getHeldItemOfType(player, LabelGunItem.class)) != null) {
            handleLabelGun(event, poseStack, camera, bufferSource, held);
            rendered = true;
        }
        if (!rendered) {
            vboCache.clear();
        }
    }

    // Thanks @tigres810
    // https://discord.com/channels/313125603924639766/983834532904042537/1009267533527928864
    public static @Nullable BlockPos lookingAt() {
        HitResult rt = Minecraft.getInstance().hitResult;
        if (rt == null) return null;

        double x = (rt.getLocation().x);
        double y = (rt.getLocation().y);
        double z = (rt.getLocation().z);

        double xla = Minecraft.getInstance().player.getLookAngle().x;
        double yla = Minecraft.getInstance().player.getLookAngle().y;
        double zla = Minecraft.getInstance().player.getLookAngle().z;

        if ((x % 1 == 0) && (xla < 0)) x -= 0.01;
        if ((y % 1 == 0) && (yla < 0)) y -= 0.01;
        if ((z % 1 == 0) && (zla < 0)) z -= 0.01;

        return new BlockPos(x, y, z);
    }

    private static @Nullable ItemStack getHeldItemOfType(
            LocalPlayer player,
            Class<?> itemClass
    ) {
        ItemStack mainHandItem = player.getMainHandItem();
        if (itemClass.isInstance(mainHandItem.getItem())) {
            return mainHandItem;
        }

        ItemStack offhandItem = player.getOffhandItem();
        if (itemClass.isInstance(offhandItem.getItem())) {
            return offhandItem;
        }

        return null; // Neither hand holds the item
    }

    private static void handleLabelGun(
            RenderLevelStageEvent event,
            PoseStack poseStack,
            Camera camera,
            MultiBufferSource.BufferSource bufferSource,
            ItemStack labelGun
    ) {
        // Get labels
        boolean onlyShowSelectedLabel = LabelGunItem.getOnlyShowActiveLabel(labelGun);
        LabelPositionHolder labelPositionHolder = LabelPositionHolder.from(labelGun);
        HashMultimap<BlockPos, String> labelsByPosition = HashMultimap.create();
        if (onlyShowSelectedLabel) {
            String activeLabel = LabelGunItem.getActiveLabel(labelGun);
            labelPositionHolder.forEach((label, pos1) -> {
                if (Objects.equals(label, activeLabel)) {
                    labelsByPosition.put(pos1, label);
                }
            });
            BlockPos hitPos = lookingAt();
            if (hitPos != null) {
                labelPositionHolder.getLabels(hitPos).forEach(label -> labelsByPosition.put(hitPos, label));
            }
        } else {
            labelPositionHolder.forEach((label, pos1) -> labelsByPosition.put(pos1, label));
        }

        RenderSystem.disableDepthTest();


        // Draw labels
        poseStack.pushPose();
        poseStack.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);
        for (Map.Entry<BlockPos, Collection<String>> entry : labelsByPosition.asMap().entrySet()) {
            BlockPos pos = entry.getKey();
            Collection<String> labels = entry.getValue();
            drawLabelsForPos(poseStack, camera, pos, bufferSource, labels);
        }
        poseStack.popPose();

        // Draw boxes
        RENDER_TYPE.setupRenderState();
        Set<BlockPos> labelledPositions = labelsByPosition.keySet();
        drawVbo(
                VBOKind.LABEL_GUN_CAPABILITIES,
                poseStack,
                labelledPositions,
                onlyShowSelectedLabel ? capabilityColorLimitedView : capabilityColor,
                event
        );
        RENDER_TYPE.clearRenderState();

        bufferSource.endBatch();
        RenderSystem.enableDepthTest();
    }


    private static void handleNetworkTool(
            RenderLevelStageEvent event,
            PoseStack poseStack,
            Camera camera,
            MultiBufferSource.BufferSource bufferSource,
            ItemStack networkTool
    ) {
        Set<BlockPos> cablePositions = NetworkToolItem.getCablePositions(networkTool);
        Set<BlockPos> capabilityPositions = NetworkToolItem.getCapabilityProviderPositions(networkTool);

        RenderSystem.disableDepthTest();

        RENDER_TYPE.setupRenderState();

        drawVbo(VBOKind.NETWORK_TOOL_CABLES, poseStack, cablePositions, cableColor, event);
        drawVbo(VBOKind.NETWORK_TOOL_CAPABILITIES, poseStack, capabilityPositions, capabilityColor, event);

        RENDER_TYPE.clearRenderState();


        bufferSource.endBatch();
        RenderSystem.enableDepthTest();
    }

    private static void drawVbo(
            VBOKind vboKind,
            PoseStack poseStack,
            Set<BlockPos> positions,
            int color,
            RenderLevelStageEvent event
    ) {
        VertexBuffer vbo = vboCache.getVBO(
                vboKind,
                positions,
                event,
                FastColor.ARGB32.red(color),
                FastColor.ARGB32.green(color),
                FastColor.ARGB32.blue(color),
                FastColor.ARGB32.alpha(color)
        );
        if (vbo != null) {
            poseStack.pushPose();
            // we need to pass in a new destination quaternion to avoid undesired camera mutation
//            poseStack.mulPose(event.getCamera().rotation().invert(new Quaternionf()));
            poseStack.translate(
                    -event.getCamera().getPosition().x,
                    -event.getCamera().getPosition().y,
                    -event.getCamera().getPosition().z
            );

            // Draw the VBO
            vbo.bind();
            assert GameRenderer.getPositionColorShader() != null;
            vbo.drawWithShader(
                    poseStack.last().pose(),
                    event.getProjectionMatrix(),
                    GameRenderer.getPositionColorShader()
            );
            VertexBuffer.unbind();

            poseStack.popPose();
        }
    }

    private static void drawLabelsForPos(
            PoseStack poseStack,
            Camera camera,
            BlockPos pos,
            MultiBufferSource mbs,
            Collection<String> labels
    ) {
        poseStack.pushPose();
        poseStack.translate(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        poseStack.mulPose(camera.rotation());
//        poseStack.mulPose(Axis.YP.rotationDegrees(180));
        poseStack.scale(-0.025f, -0.025f, 0.025f);

        Font font = Minecraft.getInstance().font;
        poseStack.translate(0, labels.size() * (font.lineHeight + 0.1) / -2f, 0);
        for (String label : labels) {
            font.drawInBatch(
                    label,
                    -font.width(label) / 2f,
                    0,
                    -0x1,
                    false,
                    poseStack.last().pose(),
                    mbs,
                    true,
                    0,
                    0xF000F0
            );
            poseStack.translate(0, font.lineHeight + 0.1, 0);

        }
        poseStack.popPose();
    }

    @HelpsWithMinecraftVersionIndependence
    private static void writeVertex(
            VertexConsumer builder,
            Matrix4f matrix4f,
            float x,
            float y,
            float z,
            int r,
            int g,
            int b,
            int a
    ) {
        builder.vertex(matrix4f, x, y, z).color(r, g, b, a).endVertex();
    }

    private static void writeFaceVertices(
            VertexConsumer builder,
            Matrix4f matrix4f,
            Direction direction,
            int r,
            int g,
            int b,
            int a
    ) {
        double scale = 1 - ((double) direction.ordinal() / 25d);
        r = (int) (r * scale);
        g = (int) (g * scale);
        b = (int) (b * scale);
        a = (int) (a * scale);
        switch (direction) {
            case DOWN:
                writeVertex(builder, matrix4f, 0F, 0F, 0F, r, g, b, a);
                writeVertex(builder, matrix4f, 1F, 0F, 0F, r, g, b, a);
                writeVertex(builder, matrix4f, 1F, 0F, 1F, r, g, b, a);
                writeVertex(builder, matrix4f, 0F, 0F, 1F, r, g, b, a);
                break;
            case UP:
                writeVertex(builder, matrix4f, 0F, 1F, 1F, r, g, b, a);
                writeVertex(builder, matrix4f, 1F, 1F, 1F, r, g, b, a);
                writeVertex(builder, matrix4f, 1F, 1F, 0F, r, g, b, a);
                writeVertex(builder, matrix4f, 0F, 1F, 0F, r, g, b, a);
                break;
            case NORTH:
                writeVertex(builder, matrix4f, 0F, 0F, 0F, r, g, b, a);
                writeVertex(builder, matrix4f, 0F, 1F, 0F, r, g, b, a);
                writeVertex(builder, matrix4f, 1F, 1F, 0F, r, g, b, a);
                writeVertex(builder, matrix4f, 1F, 0F, 0F, r, g, b, a);
                break;
            case SOUTH:
                writeVertex(builder, matrix4f, 1F, 0F, 1F, r, g, b, a);
                writeVertex(builder, matrix4f, 1F, 1F, 1F, r, g, b, a);
                writeVertex(builder, matrix4f, 0F, 1F, 1F, r, g, b, a);
                writeVertex(builder, matrix4f, 0F, 0F, 1F, r, g, b, a);
                break;
            case WEST:
                writeVertex(builder, matrix4f, 0F, 0F, 1F, r, g, b, a);
                writeVertex(builder, matrix4f, 0F, 1F, 1F, r, g, b, a);
                writeVertex(builder, matrix4f, 0F, 1F, 0F, r, g, b, a);
                writeVertex(builder, matrix4f, 0F, 0F, 0F, r, g, b, a);
                break;
            case EAST:
                writeVertex(builder, matrix4f, 1F, 0F, 0F, r, g, b, a);
                writeVertex(builder, matrix4f, 1F, 1F, 0F, r, g, b, a);
                writeVertex(builder, matrix4f, 1F, 1F, 1F, r, g, b, a);
                writeVertex(builder, matrix4f, 1F, 0F, 1F, r, g, b, a);
                break;
        }
    }

    // Enum to represent different kinds of VBOs
    private enum VBOKind {
        LABEL_GUN_CAPABILITIES,
        NETWORK_TOOL_CAPABILITIES,
        NETWORK_TOOL_CABLES
    }

    // VBOCache class to handle caching of VBOs
    private static class VBOCache {
        private final EnumMap<VBOKind, VBOEntry> cache = new EnumMap<>(VBOKind.class);
        private int lastChangeCheck = -1;

        public @Nullable VertexBuffer getVBO(
                VBOKind kind,
                Set<BlockPos> positions,
                RenderLevelStageEvent event,
                int r,
                int g,
                int b,
                int a
        ) {
            if (positions.isEmpty()) {
                return null;
            }
            @Nullable VBOEntry entry = cache.get(kind);

            boolean shouldRebuild = entry == null;

            // only compare the entries every second since it's mildly expensive
            if (entry != null
                && event.getRenderTick() != lastChangeCheck
                && !entry.positions.equals(positions)) {
                lastChangeCheck = event.getRenderTick();
                shouldRebuild = true;
            }

            if (shouldRebuild) {
                // Dispose of the old VBO if it exists
                if (entry != null) {
                    entry.vbo.close();
                }

                // Create a new VBO
                VertexBuffer vbo = createVBO(positions, r, g, b, a);

                // Cache the new VBO
                entry = new VBOEntry(new HashSet<>(positions), vbo);
                cache.put(kind, entry);
            }

            return entry.vbo;
        }

        public void clear() {
            // Dispose of all cached VBOs
            for (VBOEntry entry : cache.values()) {
                entry.vbo.close();
            }
            cache.clear();
        }

        @HelpsWithMinecraftVersionIndependence
        private BufferBuilder createBufferBuilder(int numPositions) {
            BufferBuilder bufferBuilder = new BufferBuilder(RENDER_TYPE.bufferSize() * numPositions);
            bufferBuilder.begin(RENDER_TYPE.mode(), RENDER_TYPE.format());
            return bufferBuilder;
        }

        private VertexBuffer createVBO(
                Set<BlockPos> positions,
                int r,
                int g,
                int b,
                int a
        ) {
            // Build the mesh data
            PoseStack poseStack = new PoseStack();
            // Do not undo camera transform; create vertices in world space

            BufferBuilder bufferBuilder = createBufferBuilder(positions.size());

            // Push vertices
            for (BlockPos blockPos : positions) {
                poseStack.pushPose();
                poseStack.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                Matrix4f matrix4f = poseStack.last().pose();
                for (Direction face : Direction.values()) {
                    if (!positions.contains(blockPos.relative(face))) {
                        writeFaceVertices(bufferBuilder, matrix4f, face, r, g, b, a);
                    }
                }
                poseStack.popPose();
            }

            BufferBuilder.RenderedBuffer meshData = bufferBuilder.end();
            VertexBuffer vbo = new VertexBuffer();
            vbo.bind();
            vbo.upload(meshData);
            VertexBuffer.unbind();

            return vbo;
        }

        private record VBOEntry(
                Set<BlockPos> positions,
                VertexBuffer vbo
        ) {
        }
    }
}
