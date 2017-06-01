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
import java.io.PrintStream;
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

  private final StorageStatistics stats;

  private TemporaryUsageStorage(TemporaryUsageStorage previous) {
    super(previous);
    //Copy states without ARG to set it later
    withoutARGState = LinkedListMultimap.create(previous.withoutARGState);
    previousStorage = previous;
    stats = previous.stats;
  }

  private TemporaryUsageStorage(StorageStatistics pStats) {
    withoutARGState = LinkedListMultimap.create();
    previousStorage = null;
    stats = pStats;
  }

  public TemporaryUsageStorage() {
    this(new StorageStatistics());
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

  @Override
  public TemporaryUsageStorage clone() {
    return new TemporaryUsageStorage(this);
  }

  public TemporaryUsageStorage cloneOnlyStats() {
    return new TemporaryUsageStorage(this.stats);
  }

  private void clearSets() {
    super.clear();
    deeplyCloned.clear();
    withoutARGState.clear();
  }

  public void join(TemporaryUsageStorage pRecentUsages, List<LockEffect> effects) {

    Map<LockState, LockState> reduceToExpand = new HashMap<>();
    for (SingleIdentifier id : pRecentUsages.keySet()) {
      SortedSet<UsageInfo> otherStorage = pRecentUsages.get(id);
      stats.totalUsages += otherStorage.size();
      if (effects.isEmpty()) {
        stats.copyTimer.start();
        stats.emptyJoin++;
        if (this.containsKey(id)) {
          SortedSet<UsageInfo> currentStorage = this.getStorageForId(id);
          currentStorage.addAll(otherStorage);
          stats.hitTimes++;
        } else {
          //Not deeply cloned
          super.put(id, otherStorage);
          stats.missTimes++;
        }
        stats.copyTimer.stop();
      } else {
        stats.effectTimer.start();
        stats.effectJoin++;
        LockState currentState;
        LockState expandedState;
        SortedSet<UsageInfo> result = new TreeSet<>();
        for (UsageInfo uinfo : otherStorage) {
          currentState = (LockState) uinfo.getState(LockState.class);
          if (reduceToExpand.containsKey(currentState)) {
            expandedState = reduceToExpand.get(currentState);
          } else {
            stats.expandedUsages++;
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
        stats.effectTimer.stop();
      }
    }
  }

  public StorageStatistics getStatistics() {
    return stats;
  }

  public static class StorageStatistics {
    private int totalUsages = 0;
    private int expandedUsages = 0;
    private int emptyJoin = 0;
    private int effectJoin = 0;
    private int hitTimes = 0;
    private int missTimes = 0;

    private Timer effectTimer = new Timer();
    private Timer copyTimer = new Timer();

    public void printStatistics(PrintStream out) {

      out.println("Time for effect:                    " + effectTimer);
      out.println("Time for copy:                      " + copyTimer);
      out.println("Number of empty joins:              " + emptyJoin);
      out.println("Number of effect joins:             " + effectJoin);
      out.println("Number of hit joins:                " + hitTimes);
      out.println("Number of miss joins:               " + missTimes);
      out.println("Number of expanding querries:       " + totalUsages);
      out.println("Number of executed querries:        " + expandedUsages);
    }
  }

}
