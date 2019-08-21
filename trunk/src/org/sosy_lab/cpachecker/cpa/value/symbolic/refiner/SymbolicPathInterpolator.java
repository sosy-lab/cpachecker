/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
 * {@link PathInterpolator} for
 * {@link org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA ConstraintsCPA}.
 * Allows creation of {@link SymbolicInterpolant SymbolicInterpolants}.
 */
@Options(prefix="cpa.value.symbolic.refiner")
public class SymbolicPathInterpolator
    extends GenericPathInterpolator<ForgettingCompositeState, SymbolicInterpolant> {

  public SymbolicPathInterpolator(
      final SymbolicEdgeInterpolator pEdgeInterpolator,
      final FeasibilityChecker<ForgettingCompositeState> pFeasibilityChecker,
      final GenericPrefixProvider<ForgettingCompositeState> pPrefixProvider,
      final Configuration pConfig, LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, CFA pCfa
  ) throws InvalidConfigurationException {

    super(pEdgeInterpolator,
        pFeasibilityChecker,
        pPrefixProvider,
        SymbolicInterpolantManager.getInstance(),
        pConfig,
        pLogger, pShutdownNotifier, pCfa);

    pConfig.inject(this);
  }
}
