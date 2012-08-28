/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.ProgressObserverCPA;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.ProgressObserverPrecision;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageCPA;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

public class RestartWithConditionsAlgorithm implements Algorithm {

  private final Algorithm innerAlgorithm;
  private final ARTCPA cpa;
  private final LogManager logger;

  public RestartWithConditionsAlgorithm(Algorithm pAlgorithm,
        ConfigurableProgramAnalysis pCpa, Configuration config, LogManager pLogger)
        throws InvalidConfigurationException {

    logger = pLogger;
    innerAlgorithm = pAlgorithm;

    if (!(pCpa instanceof ARTCPA)) {
      throw new InvalidConfigurationException("ARTCPA needed for RestartWithConditionsAlgorithm");
    }
    cpa = (ARTCPA)pCpa;
    if (cpa.retrieveWrappedCpa(AssumptionStorageCPA.class) == null) {
      throw new InvalidConfigurationException("AssumptionStorageCPA needed for RestartWithConditionsAlgorithm");
    }
    if (cpa.retrieveWrappedCpa(ProgressObserverCPA.class) == null) {
      throw new InvalidConfigurationException("ProgressObserverCPA needed for RestartWithConditionsAlgorithm");
    }
  }

  @Override
  public boolean run(ReachedSet pReached) throws CPAException, InterruptedException {
    boolean sound = true;

    boolean restartCPA;

    // loop if restartCPA is set to false
    do {
      restartCPA = false;
      // run the inner algorithm to fill the reached set
      sound &= innerAlgorithm.run(pReached);

      if (Iterables.any(pReached, AbstractElements.IS_TARGET_ELEMENT)) {
        return sound;
      }

      List<AbstractElement> elementsWithAssumptions = getElementsWithAssumptions(pReached);

      // if there are elements that an assumption is generated for
      if (!elementsWithAssumptions.isEmpty()) {
        logger.log(Level.INFO, "Adjusting heuristics thresholds.");
        // if any of the elements' threshold is adjusted
        if (adjustThresholds(elementsWithAssumptions, pReached)){
          restartCPA = true;

        } else {
          // no elements adjusted but there are elements with assumptions
          // the analysis should report UNSOUND
          sound = false;
        }
      }

    } while (restartCPA);

    return sound;
  }

  private List<AbstractElement> getElementsWithAssumptions(ReachedSet reached) {

    List<AbstractElement> retList = new ArrayList<AbstractElement>();

    for (AbstractElement element : reached) {

      // TODO do we need target elements?
//      if (AbstractElements.isTargetElement(element)) {
//        // create assumptions for target element
//        retList.add(element);
//
//      } else {

        // check if stored assumption is not "true"
        AssumptionStorageElement e = AbstractElements.extractElementByType(element, AssumptionStorageElement.class);

        if (!e.getAssumption().isTrue()
            || !e.getStopFormula().isTrue()) {

          retList.add(element);
        }
    }

    return retList;
  }

  private boolean adjustThresholds(List<AbstractElement> pElementsWithAssumptions, ReachedSet pReached) {
    boolean precisionAdjusted = false;

    ARTReachedSet reached = new ARTReachedSet(pReached, cpa);
    for (AbstractElement e: pElementsWithAssumptions) {
      ARTElement artElement = (ARTElement)e;

      for (ARTElement parent : ImmutableSet.copyOf(artElement.getParents())){
        precisionAdjusted |= adjustThreshold(parent, pReached);
        reached.removeSubtree(parent);
      }
    }
    return precisionAdjusted;
  }

  private boolean adjustThreshold(AbstractElement pParent, ReachedSet pReached) {
    ProgressObserverPrecision observerPrecision = Precisions.extractPrecisionByType(pReached.getPrecision(pParent), ProgressObserverPrecision.class);
    return observerPrecision.adjustPrecisions();
  }

  @Override
  public boolean reset() {
    return false;
  }
}