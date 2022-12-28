// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.Strategy.StrategyQualifier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.location.LocationPrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;

public class SummaryStrategyUnderapproximatingRefiner implements Refiner {

  private final LogManager logger;
  private int refinementNumber;
  protected final ARGCPA argCpa;

  private SummaryInformation summaryInformation;

  private SummaryStrategyUnderapproximatingRefiner(
      LogManager pLogger, final ConfigurableProgramAnalysis pCpa, CFA pCfa)
      throws InvalidConfigurationException {
    logger = pLogger;
    argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, Refiner.class);
    summaryInformation = pCfa.getSummaryInformation().orElseThrow();
  }

  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    ARGCPA argCpa =
        CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, SummaryStrategyUnderapproximatingRefiner.class);
    LogManager logger = argCpa.getLogger();

    return new SummaryStrategyUnderapproximatingRefiner(logger, pCpa, argCpa.getCfa());
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    logger.log(Level.FINE, "Refining underapproximating loopsummary strategies");
    assert ARGUtils.checkARG(pReached) : "ARG and reached set do not match before refinement";

    ARGReachedSet reached = new ARGReachedSet(pReached, argCpa, refinementNumber++);

    Optional<AbstractState> optionalRefinementState = Optional.empty();
    Optional<GhostCFA> optionalStrategy = Optional.empty();

    // TODO: Make the search for a underapproximating state smarter. This would imply getting
    // something like craig interpolants but for proofs.
    for (AbstractState state : reached.asReachedSet()) {
      LocationPrecision locationPrecision =
          ((WrapperPrecision) pReached.getPrecision(state))
              .retrieveWrappedPrecision(LocationPrecision.class);
      if (locationPrecision.getCurrentStrategy().isPresent()
          && locationPrecision.getCurrentStrategy().orElseThrow().getStrategyQualifier()
              == StrategyQualifier.Underapproximating) {
        optionalRefinementState = Optional.of(state);
        optionalStrategy = Optional.of(locationPrecision.getCurrentStrategy().orElseThrow());
        break;
      }
    }

    if (optionalRefinementState.isEmpty() || optionalStrategy.isEmpty()) {
      return false;
    } else {
      ARGState refinementState = (ARGState) optionalRefinementState.orElseThrow();
      LocationPrecision locationPrecision =
          ((WrapperPrecision) pReached.getPrecision(refinementState))
              .retrieveWrappedPrecision(LocationPrecision.class);

      // TODO: Make a deepcopy of the ghostCFA of the current strategy before adding it to the
      // forbidden strategies. Since the parameters of the ghostCFA may be updated.
      locationPrecision.addForbiddenStrategy(locationPrecision.getCurrentStrategy().orElseThrow());

      // Update the parameters of the current Strategy if wanted

      // Set the new best strategy
      // TODO: Improve the new strategy selection
      locationPrecision.setCurrentStrategy(
          summaryInformation.getBestAllowedStrategy(
              AbstractStates.extractLocation(refinementState), locationPrecision));

      // Using reached.removeSubtree does not remove only the children elements, but also the
      // element itself. Which in turn also removes the updated precision
      ArrayList<ARGState> children = Lists.newArrayList(refinementState.getChildren());

      for (int i = 0; i < children.size(); i++) {
        reached.removeSubtree(children.get(i));
      }

      return true;
    }
  }
}
