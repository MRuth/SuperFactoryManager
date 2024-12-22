package ca.teamdman.sfm.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.util.Lazy;

public class ClientKeyHelpers {
    public static boolean isKeyDown(Lazy<KeyMapping> key) {
        // special effort is needed to ensure this works properly when the manager screen is open
        // https://github.com/mekanism/Mekanism/blob/f92b48a49e0766cd3aa78e95c9c4a47ba90402f5/src/main/java/mekanism/client/key/MekKeyHandler.java
        long handle = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(
                handle,
                key.get().getKey().getValue()
        );
    }
}
