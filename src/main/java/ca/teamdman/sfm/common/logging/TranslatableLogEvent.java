package ca.teamdman.sfm.common.logging;

import ca.teamdman.sfm.common.util.SFMTranslationUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;

public record TranslatableLogEvent(
        Level level,
        Instant instant,
        TranslatableContents contents
) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(level.name());
        buf.writeLong(instant.getEpochMillisecond());
        buf.writeInt(instant.getNanoOfMillisecond());
        SFMTranslationUtils.encodeTranslation(contents, buf);
    }
    public static TranslatableLogEvent decode(FriendlyByteBuf buf) {
        var level = Level.getLevel(buf.readUtf());
        var epochMillisecond = buf.readLong();
        var epochNano = buf.readInt();
        var contents = SFMTranslationUtils.decodeTranslation(buf);

        var instant = new MutableInstant();
        instant.initFromEpochMilli(epochMillisecond, epochNano);

        return new TranslatableLogEvent(level, instant, contents);
    }
}
