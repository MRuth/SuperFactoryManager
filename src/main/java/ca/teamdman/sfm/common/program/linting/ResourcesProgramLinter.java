package ca.teamdman.sfm.common.program.linting;

import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.program.LabelPositionHolder;
import ca.teamdman.sfml.ast.Program;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Optional;

import static ca.teamdman.sfm.common.localization.LocalizationKeys.PROGRAM_WARNING_UNKNOWN_RESOURCE_ID;

public class ResourcesProgramLinter implements IProgramLinter {

    @Override
    public ArrayList<TranslatableContents> gatherWarnings(
            Program program,
            LabelPositionHolder labelPositionHolder,
            @Nullable ManagerBlockEntity managerBlockEntity
    ) {
        ArrayList<TranslatableContents> warnings = new ArrayList<>();

        // Check all referenced resources to see if they exist
        for (var resource : program.referencedResources()) {
            Optional<?> loc = resource.getLocation();
            if (loc.isEmpty()) {
                // It's a pattern-based resource or something not requiring a registry check
                continue;
            }
            // resource.getResourceType() can return null if something's not mapped
            if (resource.getResourceType() == null) {
                continue;
            }
            // If it doesn't exist in the registry, add a warning
            if (!resource.getResourceType().registryKeyExists((ResourceLocation) loc.get())) {
                warnings.add(PROGRAM_WARNING_UNKNOWN_RESOURCE_ID.get(resource));
            }
        }

        return warnings;
    }

    @Override
    public void fixWarnings(
            ManagerBlockEntity managerBlockEntity,
            ItemStack diskStack,
            Program program
    ) {
        // Resource references typically cannot be “auto-fixed,” so do nothing here.
    }
}
