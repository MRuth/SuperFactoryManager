package ca.teamdman.sfm.common.program;

import ca.teamdman.sfml.ast.ResourceLimit;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

public class InputResourceMatcher<STACK, CAP> extends ResourceMatcher<STACK, CAP> {

    private final Long2LongMap PROMISED      = new Long2LongOpenHashMap();
    private       int          promisedCount = 0;

    public InputResourceMatcher(ResourceLimit<STACK, CAP> limit) {
        super(limit);
    }

    public boolean isDone() {
        return transferred >= LIMIT.limit().quantity() - LIMIT.limit().retention();
    }

    public long getExistingPromise(int slot) {
        return PROMISED.getOrDefault(slot, 0);
    }

    public long getRemainingPromise() {
        long needed = LIMIT.limit().retention() - promisedCount;
        return needed;
    }

    public void track(long slot, long transferred, long promise) {
        this.transferred += transferred;
        this.promisedCount += promise;
        this.PROMISED.merge(slot, promise, Long::sum);
    }
}
