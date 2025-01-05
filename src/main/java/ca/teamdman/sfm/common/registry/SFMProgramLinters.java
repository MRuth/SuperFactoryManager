package ca.teamdman.sfm.common.registry;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.compat.SFMMekanismCompat;
import ca.teamdman.sfm.common.compat.SFMModCompat;
import ca.teamdman.sfm.common.program.linting.FlowProgramLinter;
import ca.teamdman.sfm.common.program.linting.IProgramLinter;
import ca.teamdman.sfm.common.program.linting.ResourcesProgramLinter;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class SFMProgramLinters {
    public static final ResourceLocation REGISTRY_ID = new ResourceLocation(SFM.MOD_ID, "program_linters");
    private static final DeferredRegister<IProgramLinter> TYPES = DeferredRegister.create(
            REGISTRY_ID,
            SFM.MOD_ID
    );
    public static final Supplier<IForgeRegistry<IProgramLinter>> DEFERRED_TYPES = TYPES.makeRegistry(
            () -> new RegistryBuilder<IProgramLinter>().setName(REGISTRY_ID));

    public static final RegistryObject<IProgramLinter> FLOW = TYPES.register(
            "flow",
            FlowProgramLinter::new
    );

    public static final RegistryObject<IProgramLinter> RESOURCE = TYPES.register(
            "resources",
            ResourcesProgramLinter::new
    );

    static {
        if (SFMModCompat.isMekanismLoaded()) {
            SFMMekanismCompat.registerProgramLinters(TYPES);
        }
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
}
