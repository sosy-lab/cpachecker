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
package org.sosy_lab.cpachecker.cpa.usage;

import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cpa.lock.LockState;
import org.sosy_lab.cpachecker.cpa.lock.LockState.LockStateBuilder;
import org.sosy_lab.cpachecker.cpa.lock.effects.LockEffect;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;

public class FunctionContainer extends TreeMap<SingleIdentifier, SortedSet<UsageInfo>> {

  private static final long serialVersionUID = 1L;
  private final List<FunctionContainer> internalFunctionContainers;
  private final List<LockEffect> effects;
  private final StorageStatistics stats;

  private final Set<SingleIdentifier> deeplyCloned = new TreeSet<>();

  public static FunctionContainer createInitialContainer() {
    return new FunctionContainer(new StorageStatistics(), new LinkedList<>() );
  }

  private FunctionContainer(StorageStatistics pStats, List<LockEffect> pEffects) {
    super();
    stats = pStats;
    internalFunctionContainers = new LinkedList<>();
    effects = pEffects;
  }

  public FunctionContainer clone(List<LockEffect> effects) {
    return new FunctionContainer(this.stats, effects);
  }

  public void join(FunctionContainer funcContainer) {
    internalFunctionContainers.add(funcContainer);
  }

  public void addUsages(SingleIdentifier id, SortedSet<UsageInfo> usages) {
    SortedSet<UsageInfo> currentStorage = getStorageForId(id);
    currentStorage.addAll(usages);
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
      super.put(id, storage);
      return storage;
    }
  }

  public void join(TemporaryUsageStorage pRecentUsages) {
    pRecentUsages.forEach((id, set) -> addUsages(id, set));
  }

  public StorageStatistics getStatistics() {
    return stats;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result
        + ((effects == null) ? 0 : effects.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    FunctionContainer other = (FunctionContainer) obj;
    if (effects == null) {
      if (other.effects != null) {
        return false;
      }
    } else if (!effects.equals(other.effects)) {
      return false;
    }
    return true;
  }

  /**
   * The method goes through the internal containers and applies all effects to the stored usages
   */
  public void exportUsages() {
    Queue<Pair<FunctionContainer, List<LockEffect>>> waitList = new LinkedList<>();
    Map<FunctionContainer, List<LockEffect>> handledContainers = new IdentityHashMap<>();
    List<LockEffect> currentEffects = Collections.emptyList();
    FunctionContainer currentContainer;

    stats.exportTimer.start();
    addToWaitlist(waitList, this, currentEffects);

    while (!waitList.isEmpty()) {
      Pair<FunctionContainer, List<LockEffect>> currentPair = waitList.poll();
      currentContainer = currentPair.getFirst();
      currentEffects = currentPair.getSecond();

      if (handledContainers.containsKey(currentContainer) &&
          handledContainers.get(currentContainer).equals(currentEffects)) {
        continue;
      }

      //Add all internal containers
      addToWaitlist(waitList, currentContainer, currentEffects);

      //Quick cache. Do not move it up!
      //Then it should consider Effects
      //TODO does it helps to organize more complicated cache?
      Map<LockState, LockState> reduceToExpand = new HashMap<>();

      for (Map.Entry<SingleIdentifier, SortedSet<UsageInfo>> entry : currentContainer.entrySet()) {
        SingleIdentifier id = entry.getKey();
        SortedSet<UsageInfo> usages = entry.getValue();
        stats.totalUsages += usages.size();

        if (currentEffects.isEmpty()) {
          stats.emptyJoin++;
          addUsages(id, usages);
        } else {
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
              currentEffects.forEach(e -> e.effect(builder));
              expandedLocks = builder.build();
              reduceToExpand.put(locks, expandedLocks);
            }
            result.add(uinfo.expand(expandedLocks));
          }
          addUsages(id, result);
          stats.effectTimer.stop();
        }
      }

      handledContainers.put(currentContainer, currentEffects);
    }
    stats.exportTimer.stop();
    //stats.numberOfFunctionContainers = handledContainers.size();
  }

  private void addToWaitlist(Queue<Pair<FunctionContainer, List<LockEffect>>> waitList,
      FunctionContainer currentContainer, List<LockEffect> currentEffects) {

    for (FunctionContainer entry : currentContainer.internalFunctionContainers) {
      LinkedList<LockEffect> completeEffects = new LinkedList<>(currentEffects);
      completeEffects.addAll(entry.effects);
      waitList.add(Pair.of(entry, completeEffects));
    }
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
