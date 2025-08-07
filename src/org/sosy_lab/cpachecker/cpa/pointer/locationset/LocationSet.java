package org.sosy_lab.cpachecker.cpa.pointer.locationset;

import java.util.Set;
import org.sosy_lab.cpachecker.cpa.pointer.pointertarget.PointerTarget;

/**
 * A set of abstract memory locations (i.e., pointer targets) that a symbolic pointer may refer to.
 *
 * <p>This abstraction is used in the pointer analysis CPA to represent the points-to sets of
 * symbolic addresses. It can represent bottom (⊥), top (⊤), or any combination of concrete memory
 * targets.
 *
 * <p><strong>Immutability:</strong> All implementations of this interface are strictly immutable.
 * Any modifying operation (e.g., adding elements) returns a new instance rather than mutating the
 * existing object.
 */
public sealed interface LocationSet permits ExplicitLocationSet, LocationSetBot, LocationSetTop {

  /**
   * Returns a new {@link LocationSet} that includes all elements of this set and the given targets,
   * with additional information about null inclusion.
   *
   * @param pLocations The set of pointer targets to include.
   * @return A new LocationSet containing the combined information.
   */
  LocationSet withPointerTargets(Set<PointerTarget> pLocations);

  /**
   * Returns a new {@link LocationSet} that merges the contents of this set and another.
   *
   * <p>This is a general-purpose union operator for two abstract location sets.
   *
   * @param pLocations Another {@link LocationSet} whose elements are to be added.
   * @return A new LocationSet containing all elements from both sets.
   */
  LocationSet withPointerTargets(LocationSet pLocations);

  /**
   * Checks whether this location set is bottom (⊥), meaning it contains no possible locations.
   *
   * @return {@code true} if the set is empty and does not contain null; {@code false} otherwise.
   */
  boolean isBot();

  /**
   * Checks whether this location set is top (⊤), meaning it may refer to any memory location.
   *
   * @return {@code true} if the set is top; {@code false} otherwise.
   */
  boolean isTop();

  /**
   * Checks whether this set only represents the {@code null} pointer.
   *
   * @return {@code true} if the set contains only null and no other targets; {@code false}
   *     otherwise.
   */
  boolean containsAllNulls();

  /**
   * Determines whether this set contains the null pointer.
   *
   * @return {@code true} if {@code null} is represented in this set; {@code false} otherwise.
   */
  boolean containsAnyNull();

  /**
   * Determines whether this location set contains the given target.
   *
   * @param pLocation The {@link PointerTarget} to check for membership.
   * @return {@code true} if this set contains the given location, {@code false} otherwise.
   */
  boolean contains(PointerTarget pLocation);

  /**
   * Checks whether all elements in the given {@link LocationSet} are contained in this one.
   *
   * @param pLocations The other location set to check for subset inclusion.
   * @return {@code true} if all elements (including null if applicable) of {@code pLocations} are
   *     contained in this set; {@code false} otherwise.
   */
  boolean containsAll(LocationSet pLocations);
}
