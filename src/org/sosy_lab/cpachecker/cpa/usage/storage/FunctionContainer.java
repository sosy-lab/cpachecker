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

import de.uni_freiburg.informatik.ultimate.smtinterpol.util.IdentityHashSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.cpa.lock.LockState;
import org.sosy_lab.cpachecker.cpa.lock.LockState.LockStateBuilder;
import org.sosy_lab.cpachecker.cpa.lock.effects.LockEffect;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@SuppressFBWarnings(
  justification = "Serialization of container is useless and not supported",
  value = "SE_BAD_FIELD"
)
public class FunctionContainer extends AbstractUsageStorage {

  private static final long serialVersionUID = 1L;
  // private final Set<FunctionContainer> internalFunctionContainers;
  private final List<LockEffect> effects;
  private final StorageStatistics stats;

  private final Set<FunctionContainer> joinedWith;

  public static FunctionContainer createInitialContainer() {
    return new FunctionContainer(new StorageStatistics(), new ArrayList<>());
  }

  private FunctionContainer(StorageStatistics pStats, List<LockEffect> pEffects) {
    super();
    stats = pStats;
    stats.numberOfFunctionContainers.inc();
    effects = pEffects;
    joinedWith = new IdentityHashSet<>();
  }

  public FunctionContainer clone(List<LockEffect> pEffects) {
    return new FunctionContainer(this.stats, pEffects);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hashCode(effects);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj) ||
        getClass() != obj.getClass()) {
      return false;
    }
    FunctionContainer other = (FunctionContainer) obj;
    return Objects.equals(effects, other.effects);
  }

  public void join(FunctionContainer funcContainer) {
    stats.totalJoins.inc();
    if (joinedWith.contains(funcContainer)) {
      //We may join two different exit states to the same parent container
      stats.hitTimes.inc();
      return;
    }
    joinedWith.add(funcContainer);

    if (funcContainer.effects.isEmpty()) {
      stats.copyTimer.start();
      stats.emptyJoin.inc();
      funcContainer.forEach(this::addUsages);
      stats.copyTimer.stop();
    } else {
      Map<LockState, LockState> reduceToExpand = new HashMap<>();

      for (Map.Entry<SingleIdentifier, SortedSet<UsageInfo>> entry : funcContainer.entrySet()) {
        SingleIdentifier id = entry.getKey();
        SortedSet<UsageInfo> usages = entry.getValue();
        stats.totalUsages.setNextValue(usages.size());

        stats.effectTimer.start();
        stats.effectJoin.inc();
        SortedSet<UsageInfo> result = new TreeSet<>();
        LockState locks, expandedLocks;
        for (UsageInfo uinfo : usages) {
          locks = (LockState) uinfo.getLockState();
          if (reduceToExpand.containsKey(locks)) {
            expandedLocks = reduceToExpand.get(locks);
          } else {
            stats.expandedUsages.inc();
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
    private StatInt totalUsages = new StatInt(StatKind.SUM, "Number of expanding querries");
    private StatCounter expandedUsages = new StatCounter("Number of executed querries");
    private StatCounter emptyJoin = new StatCounter("Number of empty joins");
    private StatCounter effectJoin = new StatCounter("Number of effect joins");
    private StatCounter hitTimes = new StatCounter("Number of hits into cache");
    private StatCounter totalJoins = new StatCounter("Total number of joins");
    private StatCounter numberOfFunctionContainers = new StatCounter("Total number of function containers");

    private StatTimer effectTimer = new StatTimer("Time for appling effect");
    private StatTimer copyTimer = new StatTimer("Time for joining");

    public void printStatistics(StatisticsWriter out) {
      out.spacer()
         .put(effectTimer)
         .put(copyTimer)
         .put(totalJoins)
         .put(hitTimes)
         .put(emptyJoin)
         .put(effectJoin)
         .put(totalUsages)
         .put(expandedUsages)
         .put(numberOfFunctionContainers);
    }
  }
}
