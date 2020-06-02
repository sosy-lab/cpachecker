// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.storage;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;

public abstract class AbstractUsageStorage extends TreeMap<SingleIdentifier, SortedSet<UsageInfo>> {

  private static final long serialVersionUID = 1L;
  private final Set<SingleIdentifier> deeplyCloned = new TreeSet<>();

  protected AbstractUsageStorage(AbstractUsageStorage previous) {
    super(previous);
  }

  protected AbstractUsageStorage(){}

  protected SortedSet<UsageInfo> getStorageForId(SingleIdentifier id) {
    if (deeplyCloned.contains(id)) {
      //List is already cloned
      assert containsKey(id);
      return get(id);
    } else {
      deeplyCloned.add(id);
      SortedSet<UsageInfo> storage;
      if (containsKey(id)) {
        //clone
        storage = new TreeSet<>(this.get(id));
      } else {
        storage = new TreeSet<>();
      }
      super.put(id, storage);
      return storage;
    }
  }

  @Override
  public void clear() {
    super.clear();
    deeplyCloned.clear();
  }

  public void copyUsagesFrom(AbstractUsageStorage pStorage) {
    pStorage.forEach(this::addUsages);
  }

  public void addUsages(SingleIdentifier id, SortedSet<UsageInfo> usages) {
    if (containsKey(id)) {
      SortedSet<UsageInfo> currentStorage = getStorageForId(id);
      currentStorage.addAll(usages);
    } else {
      super.put(id, usages);
    }
  }

  public boolean add(SingleIdentifier id, UsageInfo info) {
    SortedSet<UsageInfo> currentStorage = getStorageForId(id);
    return currentStorage.add(info);
  }

  public boolean isSubsetOf(AbstractUsageStorage pOther) {
    for (Map.Entry<SingleIdentifier, SortedSet<UsageInfo>> entry : this.entrySet()) {
      SingleIdentifier id = entry.getKey();
      if (pOther.containsKey(id)) {
        SortedSet<UsageInfo> currentSet = entry.getValue();
        SortedSet<UsageInfo> otherSet = pOther.get(id);
        if (!otherSet.containsAll(currentSet)) {
          return false;
        }
      } else {
        return false;
      }
    }
    return true;
  }
}
