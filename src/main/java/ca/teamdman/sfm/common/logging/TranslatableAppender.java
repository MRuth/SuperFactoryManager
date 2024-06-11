package ca.teamdman.sfm.common.logging;

import net.minecraft.network.chat.contents.TranslatableContents;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.message.Message;

import java.io.Serializable;
import java.util.LinkedList;

@Plugin(name = "TranslatableAppender", category = "Core", elementType = Appender.ELEMENT_TYPE, printObject = true)
public class TranslatableAppender extends AbstractAppender {
    public final LinkedList<TranslatableLogEvent> contents;

    protected TranslatableAppender(String name, Layout<? extends Serializable> layout) {
        super(name, null, layout, true, null);
        this.contents = new LinkedList<>();
    }

    @PluginFactory
    public static TranslatableAppender createAppender(@PluginBuilderAttribute("name") String name) {
        Layout<? extends Serializable> layout = PatternLayout.createDefaultLayout();
        return new TranslatableAppender(name, layout);
    }

    @Override
    public void append(LogEvent event) {
        // clone since the event is mutable
        var instant = new MutableInstant();
        instant.initFrom(event.getInstant());

        Level level = event.getLevel();

        Message message = event.getMessage();
        TranslatableContents content = new TranslatableContents(message.getFormat(), message.getParameters());

        contents.add(new TranslatableLogEvent(
                level,
                instant,
                content
        ));
//        SFM.LOGGER.debug("Appended log event to {}", System.identityHashCode(this));
    }
}