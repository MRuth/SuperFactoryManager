package ca.teamdman.sfm;

import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Allows for disabling certain performance tweaks for analysis and debugging purposes
 */
public class SFMPerformanceTweaks {
    public static final boolean OBJECT_POOL_VALIDATION = !FMLEnvironment.production;
    public static final boolean OBJECT_POOL_ENABLED = true;
    public static final boolean REGEX_CACHE_ENABLED = true;
    public static final boolean REGEX_PREDICATE_OPTIMIZATION = true;
}
