package org.sosy_lab.cpachecker.cpa.usage.storage;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleNode;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleState;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo.Access;
import org.sosy_lab.cpachecker.util.Pair;

public class UsagePoint implements Comparable<UsagePoint> {

  private static class UsagePointWithEmptyLockSet extends UsagePoint {
    //This usage is used to distinct usage points with empty lock sets with write access from each other
    public final UsageInfo keyUsage;

    private UsagePointWithEmptyLockSet(List<CompatibleNode> nodes, Access pAccess, UsageInfo pInfo) {
      super(nodes, pAccess);
      keyUsage = Objects.requireNonNull(pInfo);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + Objects.hashCode(keyUsage);
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      boolean result = super.equals(obj);
      if (!result) {
        return result;
      }
      UsagePointWithEmptyLockSet other = (UsagePointWithEmptyLockSet) obj;
      //This is for distinction usages with empty sets of locks

      return Objects.equals(keyUsage, other.keyUsage);
    }

    @Override
    public int compareTo(UsagePoint o) {
      int result = super.compareTo(o);
      if (result != 0) {
        return result;
      }
      // If we have 'result == 0' above,
      // the other UsagePoint should be also the same class
      Preconditions.checkArgument(o instanceof UsagePointWithEmptyLockSet);
      return keyUsage.compareTo(((UsagePointWithEmptyLockSet)o).keyUsage);
    }

    @Override
    public boolean covers(UsagePoint o) {
      //Here can be read accesses without locks
      if (this.access == Access.READ && o.access == Access.READ) {
        return super.covers(o);
      }
      /* Key usage is important, if it is present, it is write access without locks,
       * and we should handle all of them without inserting into covered elements of the tree structure
       */
      return false;
    }

    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public String toString() {
      String result = super.toString();
      result += ", " + keyUsage.getLine();
      return result;
    }
  }

  public final Access access;
  private final List<CompatibleNode> compatibleNodes;
  private final Set<UsagePoint> coveredUsages;

  private UsagePoint(List<CompatibleNode> nodes, Access pAccess) {
    access = pAccess;
    coveredUsages = new TreeSet<>();
    compatibleNodes = nodes;
  }

  public static UsagePoint createUsagePoint(UsageInfo info) {

    Access accessType = info.getAccess();

    FluentIterable<CompatibleNode> nodes =
        from(info.getAllCompatibleStates())
        .transform(CompatibleState::getTreeNode);

    if (nodes.allMatch(CompatibleNode::hasEmptyLockSet)) {
      return new UsagePointWithEmptyLockSet(nodes.toList(), accessType, info);
    } else {
      return new UsagePoint(nodes.toList(), accessType);
    }

  }

  public boolean addCoveredUsage(UsagePoint newChild) {
    if (!coveredUsages.contains(newChild)) {

      Optional<UsagePoint> usage = from(coveredUsages)
                         .firstMatch(u -> u.covers(newChild));

      if (usage.isPresent()) {
        assert !usage.get().equals(newChild);
        return usage.get().addCoveredUsage(newChild);
      }
      return coveredUsages.add(newChild);
    }
    return false;
  }

  public Set<UsagePoint> getCoveredUsages() {
    return coveredUsages;
  }

  @Override
  public int hashCode() {
    return Objects.hash(access, compatibleNodes);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null ||
        getClass() != obj.getClass()) {
      return false;
    }
    UsagePoint other = (UsagePoint) obj;
    return access == other.access
        && Objects.equals(compatibleNodes, other.compatibleNodes);
  }

  @Override
  public int compareTo(UsagePoint o) {
    //It is very important to compare at first the accesses, because an algorithm base on this suggestion
    int result = access.compareTo(o.access);
    if (result != 0) {
      return result;
    }
    Preconditions.checkArgument(compatibleNodes.size() == o.compatibleNodes.size());
    for (int i = 0; i < compatibleNodes.size(); i++) {
      CompatibleNode currentNode = compatibleNodes.get(i);
      CompatibleNode otherNode = o.compatibleNodes.get(i);
      result = currentNode.compareTo(otherNode);
      if (result != 0) {
        return result;
      }
    }
    return result;
  }

  //TODO CompareTo? with enums
  public boolean covers(UsagePoint o) {
    // access 'write' is higher than 'read', but only for nonempty locksets
    if (access.compareTo(o.access) > 0) {
      return false;
    }

    return from(Pair.zipList(compatibleNodes, o.compatibleNodes))
           .allMatch(p -> p.getFirst().cover(p.getSecond()));
  }

  public boolean isCompatible(UsagePoint other) {
    return from(Pair.zipList(compatibleNodes, other.compatibleNodes))
           .allMatch(p -> p.getFirst().isCompatibleWith(p.getSecond()));
  }

  public boolean isEmpty() {
    //The empty points are the special class
    return false;
  }

  @Override
  public String toString() {
    return access + ":" + compatibleNodes;
  }

  public CompatibleNode get(Class<? extends CompatibleNode> pClass) {
    for (CompatibleNode node : compatibleNodes) {
      if (node.getClass() == pClass) {
        return node;
      }
    }
    return null;
  }
}
