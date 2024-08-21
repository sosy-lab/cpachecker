// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.storage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.cpa.usage.UsageState;

public class UnrefinedUsagePointSet implements AbstractUsagePointSet {
  private final NavigableSet<UsagePoint> topUsages;
  private final Map<UsagePoint, UsageInfoSet> usageInfoSets;

  public UnrefinedUsagePointSet() {
    topUsages = new TreeSet<>();
    usageInfoSets = new HashMap<>();
  }

  public void add(UsageInfo newInfo) {
    UsageInfoSet targetSet;
    UsagePoint newPoint = newInfo.createUsagePoint();
    if (usageInfoSets.containsKey(newPoint)) {
      targetSet = usageInfoSets.get(newPoint);
    } else {
      targetSet = new UsageInfoSet();
      usageInfoSets.put(newPoint, targetSet);
    }
    add(newPoint);
    targetSet.add(newInfo);
  }

  private void add(UsagePoint newPoint) {
    if (!topUsages.contains(newPoint)) {
      // Put newPoint in the right place in tree
      Iterator<UsagePoint> iterator = topUsages.iterator();
      while (iterator.hasNext()) {
        UsagePoint point = iterator.next();
        if (newPoint.covers(point)) {
          iterator.remove();
          newPoint.addCoveredUsage(point);
        } else if (point.covers(newPoint)) {
          point.addCoveredUsage(newPoint);
          return;
        }
      }
      topUsages.add(newPoint);
    }
  }

  public UsageInfoSet getUsageInfo(UsagePoint point) {
    return usageInfoSets.get(point);
  }

  @Override
  public int size() {
    int result = 0;

    for (UsageInfoSet value : usageInfoSets.values()) {
      result += value.size();
    }

    return result;
  }

  public void reset() {
    topUsages.clear();
    usageInfoSets.clear();
  }

  public void remove(UsageState pUstate) {
    // Attention! Use carefully. May not work
    for (UsagePoint point : new TreeSet<>(usageInfoSets.keySet())) {
      UsageInfoSet uset = usageInfoSets.get(point);
      boolean b = uset.remove(pUstate);
      if (b) {
        if (uset.isEmpty()) {
          usageInfoSets.remove(point);
        }
        // May be two usages related to the same state. This is abstractState !
        // return;
      }
    }
  }

  public Iterator<UsagePoint> getPointIterator() {
    return new TreeSet<>(topUsages).iterator();
  }

  public Iterator<UsagePoint> getPointIteratorFrom(UsagePoint p) {
    return new TreeSet<>(topUsages.tailSet(p)).iterator();
  }

  public int getNumberOfTopUsagePoints() {
    return topUsages.size();
  }

  public void remove(UsagePoint currentUsagePoint) {
    usageInfoSets.remove(currentUsagePoint);
    topUsages.remove(currentUsagePoint);
    currentUsagePoint.getCoveredUsages().forEach(this::add);
  }

  NavigableSet<UsagePoint> getTopUsages() {
    return topUsages;
  }
}
