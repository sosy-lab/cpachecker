// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.BooleanFormula;

// import org.sosy_lab.cpachecker.util.automaton.CachingTargetLocationProvider;
public class BackwardBMCAlgorithm implements Algorithm {

  private LogManager logger;
  private Algorithm algorithm;
  private ConfigurableProgramAnalysis cpa;

  public BackwardBMCAlgorithm(
      Algorithm pAlgorithm, ConfigurableProgramAnalysis pCPA, LogManager pLogger) {

    logger = pLogger;
    algorithm = pAlgorithm;
    cpa = pCPA;
  }

  @Override
  public AlgorithmStatus run(final ReachedSet reachedSet)
      throws CPAException, InterruptedException {

    AlgorithmStatus status;
    status = BMCHelper.unroll(logger, reachedSet, algorithm, cpa);

    // is this correct? How will we get path formulas of loop heads?
    Optional<AbstractState> optTarget = getTarget(reachedSet);
    AbstractState target;
    if (optTarget.isPresent()) {
      // this is the main entry
      target = optTarget.get();
    } else {
      return status;
    }

    // We only have a path formula, as we do not use abstractions?
    // Why is the path formula just 'true'?
    BooleanFormula program =
        AbstractStates.extractStateByType(target, PredicateAbstractState.class)
            .getPathFormula()
            .getFormula();

    return status;
  }

  private Optional<AbstractState> getTarget(final ReachedSet reachedSet) {
    return FluentIterable.from(reachedSet).filter(AbstractStates::isTargetState).first();
  }
}
