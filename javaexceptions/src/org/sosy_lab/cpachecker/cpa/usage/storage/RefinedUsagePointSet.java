// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.storage;

import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.util.Pair;

public class RefinedUsagePointSet implements AbstractUsagePointSet {

  public static class DoubleRefinedUsagePointSet extends RefinedUsagePointSet {
    protected final UsageInfo target2;

    private DoubleRefinedUsagePointSet(UsageInfo newSet, UsageInfo newSet2) {
      super(newSet);
      target2 = newSet2;
    }

    @Override
    public int size() {
      return 2;
    }

    @Override
    public Pair<UsageInfo, UsageInfo> getUnsafePair() {
      return Pair.of(target, target2);
    }
  }

  protected final UsageInfo target;

  private RefinedUsagePointSet(UsageInfo newSet) {
    target = newSet;
  }

  public static RefinedUsagePointSet create(UsageInfo newSet, UsageInfo newSet2) {
    // We may clone it, so just == can not help
    if (newSet.getPath().equals(newSet2.getPath()) && newSet.equals(newSet2)) {
      return new RefinedUsagePointSet(newSet);
    } else {
      return new DoubleRefinedUsagePointSet(newSet, newSet2);
    }
  }

  @Override
  public int size() {
    return 1;
  }

  public Pair<UsageInfo, UsageInfo> getUnsafePair() {
    return Pair.of(target, target);
  }
}
