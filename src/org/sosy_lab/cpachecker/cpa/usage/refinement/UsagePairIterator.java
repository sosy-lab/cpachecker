// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.refinement;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageInfoSet;
import org.sosy_lab.cpachecker.util.Pair;


public class UsagePairIterator extends GenericIterator<Pair<UsageInfoSet, UsageInfoSet>, Pair<UsageInfo, UsageInfo>>{
  private final LogManager logger;

  //internal state
  private Iterator<UsageInfo> firstUsageIterator;
  private Iterator<UsageInfo> secondUsageIterator;
  private UsageInfo firstUsage = null;
  private UsageInfoSet secondUsageInfoSet;
  private final int iterationLimit;
  private int[] consideredUsages = new int[2];

  public UsagePairIterator(
      ConfigurableRefinementBlock<Pair<UsageInfo, UsageInfo>> pWrapper,
      LogManager l,
      ShutdownNotifier pNotifier,
      int pIterationLimit) {
    super(pWrapper, pNotifier);
    logger = l;
    iterationLimit = pIterationLimit;
  }

  @Override
  protected void init(Pair<UsageInfoSet, UsageInfoSet> pInput) {
    UsageInfoSet firstUsageInfoSet;
    firstUsageInfoSet = pInput.getFirst();
    secondUsageInfoSet = pInput.getSecond();

    firstUsageIterator = firstUsageInfoSet.iterator();
    secondUsageIterator = secondUsageInfoSet.iterator();

    firstUsage = null;
    consideredUsages[0] = 0;
    consideredUsages[1] = 0;
  }

  @Override
  protected Pair<UsageInfo, UsageInfo> getNext(Pair<UsageInfoSet, UsageInfoSet> pInput) {
    if (consideredUsages[0] >= iterationLimit) {
      return null;
    }

    if (firstUsage == null) {
      //first call - initialize it
      firstUsage = checkFirstIterator();
      if (firstUsage == null) {
        return null;
      }
    }

    Pair<UsageInfo, UsageInfo> result = checkSecondIterator();

    if (result == null) {
      firstUsage = checkFirstIterator();
      if (firstUsage == null) {
        return null;
      }
      secondUsageIterator = pInput.getSecond().iterator();
      result = checkSecondIterator();
      if (result == null) {
        return null;
      }
    }
    UsageInfo resultFirstUsage = result.getFirst();
    UsageInfo resultSecondUsage = result.getSecond();
    if (resultFirstUsage == resultSecondUsage) {
      resultSecondUsage = resultSecondUsage.copy();
    }
    return Pair.of(resultFirstUsage, resultSecondUsage);
  }

  private UsageInfo checkFirstIterator() {
    if (consideredUsages[0] < iterationLimit && firstUsageIterator.hasNext()) {
      consideredUsages[0]++;
      return firstUsageIterator.next();
    }
    return null;
  }

  private Pair<UsageInfo, UsageInfo> checkSecondIterator() {
    if (consideredUsages[1] < iterationLimit && secondUsageIterator.hasNext()) {
      consideredUsages[1]++;
      return Pair.of(firstUsage, secondUsageIterator.next());
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void finishIteration(Pair<UsageInfo, UsageInfo> usagePair, RefinementResult r) {
    UsageInfo first = usagePair.getFirst();
    UsageInfo second = usagePair.getSecond();

    List<UsageInfo> unreachableUsages = (List<UsageInfo>) r.getInfo(PathPairIterator.class);

    if (unreachableUsages != null && unreachableUsages.contains(second)) {
      logger.log(Level.FINE, "Usage " + secondUsageIterator + " is not reachable, remove it from container");
      secondUsageIterator.remove();
    }
    if (unreachableUsages != null && unreachableUsages.contains(first)) {
      logger.log(Level.FINE, "Usage " + firstUsageIterator + " is not reachable, remove it from container");
      firstUsageIterator.remove();
      firstUsage = null;
      secondUsageIterator = secondUsageInfoSet.iterator();
    }
    if ((first.isLooped() || second.isLooped()) && first.equals(second)) {
      first.setAsLooped();
      second.setAsLooped();
    }
  }

  @Override
  protected void handleFinishSignal(Class<? extends RefinementInterface> pCallerClass) {
    if (pCallerClass.equals(IdentifierIterator.class)) {
      firstUsageIterator = null;
      secondUsageIterator = null;
      firstUsage = null;
      secondUsageInfoSet = null;
    }
  }
}
