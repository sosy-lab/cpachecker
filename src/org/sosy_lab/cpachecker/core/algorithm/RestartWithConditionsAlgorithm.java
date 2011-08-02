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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.ProgressObserverPrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.collect.Iterables;

@Options(prefix="adjustableconditions")
public class RestartWithConditionsAlgorithm implements Algorithm, StatisticsProvider {

  private final AssumptionCollectorAlgorithm innerAlgorithm;
  private final LogManager logger;

  public RestartWithConditionsAlgorithm(Algorithm algorithm, Configuration config, LogManager logger) throws InvalidConfigurationException
  {
    config.inject(this);
    this.logger = logger;
    if (!(algorithm instanceof AssumptionCollectorAlgorithm)) {
      throw new InvalidConfigurationException("Assumption Algorithm needed for RestartWithConditionsAlgorithm");
    }
    innerAlgorithm = (AssumptionCollectorAlgorithm)algorithm;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return innerAlgorithm.getCPA();
  }

  @Override
  public boolean run(ReachedSet pReached) throws CPAException,
  InterruptedException {
    boolean sound = true;

    boolean restartCPA = false;

    // loop if restartCPA is set to false
    do {
      restartCPA = false;
      // run the inner algorithm to fill the reached set
      sound = innerAlgorithm.run(pReached);

      List<AbstractElement> elementsWithAssumptions = innerAlgorithm.getElementsWithAssumptions(pReached);

      if (Iterables.any(pReached, AbstractElements.IS_TARGET_ELEMENT)) {
        return sound;
      }

      // if there are elements that an assumption is generated for
      if(elementsWithAssumptions.size() > 0) {
        logger.log(Level.INFO, "Adjusting heuristics thresholds.");
        // if any of the elements' threshold is adjusted
        if(adjustThresholds(elementsWithAssumptions, pReached)){
          restartCPA = true;
        }
        // no elements adjusted but there are elements with assumptions
        // the analysis should report UNSOUND
        else{
          sound = false;
        }
      }

    } while (restartCPA);

    return sound;
  }

  private boolean adjustThresholds(List<AbstractElement> pElementsWithAssumptions, ReachedSet pReached) {
    boolean precisionAdjusted = false;
    for(AbstractElement e: pElementsWithAssumptions){
      ARTElement artElement = (ARTElement)e;
      Set<ARTElement> parents = artElement.getParents();
      pReached.remove(e);
      for(AbstractElement parent: parents){
        precisionAdjusted |= adjustThreshold(parent, pReached);
        pReached.reAddToWaitlist(parent);
      }
    }
    return precisionAdjusted;
  }

  private boolean adjustThreshold(AbstractElement pParent, ReachedSet pReached) {
    ProgressObserverPrecision observerPrecision = Precisions.extractPrecisionByType(pReached.getPrecision(pParent), ProgressObserverPrecision.class);
    return observerPrecision.adjustPrecisions();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    // TODO Auto-generated method stub

  }
}
