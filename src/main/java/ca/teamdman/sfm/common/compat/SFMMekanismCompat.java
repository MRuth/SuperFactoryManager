package ca.teamdman.sfm.common.compat;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.localization.LocalizationKeys;
import ca.teamdman.sfm.common.registry.SFMResourceTypes;
import ca.teamdman.sfm.common.resourcetype.*;
import ca.teamdman.sfml.ast.DirectionQualifier;
import mekanism.api.RelativeSide;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.interfaces.ISideConfiguration;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SFMMekanismCompat {
    @SuppressWarnings("DataFlowIssue")
    public static @Nullable ResourceType<?, ?, ?> getResourceType(TransmissionType trans) {
        return switch (trans) {
            case ITEM -> SFMResourceTypes.ITEM.get();
            case FLUID -> SFMResourceTypes.FLUID.get();
            case GAS -> SFMResourceTypes.DEFERRED_TYPES
                    .get()
                    .getValue(new ResourceLocation(SFM.MOD_ID, "gas"));
            case INFUSION -> SFMResourceTypes.DEFERRED_TYPES
                    .get()
                    .getValue(new ResourceLocation(SFM.MOD_ID, "infusion"));
            case PIGMENT -> SFMResourceTypes.DEFERRED_TYPES
                    .get()
                    .getValue(new ResourceLocation(SFM.MOD_ID, "pigment"));
            case SLURRY -> SFMResourceTypes.DEFERRED_TYPES
                    .get()
                    .getValue(new ResourceLocation(SFM.MOD_ID, "slurry"));
            default -> null;
        };
    }

    public static String gatherInspectionResults(BlockEntity blockEntity) {
        if (!(blockEntity instanceof ISideConfiguration sideConfiguration)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("-- Mekanism stuff\n");
        TileComponentConfig config = sideConfiguration.getConfig();
        for (TransmissionType type : TransmissionType.values()) {
            var resourceType = getResourceType(type);
            if (resourceType == null) {
                continue;
            }

            var maybeResourceTypeKe = SFMResourceTypes.DEFERRED_TYPES.get().getResourceKey(resourceType);
            if (maybeResourceTypeKe.isEmpty()) {
                continue;
            }
            var resourceTypeKey = maybeResourceTypeKe.get();

            ConfigInfo info = config.getConfig(type);
            if (info == null) {
                continue;
            }

            Set<Direction> outputSides = info.getSides(DataType::canOutput);
            if (!outputSides.isEmpty()) {
                sb
                        .append("-- ")
                        .append(LocalizationKeys.CONTAINER_INSPECTOR_MEKANISM_MACHINE_OUTPUTS.getString())
                        .append("\n");
                sb.append("INPUT ").append(resourceTypeKey.location()).append(":: FROM target ");
                sb.append(outputSides
                        .stream()
                        .map(DirectionQualifier::directionToString)
                        .collect(Collectors.joining(", ")));
                sb.append(" SIDE\n");
            }

            Set<Direction> inputSides = new HashSet<>();
            for (RelativeSide side : RelativeSide.values()) {
                DataType dataType = info.getDataType(side);
                if (dataType == DataType.INPUT
                    || dataType == DataType.INPUT_1
                    || dataType == DataType.INPUT_2
                    || dataType == DataType.INPUT_OUTPUT) {
                    inputSides.add(side.getDirection(sideConfiguration.getDirection()));
                }
            }
            if (!inputSides.isEmpty()) {
                sb
                        .append("-- ")
                        .append(LocalizationKeys.CONTAINER_INSPECTOR_MEKANISM_MACHINE_INPUTS.getString())
                        .append("\n");
                sb.append("OUTPUT ").append(resourceTypeKey.location()).append(":: TO target ");
                sb.append(inputSides
                        .stream()
                        .map(DirectionQualifier::directionToString)
                        .collect(Collectors.joining(", ")));
                sb.append(" SIDE\n");
            }
        }
        return sb.toString();
    }

    public static void register(DeferredRegister<ResourceType<?, ?, ?>> types) {
        types.register(
                "gas",
                GasResourceType::new
        );
        types.register(
                "infusion",
                InfuseResourceType::new
        );

        types.register(
                "pigment",
                PigmentResourceType::new
        );
        types.register(
                "slurry",
                SlurryResourceType::new
        );
        types.register(
                "mekanism_energy",
                MekanismEnergyResourceType::new
        );
    }
}
