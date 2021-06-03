// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.lock.AbstractLockState;
import org.sosy_lab.cpachecker.cpa.lock.LockState.LockTreeNode;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageDelta;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsagePoint;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;

public final class UsageInfo implements Comparable<UsageInfo> {

  public static enum Access {
    WRITE,
    READ;
  }

  private static class UsageCore {
    private final CFANode node;
    private final Access accessType;
    private AbstractState keyState;
    private List<CFAEdge> path;
    private final SingleIdentifier id;

    private boolean isLooped;

    private UsageCore(Access atype, CFANode n, SingleIdentifier ident) {
      node = n;
      accessType = atype;
      keyState = null;
      isLooped = false;
      id = ident;
    }
  }

  private static final UsageInfo IRRELEVANT_USAGE = new UsageInfo();

  private final UsageCore core;
  private final UsagePoint point;
  private final List<AbstractState> expandedStack;

  private UsageInfo() {
    core = null;
    point = null;
    expandedStack = null;
  }

  private UsageInfo(
      Access atype,
      CFANode n,
      SingleIdentifier ident,
      ImmutableList<CompatibleNode> pStates) {
    this(new UsageCore(atype, n, ident), new UsagePoint(pStates, atype), null);
  }

  private UsageInfo(UsageCore pCore, UsagePoint pPoint, List<AbstractState> pStack) {
    core = pCore;
    point = pPoint;
    expandedStack = pStack;
  }

  public static UsageInfo createUsageInfo(
      @NonNull Access atype, @NonNull AbstractState state, AbstractIdentifier ident) {
    if (ident instanceof SingleIdentifier) {
      ImmutableList.Builder<CompatibleNode> storedStates = ImmutableList.builder();

      for (CompatibleState s : AbstractStates.asIterable(state).filter(CompatibleState.class)) {
        if (!s.isRelevantFor((SingleIdentifier) ident)) {
          return IRRELEVANT_USAGE;
        }
        storedStates.add(s.getCompatibleNode());
      }
      UsageInfo result =
          new UsageInfo(
              atype,
              AbstractStates.extractLocation(state),
              (SingleIdentifier) ident,
              storedStates.build());
      result.core.keyState = state;
      return result;
    }
    return IRRELEVANT_USAGE;
  }

  public CFANode getCFANode() {
    return core.node;
  }

  public SingleIdentifier getId() {
    assert (core.id != null);
    return core.id;
  }

  public void setAsLooped() {
    core.isLooped = true;
  }

  public boolean isLooped() {
    return core.isLooped;
  }

  public boolean isRelevant() {
    return this != IRRELEVANT_USAGE;
  }

  @Override
  public int hashCode() {
    return Objects.hash(core.accessType, core.node, point);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    UsageInfo other = (UsageInfo) obj;
    return core.accessType == other.core.accessType
        && Objects.equals(core.node, other.core.node)
        && Objects.equals(point, other.point);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append(core.accessType);
    sb.append(" access to ");
    sb.append(core.id);
    LockTreeNode locks = getLockNode();
    if (locks == null) {
      // Lock analysis is disabled
    } else if (locks.size() == 0) {
      sb.append(" without locks");
    } else {
      sb.append(" with ");
      sb.append(locks);
    }

    return sb.toString();
  }

  public void setRefinedPath(List<CFAEdge> p) {
    core.keyState = null;
    core.path = p;
  }

  public AbstractState getKeyState() {
    return core.keyState;
  }

  public List<CFAEdge> getPath() {
    // assert path != null;
    return core.path;
  }

  @Override
  public int compareTo(UsageInfo pO) {
    int result;

    if (this == pO) {
      return 0;
    }
    result = point.compareTo(pO.point);
    if (result != 0) {
      return result;
    }

    result = this.core.node.compareTo(pO.core.node);
    if (result != 0) {
      return result;
    }
    result = this.core.accessType.compareTo(pO.core.accessType);
    if (result != 0) {
      return result;
    }
    /* We can't use key states for ordering, because the treeSets can't understand,
     * that old refined usage with zero key state is the same as new one
     */
    if (this.core.id != null && pO.core.id != null) {
      // Identifiers may not be equal here:
      // if (a.b > c.b)
      // FieldIdentifiers are the same (when we add to container),
      // but full identifiers (here) are not equal
      // TODO should we distinguish them?

    }
    return 0;
  }

  public UsageInfo copy() {
    return new UsageInfo(core, point, expandedStack);
  }

  public AbstractLockState getLockState() {
    return null;
  }

  public LockTreeNode getLockNode() {
    return point.get(LockTreeNode.class);
  }

  public UsagePoint getUsagePoint() {
    return point;
  }

  public UsageInfo expand(UsageDelta pDelta, List<AbstractState> pExpandedStack) {
    List<CompatibleNode> old = point.getCompatibleNodes();
    ImmutableList<CompatibleNode> newStates = pDelta.apply(old);
    if (newStates.isEmpty()) {
      return IRRELEVANT_USAGE;
    }
    if (newStates == old) {
      return this;
    }
    return new UsageInfo(core, new UsagePoint(newStates, core.accessType), pExpandedStack);
  }

  public List<AbstractState> getExpandedStack() {
    return expandedStack == null ? ImmutableList.of() : expandedStack;
  }
}
