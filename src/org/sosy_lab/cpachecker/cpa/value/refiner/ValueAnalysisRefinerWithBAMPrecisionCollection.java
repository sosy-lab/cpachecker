/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import com.google.common.collect.Sets;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMReachedSet;
import org.sosy_lab.cpachecker.cpa.bam.BAMUnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.refinement.GenericPrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.PathExtractor;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

public class ValueAnalysisRefinerWithBAMPrecisionCollection extends ValueAnalysisRefiner {

  ValueAnalysisRefinerWithBAMPrecisionCollection(ARGCPA pArgCPA,
      ValueAnalysisFeasibilityChecker pFeasibilityChecker,
      StrongestPostOperator<ValueAnalysisState> pStrongestPostOperator,
      PathExtractor pPathExtractor, GenericPrefixProvider<ValueAnalysisState> pPrefixProvider,
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCfa)
      throws InvalidConfigurationException {
    super(pArgCPA, pFeasibilityChecker, pStrongestPostOperator, pPathExtractor, pPrefixProvider,
        pConfig, pLogger, pShutdownNotifier, pCfa);
  }

  @Override
  protected PredicatePrecision mergePredicatePrecisionsForSubgraph(
      final ARGState pRefinementRoot, final ARGReachedSet pReached) {

    assert pReached instanceof BAMReachedSet;

    Precision result = ((BAMUnmodifiableReachedSet)pReached.asReachedSet()).getPrecisionForSubgraph(pRefinementRoot,
        (x, y) -> PredicatePrecision.unionOf(Sets.newHashSet(x, y)),
        s -> Precisions.extractPrecisionByType(s, PredicatePrecision.class));

    assert result instanceof PredicatePrecision;
    return (PredicatePrecision) result;
  }

  @Override
  protected VariableTrackingPrecision mergeValuePrecisionsForSubgraph(
      final ARGState pRefinementRoot,
      final ARGReachedSet pReached) {
    assert pReached instanceof BAMReachedSet;

    Precision result = ((BAMUnmodifiableReachedSet)pReached.asReachedSet()).getPrecisionForSubgraph(pRefinementRoot,
        (x, y) -> ((VariableTrackingPrecision)x).join((VariableTrackingPrecision)y),
        s -> Precisions.extractPrecisionByType(s, VariableTrackingPrecision.class));

    assert result instanceof VariableTrackingPrecision;
    return (VariableTrackingPrecision) result;
  }
}
