// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.induction.refiner;

import java.util.List;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Makes the CEGAR loop behave like BMC with an incremented bound: the precision describes no
 * abstraction, so the exploration unrolls the loops k times and cuts the paths off at the next
 * visit of a loop head. The refiner runs the queries of native BMC, first for the targets and then
 * the bounding assertions for the cut-offs. If a cut-off is reachable, k increases by one at every
 * cut-off.
 */
public final class BmcRefiner extends InductionRefiner {

  private BmcRefiner(ConfigurableProgramAnalysis pCpa, LogManager pLogger, Configuration pConfig)
      throws InvalidConfigurationException {
    super(pCpa, pLogger, pConfig);
  }

  public static Refiner create(ConfigurableProgramAnalysis pCpa, LogManager pLogger)
      throws InvalidConfigurationException {
    return new BmcRefiner(pCpa, pLogger, configurationOf(pCpa, BmcRefiner.class));
  }

  @Override
  protected Optional<ARGState> refine(
      ARGReachedSet pReached, List<ARGState> pTargets, List<ARGState> pCutOffs)
      throws CPAException, InterruptedException, SolverException {
    return refineAsBmc(pReached, pTargets, pCutOffs);
  }
}
