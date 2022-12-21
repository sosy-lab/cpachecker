// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.location.LocationPrecision;
import org.sosy_lab.cpachecker.cpa.value.refiner.SummaryValueRefiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;

public class SummaryStrategyRefiner implements Refiner {

  private final LogManager logger;
  private int refinementNumber;
  protected final ARGCPA argCpa;
  private SummaryInformation summaryInformation;

  private SummaryStrategyRefiner(
      LogManager pLogger, final ConfigurableProgramAnalysis pCpa, CFA pCfa)
      throws InvalidConfigurationException {
    logger = pLogger;
    argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, Refiner.class);
    summaryInformation = pCfa.getSummaryInformation().orElseThrow();
  }

  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    ARGCPA argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, SummaryValueRefiner.class);
    LogManager logger = argCpa.getLogger();

    return new SummaryStrategyRefiner(logger, pCpa, argCpa.getCfa());
  }

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
    Optional<StrategiesEnum> optionalStrategy = Optional.empty();
    while (!waitlist.isEmpty()) {
      Iterator<ARGState> iter = waitlist.iterator();
      Collection<ARGState> newWaitlist = new ArrayList<>();
      while (iter.hasNext()) {
        ARGState currentElement = iter.next();
        CFANode node = AbstractStates.extractLocation(currentElement);
        if (node.getNumEnteringEdges() == 1 && !summaryInformation
            .getFactory()
            .buildStrategy(SummaryUtils.getStrategyForEdge(node.getEnteringEdge(0)))
            .isPrecise()) {
            optionalRefinementState = Optional.of(currentElement);
          optionalStrategy = Optional.of(SummaryUtils.getStrategyForEdge(node.getEnteringEdge(0)));
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

    if (optionalRefinementState.isEmpty() || optionalStrategy.isEmpty()) {
      return false;
    } else {
      ARGState refinementState = optionalRefinementState.orElseThrow();

      if (optionalStrategy.orElseThrow() != StrategiesEnum.BASE) {
        for (ARGState parentState : refinementState.getParents()) {
          LocationPrecision locationPrecision =
              ((WrapperPrecision) pReached.getPrecision(parentState))
                  .retrieveWrappedPrecision(LocationPrecision.class);
          locationPrecision.addUnallowedStrategy(optionalStrategy.orElseThrow());
        }
      }

      reached.removeSubtree(refinementState);

      return true;
    }
  }
}
