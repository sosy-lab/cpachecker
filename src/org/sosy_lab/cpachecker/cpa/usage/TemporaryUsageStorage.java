/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.usage;

import com.google.common.collect.LinkedListMultimap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.lock.LockState;
import org.sosy_lab.cpachecker.cpa.lock.LockState.LockStateBuilder;
import org.sosy_lab.cpachecker.cpa.lock.effects.LockEffect;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;

public class TemporaryUsageStorage extends TreeMap<SingleIdentifier, SortedSet<UsageInfo>> {
  private static final long serialVersionUID = -8932709343923545136L;

  private Set<SingleIdentifier> deeplyCloned = new TreeSet<>();

  private LinkedListMultimap<SingleIdentifier, UsageInfo> withoutARGState;

  private final TemporaryUsageStorage previousStorage;

  public static int totalUsages = 0;
  public static int expandedUsages = 0;

  public TemporaryUsageStorage(TemporaryUsageStorage previous) {
    super(previous);
    //Copy states without ARG to set it later
    withoutARGState = LinkedListMultimap.create(previous.withoutARGState);
    previousStorage = previous;
  }

  public TemporaryUsageStorage() {
    withoutARGState = LinkedListMultimap.create();
    previousStorage = null;
  }

  public boolean add(SingleIdentifier id, UsageInfo info) {
    SortedSet<UsageInfo> storage = getStorageForId(id);
    if (info.getKeyState() == null) {
      withoutARGState.put(id, info);
    }
    return storage.add(info);
  }

  @Override
  public SortedSet<UsageInfo> put(SingleIdentifier id, SortedSet<UsageInfo> list) {
    deeplyCloned.add(id);
    return super.put(id, list);
  }

  public boolean addAll(SingleIdentifier id, SortedSet<UsageInfo> list) {
    SortedSet<UsageInfo> storage = getStorageForId(id);
    return storage.addAll(list);
  }

  private SortedSet<UsageInfo> getStorageForId(SingleIdentifier id) {
    if (deeplyCloned.contains(id)) {
      //List is already cloned
      assert this.containsKey(id);
      return this.get(id);
    } else {
      deeplyCloned.add(id);
      SortedSet<UsageInfo> storage;
      if (this.containsKey(id)) {
        //clone
        storage = new TreeSet<>(this.get(id));
      } else {
        storage = new TreeSet<>();
      }
      this.put(id, storage);
      return storage;
    }
  }

  public void setKeyState(ARGState state) {
    for (UsageInfo uinfo : withoutARGState.values()) {
      uinfo.setKeyState(state);
    }
    withoutARGState.clear();
  }

  @Override
  public void clear() {
    clearSets();
    TemporaryUsageStorage previous = previousStorage;
    //We cannot use recursion, due to large callstack and stack overflow exception
    while (previous != null) {
      previous.clearSets();
      previous = previous.previousStorage;
    }
  }

  private void clearSets() {
    super.clear();
    deeplyCloned.clear();
    withoutARGState.clear();
  }

  public static Timer effectTimer = new Timer();
  public static Timer copyTimer = new Timer();
  public static int emptyJoin = 0;
  public static int effectJoin = 0;
  public static int hitTimes = 0;
  public static int missTimes = 0;

  public void join(TemporaryUsageStorage pRecentUsages, List<LockEffect> effects) {

    if (effects.isEmpty()) {
      emptyJoin++;
    } else {
      effectJoin++;
    }

    Map<LockState, LockState> reduceToExpand = new HashMap<>();
    for (SingleIdentifier id : pRecentUsages.keySet()) {
      SortedSet<UsageInfo> otherStorage = pRecentUsages.get(id);
      totalUsages += otherStorage.size();
      if (effects.isEmpty()) {
        copyTimer.start();
        if (this.containsKey(id)) {
          SortedSet<UsageInfo> currentStorage = this.getStorageForId(id);
          currentStorage.addAll(otherStorage);
          hitTimes++;
        } else {
          //Not deeply cloned
          super.put(id, otherStorage);
          missTimes++;
        }
        copyTimer.stop();
      } else {
        effectTimer.start();
        LockState currentState;
        LockState expandedState;
        SortedSet<UsageInfo> result = new TreeSet<>();
        for (UsageInfo uinfo : otherStorage) {
          currentState = (LockState) uinfo.getState(LockState.class);
          if (reduceToExpand.containsKey(currentState)) {
            expandedState = reduceToExpand.get(currentState);
          } else {
            expandedUsages++;
            LockStateBuilder builder = currentState.builder();
            for (LockEffect effect : effects) {
              effect.effect(builder);
            }
            expandedState = builder.build();
            reduceToExpand.put(currentState, expandedState);
          }
          result.add(uinfo.expand(expandedState));
        }
        addAll(id, result);
        effectTimer.stop();
      }
    }
  }
}
