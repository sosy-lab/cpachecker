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

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cpa.usage.storage.AbstractUsagePointSet;
import org.sosy_lab.cpachecker.cpa.usage.storage.UnrefinedUsagePointSet;
import org.sosy_lab.cpachecker.cpa.usage.storage.UnsafeDetector;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageContainer;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageInfoSet;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsagePoint;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;


public class PointIterator extends GenericIterator<SingleIdentifier, Pair<UsageInfoSet, UsageInfoSet>>{

  private UsageContainer container;
  private UnsafeDetector detector;

  //Internal state
  //private Pair<UsagePoint, UsagePoint> pairToRefine;
  private UsagePoint firstPoint;
  private UsagePoint secondPoint;
  private UnrefinedUsagePointSet currentUsagePointSet;

  private Set<UsagePoint> toRemove = new HashSet<>();

  private final boolean considerTheSameUsagePointsAsRace = true;

  public PointIterator(ConfigurableRefinementBlock<Pair<UsageInfoSet, UsageInfoSet>> pWrapper, UsageContainer c) {
    super(pWrapper);
    if (c != null) {
      container = c;
      detector = c.getUnsafeDetector();
    }
  }

  @Override
  protected void init(SingleIdentifier id) {
    AbstractUsagePointSet pointSet = container.getUsages(id);

    assert (pointSet instanceof UnrefinedUsagePointSet);

    currentUsagePointSet = (UnrefinedUsagePointSet)pointSet;
    firstPoint = currentUsagePointSet.next(null);
    assert firstPoint != null;
    secondPoint = firstPoint;
    Pair<UsageInfoSet, UsageInfoSet> resultingPair = prepareIterationPair(firstPoint, secondPoint);
    //because the points are equal
    postpone(resultingPair);
    toRemove.clear();
  }

  @Override
  protected Pair<UsageInfoSet, UsageInfoSet> getNext(SingleIdentifier pInput) {
    do {
      if (!prepareNextPair()) {
        return null;
      }
      assert firstPoint != null && secondPoint != null;
    } while (!detector.isUnsafePair(firstPoint, secondPoint));

    Pair<UsageInfoSet, UsageInfoSet> resultingPair = prepareIterationPair(firstPoint, secondPoint);
    if (firstPoint == secondPoint) {
      postpone(resultingPair);
      return getNext(pInput);
    }
    return resultingPair;
  }

  private boolean prepareNextPair() {
    if (!iterateSecondPoint()) {
      return false;
    }
    //Check, if we need to remove smth
    if (!toRemove.isEmpty()) {
      for (UsagePoint r : toRemove) {
        if (firstPoint == r) {
          firstPoint = currentUsagePointSet.next(firstPoint);
          secondPoint = currentUsagePointSet.next(null);
          if (firstPoint == null) {
            return false;
          }
        }
        if (secondPoint == r) {
          if (!iterateSecondPoint()) {
            return false;
          }
        }
        assert secondPoint != r;
        currentUsagePointSet.remove(r);
      }
    }

    return true;
  }

  private boolean iterateSecondPoint() {
    secondPoint = currentUsagePointSet.next(secondPoint);
    if (secondPoint == null) {
      firstPoint = currentUsagePointSet.next(firstPoint);
      if (firstPoint == null) {
        return false;
      }
      //Start from first point to save the time
      if (considerTheSameUsagePointsAsRace) {
        secondPoint = firstPoint;
      } else {
        secondPoint = currentUsagePointSet.next(firstPoint);
      }
    }
    return true;
  }

  private Pair<UsageInfoSet, UsageInfoSet> prepareIterationPair(UsagePoint first, UsagePoint second) {
    UsageInfoSet firstUsageInfoSet = currentUsagePointSet.getUsageInfo(first);
    UsageInfoSet secondUsageInfoSet = currentUsagePointSet.getUsageInfo(second);
    UsageInfoSet secondSet = secondUsageInfoSet;

    if (firstUsageInfoSet == secondUsageInfoSet) {
      //To avoid concurrent modification
      secondSet = secondSet.clone();
    }

    assert secondSet != null;
    return Pair.of(firstUsageInfoSet, secondSet);
  }

  @Override
  protected void finalize(Pair<UsageInfoSet, UsageInfoSet> pPair, RefinementResult r) {
    UsageInfoSet firstUsageInfoSet = pPair.getFirst();
    UsageInfoSet secondUsageInfoSet = pPair.getSecond();

    if (firstUsageInfoSet.size() == 0) {
      //No reachable usages - remove point
      toRemove.add(firstPoint);
    }
    if (secondUsageInfoSet.size() == 0) {
      //No reachable usages - remove point
      toRemove.add(secondPoint);
    }
  }


  @Override
  protected void handleUpdateSignal(Class<? extends RefinementInterface> pCallerClass, Object pData) {
    if (pCallerClass.equals(IdentifierIterator.class)) {
      assert pData instanceof UsageContainer;
      container = (UsageContainer) pData;
      detector = container.getUnsafeDetector();
    }
  }

 /* @Override
  protected Object handleFinishSignal(Class<? extends RefinementInterface> pCallerClass) {
    if (pCallerClass.equals(IdentifierIterator))
    return null;
  }*/

  @Override
  protected void printDetailedStatistics(PrintStream pOut) {
    pOut.println("--PointIterator--");
  }
}
