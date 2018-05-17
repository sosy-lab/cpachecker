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
package org.sosy_lab.cpachecker.cpa.usage.refinement;

import com.google.common.collect.Sets;
import java.util.Iterator;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.cpa.usage.storage.UnsafeDetector;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageContainer;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageInfoSet;
import org.sosy_lab.cpachecker.util.Pair;


public class UsagePairIterator extends GenericIterator<Pair<UsageInfoSet, UsageInfoSet>, Pair<UsageInfo, UsageInfo>>{
  private final LogManager logger;

  //internal state
  private Iterator<UsageInfo> firstUsageIterator;
  private Iterator<UsageInfo> secondUsageIterator;
  private UsageInfo firstUsage = null;
  private UsageInfoSet secondUsageInfoSet;

  private UnsafeDetector detector;

  public UsagePairIterator(ConfigurableRefinementBlock<Pair<UsageInfo, UsageInfo>> pWrapper, LogManager l) {
    super(pWrapper);
    logger = l;
  }

  @Override
  protected void init(Pair<UsageInfoSet, UsageInfoSet> pInput) {
    UsageInfoSet firstUsageInfoSet;
    firstUsageInfoSet = pInput.getFirst();
    secondUsageInfoSet = pInput.getSecond();

    firstUsageIterator = firstUsageInfoSet.iterator();
    secondUsageIterator = secondUsageInfoSet.iterator();

    firstUsage = null;
  }

  @Override
  protected Pair<UsageInfo, UsageInfo> getNext(Pair<UsageInfoSet, UsageInfoSet> pInput) {
    if (firstUsage == null) {
      //first call - initialize it
      if (firstUsageIterator.hasNext()) {
        firstUsage = firstUsageIterator.next();
      } else {
        return null;
      }
    }

    Pair<UsageInfo, UsageInfo> result = checkSecondIterator();

    if (result == null && firstUsageIterator.hasNext()) {
      firstUsage = firstUsageIterator.next();
      secondUsageIterator = pInput.getSecond().iterator();
      result = checkSecondIterator();
    }
    if (result == null) {
      return null;
    } else {
      UsageInfo firstUsage = result.getFirst();
      UsageInfo secondUsage = result.getSecond();
      if (firstUsage == secondUsage) {
        secondUsage = secondUsage.copy();
      }
      return Pair.of(firstUsage, secondUsage);
    }
  }

  private Pair<UsageInfo, UsageInfo> checkSecondIterator() {
    while (secondUsageIterator.hasNext()) {
      UsageInfo secondUsage = secondUsageIterator.next();
      if (!secondUsage.isReachable()) {
        //It may happens if we refine to same sets (first == second)
        //It is normal, just skip
      } else {
        if (detector.isUnsafe(Sets.newHashSet(firstUsage, secondUsage))) {
          return Pair.of(firstUsage, secondUsage);
        }
      }
    }
    return null;
  }

  @Override
  protected void finishIteration(Pair<UsageInfo, UsageInfo> usagePair, RefinementResult r) {
    UsageInfo first = usagePair.getFirst();
    UsageInfo second = usagePair.getSecond();

    if (!second.isReachable()) {
      logger.log(Level.FINE, "Usage " + secondUsageIterator + " is not reachable, remove it from container");
      secondUsageIterator.remove();
    }
    if (!first.isReachable()) {
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
  protected void
      handleUpdateSignal(Class<? extends RefinementInterface> pCallerClass, Object pData) {
    if (pCallerClass.equals(IdentifierIterator.class)) {
      assert pData instanceof UsageContainer;
      UsageContainer container = (UsageContainer) pData;
      detector = container.getUnsafeDetector();
    }
  }
}
