// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.storage;

import com.google.common.collect.Iterables;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.cpa.usage.UsageState;

@SuppressWarnings("checkstyle:IllegalType") // TODO: use composition instead of inheritance
public class UsageInfoSet extends TreeSet<UsageInfo> {

  private static final long serialVersionUID = -5057827815596702715L;

  public UsageInfoSet() {}

  private UsageInfoSet(NavigableSet<UsageInfo> pSet) {
    super(pSet);
  }

  public boolean remove(UsageState pUstate) {
    Iterator<UsageInfo> iterator = iterator();
    boolean changed = false;
    while (iterator.hasNext()) {
      UsageInfo uinfo = iterator.next();
      AbstractState keyState = uinfo.getKeyState();
      assert keyState != null;
      if (UsageState.get(keyState).equals(pUstate)) {
        iterator.remove();
        changed = true;
      }
    }
    return changed;
  }

  public UsageInfo getOneExample() {
    return Iterables.get(this, 0);
  }

  public UsageInfoSet copy() {
    // For avoiding concurrent modification in refinement
    return new UsageInfoSet(this);
  }
}
