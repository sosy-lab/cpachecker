// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.StrategyInterface;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;

public class LoopSummaryBasedRefiner implements Refiner, StatisticsProvider {

  private final LogManager logger;

  private final Refiner firstRefiner;
  private final Refiner secondRefiner;
  protected final ARGCPA argCpa;
  private List<StrategyInterface> strategies;

  private final int maxAmntFirstRefinements;

  private int amntFirstRefinements = 0;

  public LoopSummaryBasedRefiner(
      Refiner pFirstRefiner,
      Refiner pSecondRefiner,
      LogManager pLogger,
      ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    firstRefiner = pFirstRefiner;
    secondRefiner = pSecondRefiner;
    logger = pLogger;
    argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, Refiner.class);
    maxAmntFirstRefinements =
        CPAs.retrieveCPAOrFail(pCpa, LoopSummaryCPA.class, Refiner.class).maxAmntFirstRefinements;
    strategies = CPAs.retrieveCPAOrFail(pCpa, LoopSummaryCPA.class, Refiner.class).getStrategies();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (firstRefiner instanceof StatisticsProvider) {
      ((StatisticsProvider) firstRefiner).collectStatistics(pStatsCollection);
    }
    if (secondRefiner instanceof StatisticsProvider) {
      ((StatisticsProvider) secondRefiner).collectStatistics(pStatsCollection);
    }
  }

  private boolean containsSummaryStrategy(ReachedSet pReached) {
    logger.log(
        Level.INFO,
        "Maximum amount of refinement Steps exceeded, seein if we can refine with the second refiner.");
    assert ARGUtils.checkARG(pReached) : "ARG and reached set do not match before refinement";

    final ARGState lastElement = (ARGState) pReached.getLastState();

    Collection<ARGState> waitlist = new ArrayList<>();
    Collection<ARGState> seen = new ArrayList<>();
    waitlist.add(lastElement);
    Optional<ARGState> optionalRefinementState = Optional.empty();
    while (!waitlist.isEmpty()) {
      Iterator<ARGState> iter = waitlist.iterator();
      Collection<ARGState> newWaitlist = new ArrayList<>();
      while (iter.hasNext()) {
        ARGState currentElement = iter.next();
        /*logger.log(
        Level.INFO,
        "State: " + currentElement + "\nPrecision: " + pReached.getPrecision(currentElement));*/
        if (!strategies
                .get(
                    ((LoopSummaryPrecision) pReached.getPrecision(currentElement))
                        .getStrategyCounter())
                .isPrecise()
            && ((LoopSummaryPrecision) pReached.getPrecision(currentElement)).isLoopHead()) {
          optionalRefinementState = Optional.of(currentElement);
          waitlist.clear();
          newWaitlist.clear();
          break;
        } else {
          if (!seen.contains(currentElement)) {
            newWaitlist.addAll(currentElement.getParents());
            seen.add(currentElement);
          }
        }
      }
      waitlist = newWaitlist;
    }
    return !optionalRefinementState.isEmpty();
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {

    if (amntFirstRefinements > maxAmntFirstRefinements) {
      amntFirstRefinements = 0;
      if (containsSummaryStrategy(pReached)) {
        return secondRefiner.performRefinement(pReached);
      } else {
        return firstRefiner.performRefinement(pReached);
      }
    } else {
      if (!firstRefiner.performRefinement(pReached)) {
        amntFirstRefinements = 0;
        logger.log(Level.INFO, "Performing Double refinement");
        return secondRefiner.performRefinement(pReached);
      } else {
        amntFirstRefinements += 1;
        return true;
      }
    }
  }
}
