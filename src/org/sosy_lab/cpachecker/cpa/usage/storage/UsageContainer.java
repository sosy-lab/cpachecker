/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import com.google.common.base.Preconditions;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.cpa.usage.UsageState;
import org.sosy_lab.cpachecker.cpa.usage.refinement.RefinementResult;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.StructureIdentifier;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix="cpa.usage")
public class UsageContainer {
  private final SortedMap<SingleIdentifier, UnrefinedUsagePointSet> unrefinedIds;
  private final SortedMap<SingleIdentifier, RefinedUsagePointSet> refinedIds;
  private final SortedMap<SingleIdentifier, RefinedUsagePointSet> failedIds;

  private final UnsafeDetector detector;

  private final Set<SingleIdentifier> falseUnsafes;
  private final Set<SingleIdentifier> initialUnsafes;

  // Only for statistics
  private int initialUsages = 0;

  private final LogManager logger;

  private final StatTimer resetTimer = new StatTimer("Time for reseting unsafes");
  private final StatTimer unsafeDetectionTimer = new StatTimer("Time for unsafe detection");

  private boolean usagesCalculated = false;
  private boolean oneTotalIteration = false;

  @Option(description="output only true unsafes",
      secure = true)
  private boolean printOnlyTrueUnsafes = false;

  public UsageContainer(Configuration config, LogManager l) throws InvalidConfigurationException {
    config.inject(this);
    unrefinedIds = new TreeMap<>();
    refinedIds = new TreeMap<>();
    failedIds = new TreeMap<>();
    falseUnsafes = new TreeSet<>();
    initialUnsafes = new TreeSet<>();
    logger = l;
    detector = new UnsafeDetector(config);
  }

  public void add(SingleIdentifier pId, UsageInfo pUsage) {
    SingleIdentifier id = pId;
    if (id instanceof StructureIdentifier) {
      id = ((StructureIdentifier) id).toStructureFieldIdentifier();
    }
    if (oneTotalIteration && !unrefinedIds.containsKey(id)) {
      return;
    }

    assert (!falseUnsafes.contains(id) || !refinedIds.containsKey(id));
    UnrefinedUsagePointSet uset;

    if (!unrefinedIds.containsKey(id)) {
      uset = new UnrefinedUsagePointSet();
      unrefinedIds.put(id, uset);
    } else {
      uset = unrefinedIds.get(id);
    }

    uset.add(pUsage);
  }

  private void calculateUnsafesIfNecessary() {
    if (!usagesCalculated) {
      unsafeDetectionTimer.start();
      usagesCalculated = true;
      Set<SingleIdentifier> toDelete = new HashSet<>();

      for (Entry<SingleIdentifier, UnrefinedUsagePointSet> entry : unrefinedIds.entrySet()) {
        UnrefinedUsagePointSet tmpList = entry.getValue();
        if (detector.isUnsafe(tmpList)) {
          if (!oneTotalIteration) {
            initialUsages += tmpList.size();
          }
        } else {
          toDelete.add(entry.getKey());
        }
      }
      toDelete.forEach(unrefinedIds::remove);

      if (!oneTotalIteration) {
        initialUnsafes.addAll(unrefinedIds.keySet());
      } else {
        Set<SingleIdentifier> newFalseIds = new TreeSet<>(initialUnsafes);
        newFalseIds.removeAll(falseUnsafes);
        newFalseIds.removeAll(unrefinedIds.keySet());
        newFalseIds.removeAll(refinedIds.keySet());
        newFalseIds.removeAll(failedIds.keySet());
        falseUnsafes.addAll(newFalseIds);
      }

      unsafeDetectionTimer.stop();
    }
  }

  public Set<SingleIdentifier> getFalseUnsafes() {
    return falseUnsafes;
  }

  public Iterator<SingleIdentifier> getUnsafeIterator() {
    Set<SingleIdentifier> result = new TreeSet<>(refinedIds.keySet());
    result.addAll(failedIds.keySet());
    if (!printOnlyTrueUnsafes) {
      calculateUnsafesIfNecessary();
      result.addAll(unrefinedIds.keySet());
    }
    return result.iterator();
  }

  public Iterator<SingleIdentifier> getUnrefinedUnsafeIterator() {
    // New set to avoid concurrent modification exception
    Set<SingleIdentifier> result = new TreeSet<>(unrefinedIds.keySet());
    return result.iterator();
  }

