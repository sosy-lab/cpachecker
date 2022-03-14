// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.interpolant.SymbolicInterpolant;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.interpolant.SymbolicInterpolantManager;
import org.sosy_lab.cpachecker.util.refinement.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.GenericPathInterpolator;
import org.sosy_lab.cpachecker.util.refinement.GenericPrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.PathInterpolator;

/**
 * {@link PathInterpolator} for {@link org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA
 * ConstraintsCPA}. Allows creation of {@link SymbolicInterpolant SymbolicInterpolants}.
 */
@Options(prefix = "cpa.value.symbolic.refiner")
public class SymbolicPathInterpolator
    extends GenericPathInterpolator<ForgettingCompositeState, SymbolicInterpolant> {

  public SymbolicPathInterpolator(
      final SymbolicEdgeInterpolator pEdgeInterpolator,
      final FeasibilityChecker<ForgettingCompositeState> pFeasibilityChecker,
      final GenericPrefixProvider<ForgettingCompositeState> pPrefixProvider,
      final Configuration pConfig,
      LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      CFA pCfa)
      throws InvalidConfigurationException {

    super(
        pEdgeInterpolator,
        pFeasibilityChecker,
        pPrefixProvider,
        SymbolicInterpolantManager.getInstance(),
        pConfig,
        pLogger,
        pShutdownNotifier,
        pCfa);

    pConfig.inject(this);
  }
}
