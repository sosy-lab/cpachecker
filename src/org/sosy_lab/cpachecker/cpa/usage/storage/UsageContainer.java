// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.storage;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.lock.LockState;
import org.sosy_lab.cpachecker.cpa.lock.LockState.LockStateBuilder;
import org.sosy_lab.cpachecker.cpa.lock.effects.LockEffect;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.cpa.usage.UsageState;
import org.sosy_lab.cpachecker.cpa.usage.refinement.RefinementResult;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class UsageContainer {
  private final NavigableMap<SingleIdentifier, UnrefinedUsagePointSet> unrefinedIds;
  private final NavigableMap<SingleIdentifier, RefinedUsagePointSet> refinedIds;
  private final NavigableMap<SingleIdentifier, RefinedUsagePointSet> failedIds;

  private final UnsafeDetector detector;

  private final Set<SingleIdentifier> falseUnsafes;

  private final Set<SingleIdentifier> processedUnsafes = new HashSet<>();
  // Only for statistics
  private Set<SingleIdentifier> initialSet = null;
  private int initialUsages;

  private final LogManager logger;
  private final UsageConfiguration config;

  private final StatTimer resetTimer = new StatTimer("Time for reseting unsafes");
  private final StatTimer copyTimer = new StatTimer("Time for filling global container");
  private final StatTimer emptyEffectsTimer = new StatTimer("Time for coping usages");

  int unsafeUsages = -1;
  int totalIds = 0;

  public UsageContainer(UsageConfiguration config, LogManager l, UnsafeDetector unsafeDetector) {
    this(
        new TreeMap<SingleIdentifier, UnrefinedUsagePointSet>(),
        new TreeMap<SingleIdentifier, RefinedUsagePointSet>(),
        new TreeMap<SingleIdentifier, RefinedUsagePointSet>(),
        new TreeSet<SingleIdentifier>(),
        l,
        config,
        unsafeDetector);
  }

  private UsageContainer(
      NavigableMap<SingleIdentifier, UnrefinedUsagePointSet> pUnrefinedStat,
      NavigableMap<SingleIdentifier, RefinedUsagePointSet> pRefinedStat,
      NavigableMap<SingleIdentifier, RefinedUsagePointSet> failedStat,
      Set<SingleIdentifier> pFalseUnsafes,
      LogManager pLogger,
      UsageConfiguration pConfig,
      UnsafeDetector pDetector) {
    unrefinedIds = pUnrefinedStat;
    refinedIds = pRefinedStat;
    failedIds = failedStat;
    falseUnsafes = pFalseUnsafes;
    logger = pLogger;
    config = pConfig;
    detector = pDetector;
  }

  public void initContainerIfNecessary(FunctionContainer storage) {
    if (unsafeUsages == -1) {
      copyTimer.start();
      Set<Pair<FunctionContainer, Multiset<LockEffect>>> processedContainers = new HashSet<>();
      Deque<Pair<FunctionContainer, Multiset<LockEffect>>> waitlist = new ArrayDeque<>();
      Pair<FunctionContainer, Multiset<LockEffect>> first = Pair.of(storage, HashMultiset.create());
      waitlist.add(first);

      while (!waitlist.isEmpty()) {
        Pair<FunctionContainer, Multiset<LockEffect>> currentPair = waitlist.pollFirst();

        if (!processedContainers.contains(currentPair)) {
          FunctionContainer currentContainer = currentPair.getFirst();
          Multiset<LockEffect> currentEffects = currentPair.getSecond();

          Multiset<LockEffect> newEffects = HashMultiset.create();
          newEffects.addAll(currentEffects);
          newEffects.addAll(currentContainer.getLockEffects());

          copyUsages(currentContainer, newEffects);
          processedContainers.add(currentPair);

          if (newEffects.equals(currentEffects)) {
            newEffects = currentEffects;
          }
          for (FunctionContainer container : currentContainer.getContainers()) {
            waitlist.add(Pair.of(container, newEffects));
          }
        }
      }

      calculateUnsafesIfNecessary();
      copyTimer.stop();
    }
  }

  public void forceAddNewUsages(TemporaryUsageStorage storage) {
    // This is a case of 'abort'-functions
    assert unsafeUsages == -1;
    copyUsages(storage);
  }

  private void copyUsages(AbstractUsageStorage storage) {
    emptyEffectsTimer.start();
    for (Entry<SingleIdentifier, NavigableSet<UsageInfo>> entry : storage.entrySet()) {
      SingleIdentifier id = entry.getKey();

      if (falseUnsafes.contains(id) || refinedIds.containsKey(id)) {
        continue;
      }
      UnrefinedUsagePointSet uset = getSet(id);

      for (UsageInfo uinfo : entry.getValue()) {
        if (uinfo.getKeyState() != null) {
          uset.add(uinfo);
        }
      }
    }
    emptyEffectsTimer.stop();
  }

  private void copyUsages(FunctionContainer storage, Multiset<LockEffect> currentEffects) {
    if (currentEffects.isEmpty()) {
      copyUsages(storage);
    } else {
      Map<LockState, LockState> reduceToExpand = new HashMap<>();

      for (Map.Entry<SingleIdentifier, NavigableSet<UsageInfo>> entry : storage.entrySet()) {
        SingleIdentifier id = entry.getKey();

        if (falseUnsafes.contains(id) || refinedIds.containsKey(id)) {
          continue;
        }
        UnrefinedUsagePointSet uset = getSet(id);

        LockState locks, expandedLocks;
        for (UsageInfo uinfo : entry.getValue()) {
          if (uinfo.getKeyState() == null) {
            // TODO what should we do?
            continue;
          }
          locks = (LockState) uinfo.getLockState();
          if (reduceToExpand.containsKey(locks)) {
            expandedLocks = reduceToExpand.get(locks);
          } else {
            LockStateBuilder builder = locks.builder();
            currentEffects.forEach(e -> e.effect(builder));
            expandedLocks = builder.build();
            reduceToExpand.put(locks, expandedLocks);
          }
          uset.add(uinfo.expand(expandedLocks));
        }
      }
    }
  }

  private UnrefinedUsagePointSet getSet(SingleIdentifier id) {
    assert (!falseUnsafes.contains(id) || !refinedIds.containsKey(id));

    UnrefinedUsagePointSet uset;
    if (!unrefinedIds.containsKey(id)) {
      uset = new UnrefinedUsagePointSet();
      unrefinedIds.put(id, uset);
    } else {
      uset = unrefinedIds.get(id);
    }
    return uset;
  }

  private void calculateUnsafesIfNecessary() {
    if (unsafeUsages == -1) {
      processedUnsafes.clear();
      unsafeUsages = 0;
      Set<SingleIdentifier> toDelete = new HashSet<>();

      for (Entry<SingleIdentifier, UnrefinedUsagePointSet> entry : unrefinedIds.entrySet()) {
        UnrefinedUsagePointSet tmpList = entry.getValue();
        if (detector.isUnsafe(tmpList)) {
          unsafeUsages += tmpList.size();
        } else {
          SingleIdentifier id = entry.getKey();
          toDelete.add(id);
          falseUnsafes.add(id);
        }
      }
      toDelete.forEach(this::removeIdFromCaches);

      refinedIds.forEach((id, list) -> unsafeUsages += list.size());

      if (initialSet == null) {
        assert refinedIds.isEmpty();
        initialSet = new HashSet<>(unrefinedIds.keySet());
        initialUsages = unsafeUsages;
      }
    }
  }

  private void removeIdFromCaches(SingleIdentifier id) {
    unrefinedIds.remove(id);
    processedUnsafes.add(id);
  }

  public Set<SingleIdentifier> getFalseUnsafes() {
    Set<SingleIdentifier> currentUnsafes = getAllUnsafes();
    return Sets.difference(initialSet, currentUnsafes);
  }

  private Set<SingleIdentifier> getAllUnsafes() {
    calculateUnsafesIfNecessary();
    Set<SingleIdentifier> result = new TreeSet<>(unrefinedIds.keySet());
    result.addAll(refinedIds.keySet());
    result.addAll(failedIds.keySet());
    return result;
  }

  public Iterator<SingleIdentifier> getUnsafeIterator() {
    if (config.printOnlyTrueUnsafes()) {
      return getTrueUnsafeIterator();
    } else {
      return getAllUnsafes().iterator();
    }
  }

  public Iterator<SingleIdentifier> getUnrefinedUnsafeIterator() {
    // New set to avoid concurrent modification exception
    return getKeySetIterator(unrefinedIds);
  }

  private Iterator<SingleIdentifier> getTrueUnsafeIterator() {
    // New set to avoid concurrent modification exception
    return getKeySetIterator(refinedIds);
  }

  private Iterator<SingleIdentifier> getKeySetIterator(
      NavigableMap<SingleIdentifier, ? extends AbstractUsagePointSet> map) {
    Set<SingleIdentifier> result = new TreeSet<>(map.keySet());
    return result.iterator();
  }

  public int getUnsafeSize() {
    calculateUnsafesIfNecessary();
    if (config.printOnlyTrueUnsafes()) {
      return refinedIds.size();
    } else {
      return getTotalUnsafeSize();
    }
  }

  public int getTotalUnsafeSize() {
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
    unsafeUsages = -1;
    unrefinedIds.values().forEach(UnrefinedUsagePointSet::reset);
    logger.log(Level.FINE, "Unsafes are reseted");
    resetTimer.stop();
  }

  public void removeState(final UsageState pUstate) {
    unrefinedIds.forEach((id, uset) -> uset.remove(pUstate));
    logger.log(
        Level.ALL,
        "All unsafes related to key state " + pUstate + " were removed from reached set");
  }

  public AbstractUsagePointSet getUsages(SingleIdentifier id) {
    if (unrefinedIds.containsKey(id)) {
      return unrefinedIds.get(id);
    } else if (refinedIds.containsKey(id)) {
      return refinedIds.get(id);
    } else {
      return failedIds.get(id);
    }
  }

  public void setAsFalseUnsafe(SingleIdentifier id) {
    falseUnsafes.add(id);
    removeIdFromCaches(id);
  }

  public void setAsRefined(SingleIdentifier id, RefinementResult result) {
    Preconditions.checkArgument(
        result.isTrue(), "Result is not true, can not set the set as refined");
    checkArgument(
        detector.isUnsafe(getUsages(id)),
        "Refinement is successful, but the unsafe is absent for identifier %s",
        id);

    setAsRefined(id, result.getTrueRace().getFirst(), result.getTrueRace().getSecond());
  }

  public void setAsRefined(SingleIdentifier id, UsageInfo firstUsage, UsageInfo secondUsage) {
    RefinedUsagePointSet rSet = RefinedUsagePointSet.create(firstUsage, secondUsage);
    if (firstUsage.isLooped() || secondUsage.isLooped()) {
      failedIds.put(id, rSet);
    } else {
      refinedIds.put(id, rSet);
    }
    removeIdFromCaches(id);
  }

  public void printUsagesStatistics(StatisticsWriter out) {
    int unsafeSize = getTotalUnsafeSize();
    StatInt topUsagePoints = new StatInt(StatKind.SUM, "Total amount of unrefined usage points");
    StatInt unrefinedUsages = new StatInt(StatKind.SUM, "Total amount of unrefined usages");
    StatInt refinedUsages = new StatInt(StatKind.SUM, "Total amount of refined usages");
    StatCounter failedUsages = new StatCounter("Total amount of failed usages");

    final int generalUnrefinedSize = unrefinedIds.size();
    for (UnrefinedUsagePointSet uset : unrefinedIds.values()) {
      unrefinedUsages.setNextValue(uset.size());
      topUsagePoints.setNextValue(uset.getNumberOfTopUsagePoints());
    }

    final int generalRefinedSize = refinedIds.size();
    refinedIds.forEach((id, rset) -> refinedUsages.setNextValue(rset.size()));

    final int generalFailedSize = failedIds.size();
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
        .put("Initial amount of unsafes (before refinement)", initialSet.size())
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
        .put(copyTimer)
        .put(emptyEffectsTimer);
  }

  public Set<SingleIdentifier> getProcessedUnsafes() {
    return processedUnsafes;
  }
}
