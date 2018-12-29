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
import com.google.common.collect.Sets;
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

public class UsageContainer {
  private final SortedMap<SingleIdentifier, UnrefinedUsagePointSet> unrefinedIds;
  private final SortedMap<SingleIdentifier, RefinedUsagePointSet> refinedIds;

  private final UnsafeDetector detector;

  private Set<SingleIdentifier> falseUnsafes;
  private Set<SingleIdentifier> initialUnsafes;

  // Only for statistics
  private int initialUsages = 0;

  private final LogManager logger;

  private final StatTimer resetTimer = new StatTimer("Time for reseting unsafes");
  private final StatTimer unsafeDetectionTimer = new StatTimer("Time for unsafe detection");

  private boolean usagesCalculated = false;
  private boolean oneTotalIteration = false;

  public UsageContainer(Configuration config, LogManager l) throws InvalidConfigurationException {
    unrefinedIds = new TreeMap<>();
    refinedIds = new TreeMap<>();
    falseUnsafes = new TreeSet<>();
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
        initialUnsafes = new TreeSet<>(unrefinedIds.keySet());
      } else {
        falseUnsafes = new TreeSet<>(initialUnsafes);
        falseUnsafes.removeAll(unrefinedIds.keySet());
        falseUnsafes.removeAll(refinedIds.keySet());
      }

      unsafeDetectionTimer.stop();
    }
  }

  public Set<SingleIdentifier> getFalseUnsafes() {
    return falseUnsafes;
  }

  public Iterator<SingleIdentifier> getUnsafeIterator() {
    calculateUnsafesIfNecessary();
    Set<SingleIdentifier> result = new TreeSet<>(refinedIds.keySet());
    result.addAll(unrefinedIds.keySet());
    return result.iterator();
  }

  public Iterator<SingleIdentifier> getUnrefinedUnsafeIterator() {
    // New set to avoid concurrent modification exception
    Set<SingleIdentifier> result = new TreeSet<>(unrefinedIds.keySet());
    return result.iterator();
  }

  public int getTotalUnsafeSize() {
    calculateUnsafesIfNecessary();
    return unrefinedIds.size() + refinedIds.size();
  }

  public int getProcessedUnsafeSize() {
    return refinedIds.size();
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
    } else {
      return refinedIds.get(id);
    }
  }

  public void setAsFalseUnsafe(SingleIdentifier id) {
    falseUnsafes.add(id);
    unrefinedIds.remove(id);
  }

  public void setAsRefined(SingleIdentifier id, RefinementResult result) {
    Preconditions.checkArgument(result.isTrue(), "Result is not true, can not set the set as refined");

    UsageInfo firstUsage = result.getTrueRace().getFirst();
    UsageInfo secondUsage = result.getTrueRace().getSecond();

    RefinedUsagePointSet rSet = RefinedUsagePointSet.create(firstUsage, secondUsage);
    refinedIds.put(id, rSet);
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

    int generalRefinedSize = 0;
    int generalFailedSize = 0;

    for (RefinedUsagePointSet uset : refinedIds.values()) {
      Pair<UsageInfo, UsageInfo> pair = uset.getUnsafePair();
      UsageInfo firstUsage = pair.getFirst();
      UsageInfo secondUsage = pair.getSecond();

      if (firstUsage.isLooped()) {
        failedUsages.inc();
        generalFailedSize++;
      }
      if (secondUsage.isLooped() && !firstUsage.equals(secondUsage)) {
        failedUsages.inc();
      }
      if (!firstUsage.isLooped() && !secondUsage.isLooped()) {
        generalRefinedSize++;
        refinedUsages.setNextValue(uset.size());
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

  public String getUnsafeStatus() {
    return unrefinedIds.size()
        + " unrefined, "
        + refinedIds.size()
        + " refined; "
        + falseUnsafes.size()
        + " false unsafes";
  }

  public Set<SingleIdentifier> getNotInterestingUnsafes() {
    return Sets.union(falseUnsafes, refinedIds.keySet());
  }
}
