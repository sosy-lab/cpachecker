package org.sosy_lab.cpachecker.cpa.usage.storage;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleState;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo.Access;
import org.sosy_lab.cpachecker.cpa.usage.UsageTreeNode;

public class UsagePoint implements Comparable<UsagePoint> {

  private static class UsagePointWithEmptyLockSet extends UsagePoint {
    //This usage is used to distinct usage points with empty lock sets with write access from each other
    public final UsageInfo keyUsage;

    private UsagePointWithEmptyLockSet(List<UsageTreeNode> nodes, Access pAccess, UsageInfo pInfo) {
      super(nodes, pAccess);
      assert pInfo != null;
      keyUsage = pInfo;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((keyUsage == null) ? 0 : keyUsage.hashCode());
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
      if (keyUsage == null) {
        if (other.keyUsage != null) {
          return false;
        }
      } else if (!keyUsage.equals(other.keyUsage)) {
        return false;
      }
      return true;
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
  private final List<UsageTreeNode> compatibleNodes;
  private final Set<UsagePoint> coveredUsages;

  private UsagePoint(List<UsageTreeNode> nodes, Access pAccess) {
    access = pAccess;
    coveredUsages = new TreeSet<>();
    compatibleNodes = nodes;
  }

  public static UsagePoint createUsagePoint(UsageInfo info) {

    Access accessType = info.getAccess();

    FluentIterable<UsageTreeNode> nodes =
        from(info.getAllCompatibleStates())
        .transform(CompatibleState::getTreeNode);

    if (nodes.allMatch(UsageTreeNode::hasEmptyLockSet)) {
      return new UsagePointWithEmptyLockSet(nodes.toList(), accessType, info);
    } else {
      return new UsagePoint(nodes.toList(), accessType);
    }

  }

  public boolean addCoveredUsage(UsagePoint newChild) {
    if (!coveredUsages.contains(newChild)) {
      for (UsagePoint usage : coveredUsages) {
        if (usage.covers(newChild)) {
          assert !usage.equals(newChild);
          return usage.addCoveredUsage(newChild);
        }
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
    final int prime = 31;
    int result = 1;
    result = prime * result + ((access == null) ? 0 : access.hashCode());
    result = prime * result + ((compatibleNodes == null) ? 0 : compatibleNodes.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    UsagePoint other = (UsagePoint) obj;
    if (access != other.access) {
      return false;
    }
    if (compatibleNodes == null) {
      if (other.compatibleNodes != null) {
        return false;
      }
    } else if (!compatibleNodes.equals(other.compatibleNodes)) {
      return false;
    }
    return true;
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
      UsageTreeNode currentNode = compatibleNodes.get(i);
      UsageTreeNode otherNode = o.compatibleNodes.get(i);
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
    Preconditions.checkArgument(compatibleNodes.size() == o.compatibleNodes.size());
    for (int i = 0; i < compatibleNodes.size(); i++) {
      UsageTreeNode currentNode = compatibleNodes.get(i);
      UsageTreeNode otherNode = o.compatibleNodes.get(i);
      if(!currentNode.cover(otherNode)) {
        return false;
      }
    }
    return true;
  }

  public boolean isCompatible(UsagePoint other) {
    Preconditions.checkArgument(compatibleNodes.size() == other.compatibleNodes.size());
    for (int i = 0; i < compatibleNodes.size(); i++) {
      UsageTreeNode currentNode = compatibleNodes.get(i);
      UsageTreeNode otherNode = other.compatibleNodes.get(i);
      if(!currentNode.isCompatibleWith(otherNode)) {
        return false;
      }
    }
    return true;
  }

  public boolean isEmpty() {
    //The empty points are the special class
    return false;
  }

  @Override
  public String toString() {
    return access + ":" + compatibleNodes;
  }
}
