package ca.teamdman.sfm.gametest;

import ca.teamdman.sfm.SFM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraftforge.gametest.GameTestHolder;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

@GameTestHolder(SFM.MOD_ID)
public class SFM_A_GameTestStructureGeneratorTest extends SFMGameTestBase {
    @GameTest(template = "25x4x25")
    public static void create_structure_nbt(GameTestHelper helper) {
        StructureBlockEntity structureBlockEntity;
        structureBlockEntity = (StructureBlockEntity) helper.getBlockEntity(new BlockPos(0, 0, 0));
        assert structureBlockEntity != null;
        String oldName = structureBlockEntity.getStructureName();
        StructureMode oldMode = structureBlockEntity.getMode();
        Vec3i oldSize = structureBlockEntity.getStructureSize();
        try {
            structureBlockEntity.setMode(StructureMode.SAVE);
            int existingCount = 0;
            Set<String> structureNames = GameTestRegistry
                    .getAllTestFunctions()
                    .stream()
                    // sfmmekanismcompatgametests.mek_chemtank_infusion_full
                    .filter(testFunction -> testFunction.getTestName().startsWith("sfm"))
                    .map(TestFunction::getStructureName)
                    .map(structureName -> structureName.split("sfm:")[1])
                    .collect(Collectors.toSet());
            for (String structureName : structureNames) {
                // extract size
                Vec3i size;
                // sfm:sfmmekanismcompatgametests.3x2x1
                String[] chunks = structureName.split("\\.");
                if (chunks.length != 2) {
                    SFM.LOGGER.warn(
                            "Structure name does not contain dimensions: {}",
                            structureName
                    );
                    continue;
                }
                String dimensions = chunks[1];
                String[] xyz = dimensions.split("x");
                size = new Vec3i(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
                structureBlockEntity.setStructureSize(size);

                // update structure size
                structureBlockEntity.setStructureSize(size);

                // rename the structure
                structureBlockEntity.setStructureName(structureName);

                // perform save
                structureBlockEntity.saveStructure();
                Path structurePath = helper
                        .getLevel()
                        .getStructureManager()
                        .getPathToGeneratedStructure(new ResourceLocation("minecraft", structureName), ".nbt");

                // copy file
                Path repoDir = structurePath
                        .toAbsolutePath()
                        .getParent()
                        .getParent()
                        .getParent()
                        .getParent()
                        .getParent()
                        .getParent()
                        .getParent();
                Path targetDir = repoDir.resolve("src/gametest/resources/data/sfm/structures");
//                    SFM.LOGGER.info("Copying {} to {}", structurePath, targetDir);
                try {
                    try {
                        java.nio.file.Files.copy(
                                structurePath,
                                targetDir.resolve(structurePath.getFileName())
                        );
                    } catch (FileAlreadyExistsException e) {
                        existingCount++;
                    }
                } catch (Exception e) {
                    SFM.LOGGER.error("Failed to copy structure", e);
                }
            }
            if (existingCount > 0) {
                SFM.LOGGER.warn("Skipped {} existing structures", existingCount);
            }
            helper.succeed();
        } catch (Throwable t) {
            SFM.LOGGER.error("Failed to create structures", t);
            helper.fail("Failed to create structures");
        } finally {
            structureBlockEntity.setStructureName(oldName);
            structureBlockEntity.setMode(oldMode);
            structureBlockEntity.setStructureSize(oldSize);

        }
    }
}
