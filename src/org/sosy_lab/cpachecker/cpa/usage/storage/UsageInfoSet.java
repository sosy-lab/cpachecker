// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.storage;

import com.google.common.collect.ForwardingSet;
import com.google.common.collect.Iterables;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.cpa.usage.UsageState;

public class UsageInfoSet extends ForwardingSet<UsageInfo> {
  private final Set<UsageInfo> usageSet;

  public UsageInfoSet() {
    usageSet = new ConcurrentSkipListSet<>();
  }

  private UsageInfoSet(Set<UsageInfo> pSet) {
    usageSet = new ConcurrentSkipListSet<>(pSet);
  }

  public boolean remove(UsageState pUstate) {
    Iterator<UsageInfo> iterator = usageSet.iterator();
    boolean changed = false;
    while (iterator.hasNext()) {
      UsageInfo uinfo = iterator.next();
      AbstractState keyState = uinfo.getKeyState();
      assert (keyState != null);
      if (UsageState.get(keyState).equals(pUstate)) {
        iterator.remove();
        changed = true;
      }
    }
    return changed;
  }

  public UsageInfo getOneExample() {
    return Iterables.get(usageSet, 0);
  }

  public UsageInfoSet copy() {
    // For avoiding concurrent modification in refinement
    return new UsageInfoSet(usageSet);
  }

  @Override
  protected Set<UsageInfo> delegate() {
    return usageSet;
  }
}
