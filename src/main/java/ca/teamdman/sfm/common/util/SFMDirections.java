package ca.teamdman.sfm.common.util;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;

public class SFMDirections {
    public static final Direction[] DIRECTIONS = Direction.values();
    public static final Direction[] DIRECTIONS_WITH_NULL = new Direction[]{
            null,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.EAST,
            Direction.WEST,
            Direction.UP,
            Direction.DOWN
    };

    public static class NullableDirectionIterator implements Iterator<Direction> {
        private int index = 0;

        @Override
        public boolean hasNext() {
            return index < DIRECTIONS_WITH_NULL.length;
        }

        @Override
        public @Nullable Direction next() {
            if (hasNext()) {
                return DIRECTIONS_WITH_NULL[index++];
            }
            throw new NoSuchElementException();
        }
    }

    public static class SingleNullDirectionIterator implements Iterator<Direction> {
        private boolean hasNext = true;

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public @Nullable Direction next() {
            if (hasNext) {
                hasNext = false;
                return null;
            }
            throw new NoSuchElementException();
        }
    }

    public record NullableDirectionEnumMap<T>(T[] buckets) {
        public NullableDirectionEnumMap() {
            //noinspection unchecked
            this((T[]) new Object[DIRECTIONS_WITH_NULL.length]);
        }

        public boolean containsKey(@Nullable Direction direction) {
            return buckets[keyFor(direction)] != null;
        }

        public void forEach(BiConsumer<Direction, T> callback) {
            for (Direction direction : DIRECTIONS_WITH_NULL) {
                T value = buckets[keyFor(direction)];
                if (value != null) {
                    callback.accept(direction, value);
                }
            }
        }

        public void remove(@Nullable Direction direction) {
            buckets[keyFor(direction)] = null;
        }

        public boolean isEmpty() {
            for (T bucket : buckets) {
                if (bucket != null) {
                    return false;
                }
            }
            return true;
        }

        public void put(
                @Nullable Direction direction,
                T value
        ) {
            buckets[keyFor(direction)] = value;
        }

        public @Nullable T get(@Nullable Direction direction) {
            return buckets[keyFor(direction)];
        }

        public int size() {
            int count = 0;
            for (T bucket : buckets) {
                if (bucket != null) {
                    count++;
                }
            }
            return count;
        }

        private int keyFor(@Nullable Direction direction) {
            return direction == null ? 0 : direction.ordinal() + 1;
        }
    }
}
