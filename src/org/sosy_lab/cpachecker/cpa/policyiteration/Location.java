package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.List;

import org.sosy_lab.cpachecker.cfa.model.CFANode;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;

/**
 * Object uniquely identifying the location in the stack trace.
 */
public final class Location {
  final CFANode node;

  /**
   * Sequence of caller nodes.
   * Analysis should add one for each
   * {@link org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge} seen.
   */
  final ImmutableList<CFANode> callerNodes;

  private static int locationCounter = -1;
  private static final BiMap<Integer, Location> serializationMap = HashBiMap.create();
  private final int locationID;

  private Location(CFANode pNode, List<CFANode> pCallerNodes) {
    node = pNode;
    callerNodes = ImmutableList.copyOf(pCallerNodes);

    // Making sure that equal location will have the same identifier.
    if (serializationMap.inverse().containsKey(this)) {

      // NOTE: check how dangerous this is.
      // Normally we shouldn't let {@code this} reference escape during the
      // construction, but in this case it appears fine.
      locationID = serializationMap.inverse().get(this);
    } else {
      locationID = ++locationCounter;
      serializationMap.put(locationID, this);
    }
  }

  /**
   * Initial Location.
   */
  public static Location initial(CFANode initial) {
    return new Location(initial, ImmutableList.<CFANode>of());
  }

  public static Location withCallsite(Location old,
      CFANode pCallsite,
      CFANode node) {
    return new Location(
        node,
        ImmutableList.<CFANode>builder()
        .addAll(old.callerNodes)
        .add(pCallsite)
        .build());
  }

  public static Location popCallsite(Location old, CFANode node) {
    return new Location(
        node,
        old.callerNodes.subList(0, old.callerNodes.size() - 1));
  }

  public static Location ofID(int l) {
    return serializationMap.get(l);
  }

  public int toID() {
    return locationID;
  }

  public static Location withNode(Location old, CFANode node) {
    return new Location(node, old.callerNodes);
  }

  @Override
  public String toString() {
    return String.format("%s(%s)", node, Joiner.on(",").join(callerNodes));
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(node, callerNodes);
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
    return node == other.node && callerNodes.equals(other.callerNodes);
  }
}
