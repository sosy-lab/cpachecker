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
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.StrategyInterface;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;

public class LoopSummaryStrategyRefiner implements Refiner {

  private final LogManager logger;
  private int refinementNumber;
  protected final ARGCPA argCpa;
  private List<StrategyInterface> strategies;

  public LoopSummaryStrategyRefiner(LogManager pLogger, final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    logger = pLogger;
    argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, Refiner.class);
    strategies = CPAs.retrieveCPAOrFail(pCpa, LoopSummaryCPA.class, Refiner.class).getStrategies();
  }

  @SuppressWarnings("unused")
  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    logger.log(Level.INFO, "Using the second refiner, to refine loopsummary strategies");
    assert ARGUtils.checkARG(pReached) : "ARG and reached set do not match before refinement";

    final ARGState lastElement = (ARGState) pReached.getLastState();
    assert lastElement.isTarget()
        : "Last element in reached is not a target state before refinement";
    ARGReachedSet reached = new ARGReachedSet(pReached, argCpa, refinementNumber++);

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

    if (optionalRefinementState.isEmpty()) {
      return false;
    } else {
      ARGState refinementState = optionalRefinementState.orElseThrow();
      LoopSummaryPrecision newPrecision =
          (LoopSummaryPrecision) pReached.getPrecision(refinementState);
      newPrecision.updateStrategy();
      reached.removeSubtree(refinementState);
    }

    return false;
  }
}
