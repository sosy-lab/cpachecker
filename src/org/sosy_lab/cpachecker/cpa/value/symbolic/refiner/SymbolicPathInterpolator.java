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

import java.util.ArrayDeque;
import java.util.List;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisInformation;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.ForgettingCompositeState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.interpolant.SymbolicInterpolant;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.refiner.EdgeInterpolator;
import org.sosy_lab.cpachecker.util.refiner.ErrorPathClassifier;
import org.sosy_lab.cpachecker.util.refiner.ErrorPathClassifier.PrefixPreference;
import org.sosy_lab.cpachecker.util.refiner.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.refiner.GenericPathInterpolator;
import org.sosy_lab.cpachecker.util.refiner.InterpolantManager;
import org.sosy_lab.cpachecker.util.refiner.PathInterpolator;

/**
 * {@link PathInterpolator} for
 * {@link org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA ConstraintsCPA}.
 * Allows creation of {@link SymbolicInterpolant SymbolicInterpolants}.
 */
@Options(prefix="cpa.value.symbolic.refiner")
public class SymbolicPathInterpolator
    extends GenericPathInterpolator<ForgettingCompositeState, SymbolicInterpolant> {

  private final FeasibilityChecker<ForgettingCompositeState> checker;
  private final ErrorPathClassifier classifier;

  @Option(description = "How to choose which prefix to use for interpolation")
  private PrefixPreference prefixPreference = PrefixPreference.FLOAT_AND_BITVECTOR_BEST;

  public SymbolicPathInterpolator(
      final EdgeInterpolator<ForgettingCompositeState, ValueAnalysisInformation, SymbolicInterpolant> pEdgeInterpolator,
      final InterpolantManager<ForgettingCompositeState, SymbolicInterpolant> pInterpolantManager,
      final FeasibilityChecker<ForgettingCompositeState> pFeasibilityChecker,
      final ErrorPathClassifier pPathClassifier,
      final Configuration pConfig, LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, CFA pCfa
  ) throws InvalidConfigurationException {

    super(pEdgeInterpolator, pInterpolantManager, pFeasibilityChecker, pPathClassifier, pConfig,
        pLogger, pShutdownNotifier, pCfa);

    pConfig.inject(this);

    checker = pFeasibilityChecker;
    classifier = pPathClassifier;
  }

  @Override
  protected ARGPath obtainErrorPathPrefix(
      final ARGPath pErrorPath,
      final SymbolicInterpolant pInterpolant
  ) throws CPAException {

    final List<ARGPath> prefixes =
        checker.getInfeasiblePrefixes(pErrorPath,
                                      pInterpolant.reconstructState(),
                                      new ArrayDeque<ForgettingCompositeState>());

    totalPrefixes.setNextValue(prefixes.size());

    return classifier.obtainSlicedPrefix(prefixPreference, pErrorPath, prefixes);
  }
}
