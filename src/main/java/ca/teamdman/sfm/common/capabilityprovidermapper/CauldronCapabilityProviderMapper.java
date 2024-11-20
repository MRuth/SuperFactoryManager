package ca.teamdman.sfm.common.capabilityprovidermapper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class CauldronCapabilityProviderMapper implements CapabilityProviderMapper {
    @Override
    public @Nullable ICapabilityProvider getProviderFor(LevelAccessor level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (state.getBlock() == Blocks.CAULDRON
            || state.getBlock() == Blocks.WATER_CAULDRON
            || state.getBlock() == Blocks.LAVA_CAULDRON) {
            return new CauldronCapabilityProvider(level, pos);
        }
        return null;
    }

    private static class CauldronCapabilityProvider implements ICapabilityProvider {
        private final LazyOptional<IFluidHandler> fluidHandlerLazyOptional;


        public CauldronCapabilityProvider(LevelAccessor level, BlockPos pos) {
            this.fluidHandlerLazyOptional = LazyOptional.of(() -> new CauldronFluidHandler(level, pos));
        }

        @Nonnull
        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            if (cap == ForgeCapabilities.FLUID_HANDLER) {
                return fluidHandlerLazyOptional.cast();
            }
            return LazyOptional.empty();
        }
    }

    private record CauldronFluidHandler(
            LevelAccessor level,
            BlockPos pos
    ) implements IFluidHandler {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            var state = level.getBlockState(pos);
            if (state.getBlock() == Blocks.WATER_CAULDRON) {
                int level = state.getValue(LayeredCauldronBlock.LEVEL);
                if (level == 0) {
                    return FluidStack.EMPTY;
                }
                return new FluidStack(Fluids.WATER, level * 250);
            } else if (state.getBlock() == Blocks.LAVA_CAULDRON) {
                return new FluidStack(Fluids.LAVA, 1000);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return 1000;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return stack.getFluid() == Fluids.WATER || stack.getFluid() == Fluids.LAVA;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            var state = level.getBlockState(pos);
            if (state.getBlock() == Blocks.CAULDRON) { // if empty
                if (resource.getFluid() == Fluids.WATER) {
                    int layers = Math.min(3, resource.getAmount() / 250);
                    if (action.execute()) {
                        level.setBlock(
                                pos,
                                Blocks.WATER_CAULDRON.defaultBlockState().setValue(
                                        LayeredCauldronBlock.LEVEL,
                                        layers
                                ),
                                Block.UPDATE_ALL
                        );
                    }
                    return layers * 250;
                } else if (resource.getFluid() == Fluids.LAVA && resource.getAmount() >= 1000) {
                    if (action.execute()) {
                        level.setBlock(
                                pos,
                                Blocks.LAVA_CAULDRON.defaultBlockState(),
                                Block.UPDATE_ALL
                        );
                    }
                    return 1000;
                }
            } else if (state.getBlock() instanceof LayeredCauldronBlock) {
                int waterLevel = state.getValue(LayeredCauldronBlock.LEVEL);
                if (waterLevel >= 3) {
                    return 0;
                }
                int waterLevelIncrease = Math.min(3 - waterLevel, Math.min(3, resource.getAmount() / 250));
                if (action.execute()) {
                    level.setBlock(
                            pos,
                            state.setValue(LayeredCauldronBlock.LEVEL, waterLevel + waterLevelIncrease),
                            Block.UPDATE_ALL
                    );
                }
                return waterLevelIncrease * 250;
            }
            return 0;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof LayeredCauldronBlock) {
                int waterLevel = state.getValue(LayeredCauldronBlock.LEVEL);
                if (waterLevel == 0) {
                    return FluidStack.EMPTY;
                }
                int waterLevelDrain = Math.min(waterLevel, resource.getAmount() / 250);
                if (action.execute()) {
                    int resultLevel = waterLevel - waterLevelDrain;
                    if (resultLevel == 0) {
                        level.setBlock(
                                pos,
                                Blocks.CAULDRON.defaultBlockState(),
                                Block.UPDATE_ALL
                        );
                    } else {
                        level.setBlock(
                                pos,
                                state.setValue(LayeredCauldronBlock.LEVEL, resultLevel),
                                Block.UPDATE_ALL
                        );
                    }
                }
                return new FluidStack(Fluids.WATER, waterLevelDrain * 250);
            } else if (state.getBlock() == Blocks.LAVA_CAULDRON && resource.getAmount() >= 1000) {
                if (action.execute()) {
                    level.setBlock(
                            pos,
                            Blocks.CAULDRON.defaultBlockState(),
                            Block.UPDATE_ALL
                    );
                }
                return new FluidStack(Fluids.LAVA, 1000);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof LayeredCauldronBlock) {
                int waterLevel = state.getValue(LayeredCauldronBlock.LEVEL);
                if (waterLevel == 0) {
                    return FluidStack.EMPTY;
                }
                int waterLevelDrain = Math.min(waterLevel, maxDrain / 250);
                if (action.execute()) {
                    level.setBlock(
                            pos,
                            state.setValue(LayeredCauldronBlock.LEVEL, waterLevel - waterLevelDrain),
                            Block.UPDATE_ALL
                    );
                }
                return new FluidStack(Fluids.WATER, waterLevelDrain * 250);
            } else if (state.getBlock() == Blocks.LAVA_CAULDRON && maxDrain >= 1000) {
                if (action.execute()) {
                    level.setBlock(
                            pos,
                            Blocks.CAULDRON.defaultBlockState(),
                            Block.UPDATE_ALL
                    );
                }
                return new FluidStack(Fluids.LAVA, 1000);
            }
            return FluidStack.EMPTY;
        }
    }
}
