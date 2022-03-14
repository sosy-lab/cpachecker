// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.storage;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import de.uni_freiburg.informatik.ultimate.smtinterpol.util.IdentityHashSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.lock.effects.LockEffect;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@SuppressFBWarnings(
    justification = "Serialization of container is useless and not supported",
    value = "SE_BAD_FIELD")
public class FunctionContainer extends AbstractUsageStorage {

  private static final long serialVersionUID = 1L;
  // private final Set<FunctionContainer> internalFunctionContainers;
  private final Multiset<LockEffect> effects;
  private final StorageStatistics stats;

  private final Set<FunctionContainer> joinedWith;

  private final Set<TemporaryUsageStorage> storages;

  public static FunctionContainer createInitialContainer() {
    return new FunctionContainer(new StorageStatistics(), HashMultiset.create());
  }

  private FunctionContainer(StorageStatistics pStats, Multiset<LockEffect> pEffects) {
    stats = pStats;
    stats.numberOfFunctionContainers.inc();
    effects = pEffects;
    joinedWith = new IdentityHashSet<>();
    storages = new HashSet<>();
  }

  public FunctionContainer clone(Multiset<LockEffect> pEffects) {
    return new FunctionContainer(stats, pEffects);
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
    return this == obj;
  }

  public void join(FunctionContainer funcContainer) {
    stats.totalJoins.inc();
    if (joinedWith.contains(funcContainer)) {
      // We may join two different exit states to the same parent container
      stats.hitTimes.inc();
      return;
    }
    if (!funcContainer.isEmpty() || !funcContainer.joinedWith.isEmpty()) {
      joinedWith.add(funcContainer);
    }
  }

  public void join(TemporaryUsageStorage pRecentUsages) {
    stats.copyTimer.start();
    copyUsagesFrom(pRecentUsages);
    stats.copyTimer.stop();
  }

  public void clearStorages() {
    storages.forEach(TemporaryUsageStorage::clear);
    storages.clear();
  }

  public void registerTemporaryContainer(TemporaryUsageStorage storage) {
    storages.add(storage);
  }

  public StorageStatistics getStatistics() {
    return stats;
  }

  public Set<FunctionContainer> getContainers() {
    return joinedWith;
  }

  public Multiset<LockEffect> getLockEffects() {
    return effects;
  }

  public static class StorageStatistics {
    private StatCounter hitTimes = new StatCounter("Number of hits into cache");
    private StatCounter totalJoins = new StatCounter("Total number of joins");
    private StatCounter numberOfFunctionContainers =
        new StatCounter("Total number of function containers");

    private StatTimer copyTimer = new StatTimer("Time for coping usages");

    public void printStatistics(StatisticsWriter out) {
      out.spacer().put(copyTimer).put(totalJoins).put(hitTimes).put(numberOfFunctionContainers);
    }
  }
}
