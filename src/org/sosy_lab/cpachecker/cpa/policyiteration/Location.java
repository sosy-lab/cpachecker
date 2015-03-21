package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.Collection;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState;
import org.sosy_lab.cpachecker.cpa.loopstack.LoopstackState;
import org.sosy_lab.cpachecker.util.UniqueIdGenerator;

import com.google.common.base.Objects;
import com.google.common.base.Predicates;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * Object uniquely identifying the location in the stack trace.
 */
public final class Location {
  private final CFANode node;

  // Use other CPA states for partitioning.
  private final ImmutableSet<AbstractState> otherStates;
  private Integer locationID = null;
  private final int hashCache;

  // Serializing to and from integer.
  private static final UniqueIdGenerator locationCounter = new UniqueIdGenerator();
  private static final BiMap<Integer, Location> serializationMap = HashBiMap.create();

  private Location(CFANode pNode, Iterable<AbstractState> pOtherStates) {
    node = pNode;
    otherStates = ImmutableSet.copyOf(pOtherStates);
    hashCache = Objects.hashCode(node, otherStates);
    //noinspection ResultOfMethodCallIgnored
    toID();
  }

  public static Location of(
      CFANode pNode, Collection<AbstractState> otherStates
  ) {
    @SuppressWarnings("unchecked")
    Iterable<AbstractState> filteredStates = Iterables.filter(otherStates,
        Predicates.<AbstractState>or(
            Predicates.instanceOf(CallstackState.class),
            Predicates.instanceOf(LoopstackState.class),
            Predicates.instanceOf(FunctionPointerState.class)
        ));
    return new Location(pNode, filteredStates);
  }

  public CFANode getFinalNode() {
    return node;
  }

  /**
   * Initial Location.
   */
  public static Location initial(CFANode initial) {
    return new Location(initial, ImmutableList.<AbstractState>of());
  }

  /**
   * De-serialize location from an identifier.
   */
  public static Location ofID(int l) {
    return serializationMap.get(l);
  }

  /**
   * Serialize location to an identifier.
   * Guarantees: identifier is always non-negative.
   */
  public int toID() {
    if (locationID != null) {
      return locationID;
    }

    // Making sure that equal location will have the same identifier.
    if (serializationMap.inverse().containsKey(this)) {
      locationID = serializationMap.inverse().get(this);
    } else {
      locationID = locationCounter.getFreshId();
      serializationMap.put(locationID, this);
    }
    return locationID;
  }

  @Override
  public String toString() {
    return String.format("%s(%s)", node, locationID);
  }

  @Override
  public int hashCode() {
    return hashCache;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (o.getClass() != getClass()) {
      return false;
    }
    Location other = (Location) o;
    return node == other.node && otherStates.equals(other.otherStates);
  }
}
