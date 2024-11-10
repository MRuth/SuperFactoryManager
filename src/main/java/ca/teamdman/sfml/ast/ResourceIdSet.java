package ca.teamdman.sfml.ast;

import ca.teamdman.sfm.common.registry.SFMResourceTypes;
import ca.teamdman.sfm.common.resourcetype.ResourceType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A read-only set of {@link ResourceIdentifier} objects.
 * Do NOT modify this after creation since the {@link this#referencedResourceTypes} will become inaccurate.
 */
public final class ResourceIdSet implements ASTNode {
    public static final ResourceIdSet EMPTY = new ResourceIdSet(new LinkedHashSet<>());
    public static final ResourceIdSet MATCH_ALL = new ResourceIdSet(new LinkedHashSet<>(List.of(ResourceIdentifier.MATCH_ALL)));
    private final ArrayList<ResourceIdentifier<?, ?, ?>> resourceIds;
    private @Nullable List<ResourceType<?,?,?>> referencedResourceTypes;

    public ResourceIdSet(Collection<ResourceIdentifier<?, ?, ?>> contents) {
        this(new LinkedHashSet<>(contents));
    }

    /**
     * See also: {@link ResourceLimits#getReferencedResourceTypes()}
     */
    public Collection<? extends ResourceType<?, ?, ?>> getReferencedResourceTypes() {
        if (referencedResourceTypes == null) {
            HashSet<ResourceType<?, ?, ?>> set = new HashSet<>(SFMResourceTypes.getResourceTypeCount());
            for (ResourceIdentifier<?, ?, ?> resourceId : resourceIds) {
                set.add(resourceId.getResourceType());
            }

            referencedResourceTypes = new ArrayList<>(set);
        }
        return referencedResourceTypes;
    }

    public boolean couldMatchMoreThanOne() {
        return resourceIds.size() > 1 || stream().anyMatch(ResourceIdentifier::usesRegex);
    }

    public int size() {
        return resourceIds.size();
    }

    public boolean isEmpty() {
        return resourceIds.isEmpty();
    }

    public ResourceIdSet(
            LinkedHashSet<ResourceIdentifier<?, ?, ?>> resourceIds
    ) {
        this.resourceIds = new ArrayList<>(resourceIds);
    }

    public @Nullable ResourceIdentifier<?, ?, ?> getMatchingFromStack(Object stack) {
        for (ResourceIdentifier<?, ?, ?> entry : resourceIds) {
            if (entry.matchesStack(stack)) {
                return entry;
            }
        }
        return null;
    }

    public boolean noneMatchStack(Object stack) {
        return getMatchingFromStack(stack) == null;
    }

    public boolean anyMatchResourceLocation(ResourceLocation location) {
        return resourceIds.stream().anyMatch(x -> x.matchesResourceLocation(location));
    }

    @Override
    public String toString() {
        return "ResourceIdSet{" +
               resourceIds.stream().map(ResourceIdentifier::toString).collect(Collectors.joining(", ")) +
               '}';
    }

    public String toStringCondensed() {
        return resourceIds.stream().map(ResourceIdentifier::toStringCondensed).collect(Collectors.joining(" OR "));
    }

    public Stream<ResourceIdentifier<?,?,?>> stream() {
        return resourceIds.stream();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ResourceIdSet) obj;
        return Objects.equals(this.resourceIds, that.resourceIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceIds);
    }

}