  public int getUnsafeSize() {
    calculateUnsafesIfNecessary();
    if (printOnlyTrueUnsafes) {
      return getProcessedUnsafeSize();
    } else {
      return getTotalUnsafeSize();
    }
  }

  public int getTotalUnsafeSize() {
    calculateUnsafesIfNecessary();
    return unrefinedIds.size() + refinedIds.size() + failedIds.size();
  }

  public int getProcessedUnsafeSize() {
    return refinedIds.size() + failedIds.size();
  }

  public UnsafeDetector getUnsafeDetector() {
    return detector;
  }

  public void resetUnrefinedUnsafes() {
    resetTimer.start();
    usagesCalculated = false;
    oneTotalIteration = true;
    unrefinedIds.forEach((k, v) -> v.reset());
    logger.log(Level.FINE, "Unsafes are reseted");
    resetTimer.stop();
  }

  public void removeState(final UsageState pUstate) {
    unrefinedIds.forEach((id, uset) -> uset.remove(pUstate));
    logger.log(Level.ALL, "All unsafes related to key state " + pUstate + " were removed from reached set");
  }

  public AbstractUsagePointSet getUsages(SingleIdentifier id) {
    if (unrefinedIds.containsKey(id)) {
      return unrefinedIds.get(id);
    } else if (refinedIds.containsKey(id)){
      return refinedIds.get(id);
    } else {
      return failedIds.get(id);
    }
  }

  public void setAsFalseUnsafe(SingleIdentifier id) {
    falseUnsafes.add(id);
    unrefinedIds.remove(id);
  }

  public void setAsRefined(SingleIdentifier id, RefinementResult result) {
    Preconditions.checkArgument(result.isTrue(), "Result is not true, can not set the set as refined");
    Preconditions.checkArgument(detector.isUnsafe(getUsages(id)), "Refinement is successful, but the unsafe is absent for identifier " + id);

    UsageInfo firstUsage = result.getTrueRace().getFirst();
    UsageInfo secondUsage = result.getTrueRace().getSecond();

    RefinedUsagePointSet rSet = RefinedUsagePointSet.create(firstUsage, secondUsage);
    if (firstUsage.isLooped() || secondUsage.isLooped()) {
      failedIds.put(id, rSet);
    } else {
      refinedIds.put(id, rSet);
    }
    unrefinedIds.remove(id);
  }

  public void printUsagesStatistics(StatisticsWriter out) {
    int unsafeSize = getTotalUnsafeSize();
    StatInt topUsagePoints = new StatInt(StatKind.SUM, "Total amount of unrefined usage points");
    StatInt unrefinedUsages = new StatInt(StatKind.SUM, "Total amount of unrefined usages");
    StatInt refinedUsages = new StatInt(StatKind.SUM, "Total amount of refined usages");
    StatCounter failedUsages = new StatCounter("Total amount of failed usages");

    final int generalUnrefinedSize = unrefinedIds.keySet().size();
    for (UnrefinedUsagePointSet uset : unrefinedIds.values()) {
      unrefinedUsages.setNextValue(uset.size());
      topUsagePoints.setNextValue(uset.getNumberOfTopUsagePoints());
    }

    final int generalRefinedSize = refinedIds.keySet().size();
    refinedIds.forEach(
        (id, rset) -> refinedUsages.setNextValue(rset.size()));

    final int generalFailedSize = failedIds.keySet().size();
    for (RefinedUsagePointSet uset : failedIds.values()) {
      Pair<UsageInfo, UsageInfo> pair = uset.getUnsafePair();
      if (pair.getFirst().isLooped()) {
        failedUsages.inc();
      }
      if (pair.getSecond().isLooped() && !pair.getFirst().equals(pair.getSecond())) {
        failedUsages.inc();
      }
    }

    out.spacer()
        .put("Total amount of unsafes", unsafeSize)
        .put("Initial amount of unsafes (before refinement)", unsafeSize + falseUnsafes.size())
        .put("Initial amount of usages (before refinement)", initialUsages)
        .put("Initial amount of refined false unsafes", falseUnsafes.size())
        .put("Total amount of unrefined unsafes", generalUnrefinedSize)
        .put(topUsagePoints)
        .put(unrefinedUsages)
        .put("Total amount of refined unsafes", generalRefinedSize)
        .put(refinedUsages)
        .put("Total amount of failed unsafes", generalFailedSize)
        .put(failedUsages)
        .put(resetTimer)
        .put(unsafeDetectionTimer);
  }
}
