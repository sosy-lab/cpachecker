/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.seplogic.SeplogicCPA;
import org.sosy_lab.cpachecker.cpa.seplogic.SeplogicElement;
import org.sosy_lab.cpachecker.cpa.seplogic.SeplogicElement.SeplogicQueryUnsuccessful;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractElements;

@Options(prefix="biabductor")
public class BiabductionAlgorithm implements Algorithm, StatisticsProvider {

  private static class BiabductionStatistics implements Statistics {

    private final Timer totalTimer = new Timer();
    private final Timer refinementTimer = new Timer();
    private final Timer gcTimer = new Timer();

    private volatile int countRefinements = 0;

    @Override
    public String getName() {
      return "Biabduction algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult,
        ReachedSet pReached) {

      out.println("Number of biabductions:            " + countRefinements);

      if (countRefinements > 0) {
        out.println("Total time for biabduction algorithm: " + totalTimer);
        out.println("Time for refinements:                 " + refinementTimer);
        out.println("Average time for refinement:          " + refinementTimer.printAvgTime());
        out.println("Max time for refinement:              " + refinementTimer.printMaxTime());
        out.println("Time for garbage collection:          " + gcTimer);
      }
    }
  }

  private final BiabductionStatistics stats = new BiabductionStatistics();

  private static final int GC_PERIOD = 100;
  private int gcCounter = 0;

  private final LogManager logger;
  private final Algorithm algorithm;

  private ConfigurableProgramAnalysis cpa;
  private SeplogicCPA slCPA;

  private Configuration config;


  public BiabductionAlgorithm(Algorithm algorithm, ConfigurableProgramAnalysis pCpa, Configuration config, LogManager logger) throws InvalidConfigurationException, CPAException {
    config.inject(this);
    this.algorithm = algorithm;
    this.config = config;
    this.logger = logger;
    this.cpa = pCpa;
    this.slCPA = ((WrapperCPA) pCpa).retrieveWrappedCpa(SeplogicCPA.class);
  }


  @Override
  public boolean run(ReachedSet reached) throws CPAException, InterruptedException {
    boolean sound = true;
    Set<Integer> seenErrorNodes = new HashSet<Integer>();

    stats.totalTimer.start();

    boolean continueAnalysis;
    do {
      continueAnalysis = false;

      // run algorithm
      try {
        sound &= algorithm.run(reached);
      } catch (SeplogicQueryUnsuccessful e) {
        SeplogicElement elem = AbstractElements.extractElementByType(reached.getLastElement(), SeplogicElement.class);
        slCPA.setAbductionState(elem.getMissing());
        int lastLoc = AbstractElements.extractLocation(reached.getLastElement()).getNodeNumber();

        if (!seenErrorNodes.contains(lastLoc)) {
          seenErrorNodes.add(lastLoc);
          continueAnalysis = true;

          CFANode mainFunction = AbstractElements.extractLocation(reached.getFirstElement());
          ReachedSetFactory singleReachedSetFactory;
          try {
            singleReachedSetFactory = new ReachedSetFactory(config, logger);
          } catch (InvalidConfigurationException e1) {
            throw new RuntimeException(e1);
          }
          AbstractElement initialElement = cpa.getInitialElement(mainFunction);
          Precision initialPrecision = cpa.getInitialPrecision(mainFunction);

          reached = singleReachedSetFactory.create();
          reached.add(initialElement, initialPrecision);
        }
      }

    } while (continueAnalysis);

    stats.totalTimer.stop();
    return sound;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
  }

}