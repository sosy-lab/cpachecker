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

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cpa.lock.LockState;
import org.sosy_lab.cpachecker.cpa.lock.LockState.LockStateBuilder;
import org.sosy_lab.cpachecker.cpa.lock.effects.LockEffect;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;

public class FunctionContainer extends AbstractUsageStorage {

  private static final long serialVersionUID = 1L;
  //private final Set<FunctionContainer> internalFunctionContainers;
  private final List<LockEffect> effects;
  private final StorageStatistics stats;

  private final Set<FunctionContainer> joinedWith;

  public static FunctionContainer createInitialContainer() {
    return new FunctionContainer(new StorageStatistics(), new LinkedList<>() );
  }

  private FunctionContainer(StorageStatistics pStats, List<LockEffect> pEffects) {
    super();
    stats = pStats;
    effects = pEffects;
    joinedWith = new HashSet<>();
  }

  public FunctionContainer clone(List<LockEffect> effects) {
    return new FunctionContainer(this.stats, effects);
  }

  public void join(FunctionContainer funcContainer) {
    if (joinedWith.contains(funcContainer)) {
      //We may join two different exit states to the same parent container
      stats.hitTimes++;
      return;
    }
    stats.missTimes++;
    joinedWith.add(funcContainer);

    if (funcContainer.effects.isEmpty()) {
      stats.copyTimer.start();
      stats.effectJoin++;
      funcContainer.forEach((id, set) -> this.addUsages(id, set));
      stats.copyTimer.stop();
    } else {
      Map<LockState, LockState> reduceToExpand = new HashMap<>();

      for (Map.Entry<SingleIdentifier, SortedSet<UsageInfo>> entry : funcContainer.entrySet()) {
        SingleIdentifier id = entry.getKey();
        SortedSet<UsageInfo> usages = entry.getValue();
        stats.totalUsages += usages.size();

        stats.effectTimer.start();
        stats.effectJoin++;
        SortedSet<UsageInfo> result = new TreeSet<>();
        LockState locks, expandedLocks;
        for (UsageInfo uinfo : usages) {
          locks = (LockState) uinfo.getState(LockState.class);
          if (reduceToExpand.containsKey(locks)) {
            expandedLocks = reduceToExpand.get(locks);
          } else {
            stats.expandedUsages++;
            LockStateBuilder builder = locks.builder();
            funcContainer.effects.forEach(e -> e.effect(builder));
            expandedLocks = builder.build();
            reduceToExpand.put(locks, expandedLocks);
          }
          result.add(uinfo.expand(expandedLocks));
        }
        addUsages(id, result);
        stats.effectTimer.stop();
      }
    }
  }

  public void join(TemporaryUsageStorage pRecentUsages) {
    stats.copyTimer.start();
    pRecentUsages.forEach((id, set) -> this.addUsages(id, set));
    stats.copyTimer.stop();
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
    private int numberOfFunctionContainers = 0;

    Timer effectTimer = new Timer();
    Timer copyTimer = new Timer();
    Timer exportTimer = new Timer();

    public void printStatistics(PrintStream out) {

      out.println("Time for effect:                    " + effectTimer);
      out.println("Time for copy:                      " + copyTimer);
      out.println("Time for exporting unsafes:         " + exportTimer);
      out.println("Number of empty joins:              " + emptyJoin);
      out.println("Number of effect joins:             " + effectJoin);
      out.println("Number of hit joins:                " + hitTimes);
      out.println("Number of miss joins:               " + missTimes);
      out.println("Number of expanding querries:       " + totalUsages);
      out.println("Number of executed querries:        " + expandedUsages);
      out.println("Total number of function containers:" + numberOfFunctionContainers);
    }
  }
}
