// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.storage;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.NavigableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.lock.DeadLockState.DeadLockTreeNode;
import org.sosy_lab.cpachecker.cpa.lock.LockIdentifier;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo.Access;
import org.sosy_lab.cpachecker.util.Pair;

public class UnsafeDetector {

  public enum UnsafeMode {
    RACE,
    DEADLOCKCIRCULAR,
    DEADLOCKDISPATCH
  }

  private final UsageConfiguration config;

  public UnsafeDetector(UsageConfiguration pConfig) {
    config = pConfig;
  }

  public boolean isUnsafe(AbstractUsagePointSet set) {
    if (set instanceof RefinedUsagePointSet) {
      return true;
    }
    return isUnsafe((UnrefinedUsagePointSet) set);
  }

  public boolean isUnsafe(Set<UsageInfo> set) {
    return isUnsafe(preparePointSet(set));
  }

  private UnrefinedUsagePointSet preparePointSet(Set<UsageInfo> set) {
    UnrefinedUsagePointSet tmpSet = new UnrefinedUsagePointSet();
    set.forEach(tmpSet::add);
    return tmpSet;
  }

  private boolean isUnsafe(UnrefinedUsagePointSet set) {
    return isUnsafe(set.getTopUsages());
  }

  public Pair<UsageInfo, UsageInfo> getUnsafePair(AbstractUsagePointSet set) {
    assert isUnsafe(set);

    if (set instanceof RefinedUsagePointSet) {
      return ((RefinedUsagePointSet) set).getUnsafePair();
    } else {
      UnrefinedUsagePointSet unrefinedSet = (UnrefinedUsagePointSet) set;
      Pair<UsagePoint, UsagePoint> result = getUnsafePair(unrefinedSet.getTopUsages());

      assert result != null;

      return Pair.of(
          unrefinedSet.getUsageInfo(result.getFirst()).getOneExample(),
          unrefinedSet.getUsageInfo(result.getSecond()).getOneExample());
    }
  }

  public Pair<UsagePoint, UsagePoint> getUnsafePointPair(UnrefinedUsagePointSet set) {
    return getUnsafePair(set.getTopUsages());
  }

  private boolean isUnsafe(NavigableSet<UsagePoint> points) {
    for (UsagePoint point1 : points) {
      for (UsagePoint point2 : points.tailSet(point1)) {
        if (isUnsafePair(point1, point2)) {
          return true;
        }
      }
    }
    return false;
  }

  private Pair<UsagePoint, UsagePoint> getUnsafePair(NavigableSet<UsagePoint> set) {

    for (UsagePoint point1 : set) {
      for (UsagePoint point2 : set.tailSet(point1)) {
        if (point1.equals(point2)) {
          /* There can be an unsafe even with only one usage,
           * but at first we find two different usages
           */
          continue;
        }
        if (isUnsafePair(point1, point2)) {
          return Pair.of(point1, point2);
        }
      }
    }
    // Now we find an unsafe only from one usage
    if (!config.ignoreEmptyLockset()) {
      for (UsagePoint point : set) {
        if (isUnsafePair(point, point)) {
          return Pair.of(point, point);
        }
      }
    }
    // If we can not find an unsafe here, fail
    return null;
  }

  public boolean isUnsafePair(UsagePoint point1, UsagePoint point2) {
    if (point1.isCompatible(point2)) {
      switch (config.getUnsafeMode()) {
        case RACE:
          return isRace(point1, point2);

        case DEADLOCKDISPATCH:
          return isDeadlockDispatch(point1, point2);

        case DEADLOCKCIRCULAR:
          return isDeadlockCircular(point1, point2);

        default:
          throw new AssertionError("Unknown mode: " + config.getUnsafeMode());
      }
    }
    return false;
  }

  private boolean isRace(UsagePoint point1, UsagePoint point2) {
    if (point1.getAccess() == Access.WRITE || point2.getAccess() == Access.WRITE) {
      if (config.ignoreEmptyLockset() && point1.isEmpty() && point2.isEmpty()) {
        return false;
      }
      return true;
    }
    return false;
  }

  private boolean isDeadlockDispatch(UsagePoint point1, UsagePoint point2) {
    LockIdentifier intLock = LockIdentifier.of(checkNotNull(config.getIntLockName()));
    DeadLockTreeNode node1 = point1.get(DeadLockTreeNode.class);
    DeadLockTreeNode node2 = point2.get(DeadLockTreeNode.class);

    if (node2.contains(intLock) && !node1.contains(intLock)) {
      for (LockIdentifier lock1 : node1) {
        int index1 = node2.indexOf(lock1);
        int index2 = node2.indexOf(intLock);

        if (index1 > index2) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isDeadlockCircular(UsagePoint point1, UsagePoint point2) {
    // Deadlocks
    DeadLockTreeNode node1 = point1.get(DeadLockTreeNode.class);
    DeadLockTreeNode node2 = point2.get(DeadLockTreeNode.class);

    for (LockIdentifier lock1 : node1) {
      for (LockIdentifier lock2 : node2) {
        int index1 = node1.indexOf(lock1);
        int index2 = node1.indexOf(lock2);
        int otherIndex1 = node2.indexOf(lock1);
        int otherIndex2 = node2.indexOf(lock2);
        if (otherIndex1 >= 0
            && index2 >= 0
            && ((index1 > index2 && otherIndex1 < otherIndex2)
                || (index1 < index2 && otherIndex1 > otherIndex2))) {
          return true;
        }
      }
    }
    return false;
  }
}
