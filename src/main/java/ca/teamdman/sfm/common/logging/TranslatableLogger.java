package ca.teamdman.sfm.common.logging;

import ca.teamdman.sfm.SFM;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.network.NetworkHooks;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.time.Instant;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TranslatableLogger {
    private static final LoggerContext CONTEXT = new LoggerContext(SFM.MOD_ID);
    private final Logger logger;
    private Level logLevel = Level.OFF;

    /// shortest circuit for performance optimization
    private boolean active = false;

    /// faster than logger.isEnabled
    private static final Map<Level, Predicate<Level>> shouldLogGivenCurrent = Map.of(
            Level.OFF, currentLevel -> false,
            Level.FATAL, currentLevel -> currentLevel != Level.OFF,
            Level.ERROR, currentLevel -> currentLevel != Level.OFF && currentLevel != Level.FATAL,
            Level.WARN, currentLevel ->  currentLevel != Level.OFF && currentLevel != Level.FATAL && currentLevel != Level.ERROR,
            Level.INFO, currentLevel -> currentLevel == Level.INFO || currentLevel == Level.DEBUG || currentLevel == Level.TRACE || currentLevel == Level.ALL,
            Level.DEBUG, currentLevel -> currentLevel == Level.DEBUG || currentLevel == Level.TRACE || currentLevel == Level.ALL,
            Level.TRACE, currentLevel -> currentLevel == Level.TRACE || currentLevel == Level.ALL,
            Level.ALL, currentLevel -> true
    );

    public TranslatableLogger(String name) {
        // Create logger
        this.logger = CONTEXT.getLogger(name);


        // Register the config to the logger
        Configuration configuration = CONTEXT.getConfiguration();
        configuration.removeLogger(name);
        LoggerConfig config = new LoggerConfig(name, logLevel, false);
        configuration.addLogger(name, config);

        // Create appender
        TranslatableAppender appender = TranslatableAppender.createAppender(name);

        // Unregister any previous appenders that may have been created for this location
        config.removeAppender(name);

        // Attach the appender to the logger
        config.addAppender(appender, Level.TRACE, null);

        // Start the appender
        appender.start();
    }

    public static boolean comesAfter(Instant a, Instant b) {
        return a.getEpochSecond() > b.getEpochSecond() || (
                a.getEpochSecond() == b.getEpochSecond()
                && a.getNanoOfSecond() > b.getNanoOfSecond()
        );
    }

    public static ArrayDeque<TranslatableLogEvent> decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        ArrayDeque<TranslatableLogEvent> contents = new ArrayDeque<>(size);
        for (int i = 0; i < size; i++) {
            contents.add(TranslatableLogEvent.decode(buf));
        }
        return contents;
    }

    /**
     * Writes logs to the buffer.
     * Will safely stop writing once the buffer is full.
     * Will remove from the list the logs that were written.
     *
     * @see NetworkHooks#openScreen(ServerPlayer, MenuProvider, Consumer) the byte limit
     */
    public static void encodeAndDrain(Collection<TranslatableLogEvent> logs, FriendlyByteBuf buf) {
        int maxReadableBytes = 32600;
        FriendlyByteBuf chunk = new FriendlyByteBuf(Unpooled.buffer());
        int count = 0;
        for (Iterator<TranslatableLogEvent> iterator = logs.iterator(); iterator.hasNext(); ) {
            TranslatableLogEvent entry = iterator.next();
            FriendlyByteBuf check = new FriendlyByteBuf(Unpooled.buffer());
            entry.encode(check);
            if (check.readableBytes() + chunk.readableBytes() + buf.readableBytes() >= maxReadableBytes) {
                break;
            }
            chunk.writeBytes(check);
            iterator.remove();
            count += 1;
        }

        buf.writeVarInt(count);
        buf.writeBytes(chunk);
    }

    public Level getLogLevel() {
        return CONTEXT.getConfiguration().getLoggerConfig(logger.getName()).getLevel();
    }

    public void setLogLevel(Level level) {
        LoggerConfig found = CONTEXT.getConfiguration().getLoggerConfig(logger.getName());
        found.setLevel(level);

        this.active = level != Level.OFF;
        this.logLevel = level;
        CONTEXT.updateLoggers();
    }

    private LinkedList<TranslatableLogEvent> getContents() {
        var appenders = CONTEXT.getConfiguration().getLoggerConfig(logger.getName()).getAppenders();
        if (appenders.containsKey(logger.getName())) {
            var appender = appenders.get(logger.getName());
            if (appender instanceof TranslatableAppender ta) {
                return ta.contents;
            }
        }
        return new LinkedList<>();
    }

    public ArrayDeque<TranslatableLogEvent> getLogsAfter(Instant instant) {
        List<TranslatableLogEvent> contents = getContents();
        ArrayDeque<TranslatableLogEvent> toSend = new ArrayDeque<>();
        // Add from tail until we reach the last sent marker
        var iter = contents.listIterator(contents.size());
        while (iter.hasPrevious()) {
            var entry = iter.previous();
            if (comesAfter(entry.instant(), instant)) {
                toSend.addFirst(entry);
            } else {
                break;
            }
        }
        return toSend;
    }

    public ArrayDeque<TranslatableLogEvent> getLogs() {
        return new ArrayDeque<>(getContents());
    }

    public void pruneSoWeDontEatAllTheRam() {
        List<TranslatableLogEvent> contents = getContents();
        if (contents.size() > 10_000) {
            int overage = contents.size() - 10_000;
            int to_prune = overage + 500;
            contents.subList(0, to_prune).clear();
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isLevelEnabled(Level attempting) {
        return active && shouldLogGivenCurrent.get(attempting).test(logLevel);
    }

    public void info(TranslatableContents contents) {
        if (!this.active) return;
        logger.info(contents.getKey(), contents.getArgs());
    }

    public void info(Consumer<Consumer<TranslatableContents>> logger) {
        if (!this.isLevelEnabled(Level.INFO)) return;
        logger.accept(this::info);
    }

    public void warn(TranslatableContents contents) {
        if (!this.active) return;
        logger.warn(contents.getKey(), contents.getArgs());
    }

    public void warn(Consumer<Consumer<TranslatableContents>> logger) {
        if (!this.isLevelEnabled(Level.WARN)) return;
        logger.accept(this::warn);
    }

    public void error(TranslatableContents contents) {
        if (!this.active) return;
        logger.error(contents.getKey(), contents.getArgs());
    }

    public void error(Consumer<Consumer<TranslatableContents>> logger) {
        if (!this.isLevelEnabled(Level.ERROR)) return;
        logger.accept(this::error);
    }

    public void debug(TranslatableContents contents) {
        if (!this.active) return;
        logger.debug(contents.getKey(), contents.getArgs());
    }

    public void debug(Consumer<Consumer<TranslatableContents>> logger) {
        if (!this.isLevelEnabled(Level.DEBUG)) return;
        logger.accept(this::debug);
    }

    public void trace(TranslatableContents contents) {
        if (!this.active) return;
        logger.trace(contents.getKey(), contents.getArgs());
    }

    public void trace(Consumer<Consumer<TranslatableContents>> logger) {
        if (!this.isLevelEnabled(Level.TRACE)) return;
        logger.accept(this::trace);
    }

    public void clear() {
        getContents().clear();
    }
}
