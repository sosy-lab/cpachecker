/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.usage.storage;

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
}
