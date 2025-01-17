package ca.teamdman.sfm.common.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// The content changes between the versions of Minecraft.
@Retention(RetentionPolicy.CLASS)
@Target({
        ElementType.METHOD,
        ElementType.FIELD,
        ElementType.PARAMETER,
        ElementType.LOCAL_VARIABLE,
        ElementType.TYPE_USE
})
public @interface MCVersionDependentBehaviour {
}
