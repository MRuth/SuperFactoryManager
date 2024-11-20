package ca.teamdman.sfm.common.compat;

import ca.teamdman.sfm.common.resourcetype.GasResourceType;
import ca.teamdman.sfm.common.resourcetype.InfuseResourceType;
import ca.teamdman.sfm.common.resourcetype.PigmentResourceType;
import ca.teamdman.sfm.common.resourcetype.SlurryResourceType;
import com.google.common.collect.Sets;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SFMCompat {
    private static final List<Capability<?>> CAPABILITIES = new ArrayList<>();

    public static boolean isMekanismLoaded() {
        return isModLoaded("mekanism");
    }

    public static boolean isAE2Loaded() {
        return isModLoaded("ae2");
    }

    public static boolean isModLoaded(String modid) {
        return ModList.get().getModContainerById(modid).isPresent();
    }

    public static List<Capability<?>> getCapabilities() {
        if (!CAPABILITIES.isEmpty()) {
            return new ArrayList<>(CAPABILITIES);
        }

        Set<Capability<?>> caps = Sets.newHashSet(
                ForgeCapabilities.ITEM_HANDLER,
                ForgeCapabilities.FLUID_HANDLER,
                ForgeCapabilities.ENERGY
        );

        if (isMekanismLoaded()) {
            caps.addAll(List.of(
                    GasResourceType.CAP,
                    InfuseResourceType.CAP,
                    PigmentResourceType.CAP,
                    SlurryResourceType.CAP
            ));
        }

        CAPABILITIES.addAll(caps);
        return CAPABILITIES;
    }
}
