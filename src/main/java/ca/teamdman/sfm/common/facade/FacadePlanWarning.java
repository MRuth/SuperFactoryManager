package ca.teamdman.sfm.common.facade;

import net.minecraft.network.chat.MutableComponent;

public record FacadePlanWarning(
        MutableComponent confirmTitle,
        MutableComponent confirmMessage,
        MutableComponent confirmYes,
        MutableComponent confirmNo
) {
}
