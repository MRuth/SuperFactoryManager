package ca.teamdman.sfm.common.localization;

import ca.teamdman.sfm.common.util.SFMTranslationUtils;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.function.Supplier;

public record LocalizationEntry(
        Supplier<String> key,
        Supplier<String> value
) {
    public LocalizationEntry(
            String key,
            String value
    ) {
        this(() -> key, () -> value);
    }

    public TranslatableContents get(Object... args) {
        return SFMTranslationUtils.getTranslatableContents(key.get(), args);
    }

    public TranslatableContents get() {
        return SFMTranslationUtils.getTranslatableContents(key.get());
    }

    public String getString() {
        return I18n.get(key.get());
    }

    public String getString(Object... args) {
        return I18n.get(key.get(), args);
    }

    public MutableComponent getComponent() {
        return Component.translatable(key.get());
    }

    public MutableComponent getComponent(Object... args) {
        return Component.translatable(key.get(), args);
    }
}
