package ca.teamdman.sfm.common.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// A BlockPos passed here will not be stored as a reference beyond the method call.
/// BlockPos::asLong may be used but that's fine.
/// It's fine to pass a MutableBlockPos here.
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.PARAMETER})
public @interface NotStored {
}
