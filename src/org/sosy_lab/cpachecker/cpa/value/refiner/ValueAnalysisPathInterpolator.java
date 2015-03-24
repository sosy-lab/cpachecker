/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value.refiner;

import java.util.Collections;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ErrorPathClassifier.PrefixPreference;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.UseDefBasedInterpolator;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.UseDefRelation;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisEdgeInterpolator;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisInterpolantManager;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.refiner.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.refiner.GenericPathInterpolator;
import org.sosy_lab.cpachecker.util.refiner.StrongestPostOperator;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

@Options(prefix="cpa.value.refiner")
public class ValueAnalysisPathInterpolator
    extends GenericPathInterpolator<ValueAnalysisState, Pair<Value, Type>, ValueAnalysisInterpolant> {

  /**
   * whether or not to do lazy-abstraction, i.e., when true, the re-starting node
   * for the re-exploration of the ARG will be the node closest to the root
   * where new information is made available through the current refinement
   */
  @Option(secure=true, description="whether or not to do lazy-abstraction")
  private boolean doLazyAbstraction = true;

  @Option(secure=true, description="whether or not to perform path slicing before interpolation")
  private boolean pathSlicing = true;

  @Option(secure=true, description="whether to perform (more precise) edge-based interpolation or (more efficient) path-based interpolation")
  private boolean performEdgeBasedInterpolation = true;

  @Option(secure=true, description="which prefix of an actual counterexample trace should be used for interpolation")
  private PrefixPreference prefixPreference = PrefixPreference.DOMAIN_BEST_SHALLOW;

  // statistics
  private StatCounter totalInterpolations   = new StatCounter("Number of interpolations");
  private StatInt totalInterpolationQueries = new StatInt(StatKind.SUM, "Number of interpolation queries");
  private StatInt sizeOfInterpolant         = new StatInt(StatKind.AVG, "Size of interpolant");
  private StatTimer timerInterpolation      = new StatTimer("Time for interpolation");
  private StatInt totalPrefixes = new StatInt(StatKind.SUM, "Number of sliced prefixes");

  private final CFA cfa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;

  public ValueAnalysisPathInterpolator(
      final FeasibilityChecker<ValueAnalysisState> pFeasibilityChecker,
      final StrongestPostOperator<ValueAnalysisState> pStrongestPostOperator,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa)
      throws InvalidConfigurationException {

    super(new ValueAnalysisEdgeInterpolator(pFeasibilityChecker,
            pStrongestPostOperator,
            pConfig,
            pLogger,
            pShutdownNotifier,
            pCfa),
        ValueAnalysisInterpolantManager.getInstance(),
        pFeasibilityChecker,
        pConfig,
        pLogger,
        pShutdownNotifier,
        pCfa);

    pConfig.inject(this);
    config = pConfig;

    logger           = pLogger;
    cfa              = pCfa;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public Map<ARGState, ValueAnalysisInterpolant> performInterpolation(
      final ARGPath errorPath,
      final ValueAnalysisInterpolant interpolant
  ) throws CPAException {

    if (performEdgeBasedInterpolation) {
      return super.performInterpolation(errorPath, interpolant);

    } else {
      totalInterpolations.inc();
      timerInterpolation.start();

      ARGPath errorPathPrefix = obtainErrorPathPrefix(errorPath, interpolant);

      Map<ARGState, ValueAnalysisInterpolant> interpolants =
          performPathBasedInterpolation(errorPathPrefix);

      timerInterpolation.stop();

      return interpolants;
    }
  }

  /**
   * This method performs interpolation on the complete path, based on the
   * use-def-relation. It creates fake interpolants that are not inductive.
   *
   * @param errorPathPrefix the error path prefix to interpolate
   * @return
   */
  private Map<ARGState, ValueAnalysisInterpolant> performPathBasedInterpolation(ARGPath errorPathPrefix) {

    assert(prefixPreference != PrefixPreference.DEFAULT)
    : "static path-based interpolation requires a sliced infeasible prefix"
    + " - set cpa.value.refiner.prefixPreference, e.g. to " + PrefixPreference.DOMAIN_BEST_DEEP;

    Map<ARGState, ValueAnalysisInterpolant> interpolants = new UseDefBasedInterpolator(
        errorPathPrefix,
        new UseDefRelation(errorPathPrefix,
            cfa.getVarClassification().isPresent()
              ? cfa.getVarClassification().get().getIntBoolVars()
              : Collections.<String>emptySet(),
              "EQUALITY")).obtainInterpolants();

    totalInterpolationQueries.setNextValue(1);

    int size = 0;
    for(ValueAnalysisInterpolant itp : interpolants.values()) {
      size = size + itp.getSize();
    }
    sizeOfInterpolant.setNextValue(size);

    return interpolants;
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }
}
