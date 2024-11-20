package ca.teamdman.sfml.ast;

import ca.teamdman.sfm.common.program.IInputResourceTracker;
import ca.teamdman.sfm.common.program.IOutputResourceTracker;
import ca.teamdman.sfm.common.registry.SFMResourceTypes;
import ca.teamdman.sfm.common.resourcetype.ResourceType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A set of {@link ResourceLimit} objects.
 * Do NOT modify this after creation since the {@link this#referencedResourceTypes} will become inaccurate.
 */
public final class ResourceLimits implements ASTNode, ToStringPretty {
    private @Nullable LinkedHashSet<ResourceType<?,?,?>> referencedResourceTypes = null;
    private final List<ResourceLimit> resourceLimitList;
    private final ResourceIdSet exclusions;

    /**
     *
     */
    public ResourceLimits(
            List<ResourceLimit> resourceLimitList,
            ResourceIdSet exclusions
    ) {
        this.resourceLimitList = resourceLimitList;
        this.exclusions = exclusions;
    }

    public List<IInputResourceTracker> createInputTrackers() {
        List<IInputResourceTracker> rtn = new ObjectArrayList<>(resourceLimitList.size());
        for (ResourceLimit rl : resourceLimitList) {
            rtn.add(rl.createInputTracker(exclusions));
        }
        return rtn;
    }

    public List<IOutputResourceTracker> createOutputTrackers() {
        List<IOutputResourceTracker> rtn = new ObjectArrayList<>(resourceLimitList.size());
        for (ResourceLimit rl : resourceLimitList) {
            rtn.add(rl.createOutputTracker(exclusions));
        }
        return rtn;
    }

    public ResourceLimits withDefaultLimit(Limit limit) {
        List<ResourceLimit> defaulted = new ObjectArrayList<>(this.resourceLimitList.size());
        for (ResourceLimit rl : this.resourceLimitList) {
            defaulted.add(rl.withDefaultLimit(limit));
        }
        return new ResourceLimits(
                defaulted,
                exclusions
        );
    }

    public ResourceLimits withExclusions(ResourceIdSet exclusions) {
        return new ResourceLimits(resourceLimitList, exclusions);
    }

    /**
     * See also: {@link ResourceIdSet#getReferencedResourceTypes()}
     */
    public Set<ResourceType<?, ?, ?>> getReferencedResourceTypes() {
        if (referencedResourceTypes == null) {
            referencedResourceTypes = new LinkedHashSet<>(SFMResourceTypes.getResourceTypeCount());
            for (ResourceLimit resourceLimit : resourceLimitList) {
                referencedResourceTypes.addAll(resourceLimit.resourceIds().getReferencedResourceTypes());
            }
        }
        return referencedResourceTypes;
    }

    @Override
    public String toString() {
        String rtn = this.resourceLimitList.stream()
                .map(ResourceLimit::toString)
                .collect(Collectors.joining(",\n"));
        if (!exclusions.isEmpty()) {
            rtn += "\nEXCEPT\n" + exclusions.stream()
                    .map(ResourceIdentifier::toString)
                    .collect(Collectors.joining(",\n"));
        }
        return rtn;
    }

    public String toStringCondensed(Limit defaults) {
        String rtn = resourceLimitList.stream()
                .map(rl -> rl.toStringCondensed(defaults))
                .map(x -> resourceLimitList.size() == 1 ? x : x + ",")
                .collect(Collectors.joining("\n"));
        if (!exclusions.isEmpty()) {
            rtn += "\nEXCEPT\n" + exclusions.stream()
                    .map(ResourceIdentifier::toStringCondensed)
                    .collect(Collectors.joining(",\n"));
        }
        return rtn;
    }

    public List<ResourceLimit> resourceLimitList() {
        return resourceLimitList;
    }

    public ResourceIdSet exclusions() {
        return exclusions;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ResourceLimits) obj;
        return Objects.equals(this.resourceLimitList, that.resourceLimitList) &&
               Objects.equals(this.exclusions, that.exclusions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceLimitList, exclusions);
    }

}