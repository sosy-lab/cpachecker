/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.interval.Refiner;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Map;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssignmentsInPathCondition.UniqueAssignmentsInPathConditionState;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisInterpolantManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.refinement.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.GenericPathInterpolator;
import org.sosy_lab.cpachecker.util.refinement.GenericPrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

@Options(prefix = "cpa.interval.refinement")
public class IntervalAnalysisPathInterpolator
    extends GenericPathInterpolator<IntervalAnalysisState, IntervalAnalysisInterpolant> {

  /**
   * a reference to the assignment-counting state, to make the precision increment aware of
   * thresholds
   */
  private UniqueAssignmentsInPathConditionState assignments = null;

  private final CFA cfa;

  private final IntervalAnalysisInterpolantManager interpolantManager;

  public IntervalAnalysisPathInterpolator(
      final FeasibilityChecker<IntervalAnalysisState> pFeasibilityChecker,
      final StrongestPostOperator<IntervalAnalysisState> pStrongestPostOperator,
      final GenericPrefixProvider<IntervalAnalysisState> pPrefixProvider,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa,
      IntervalAnalysisState initialState)
      throws InvalidConfigurationException {

    super(
        new IntervalAnalysisEdgeInterpolator(
            pStrongestPostOperator,
            pFeasibilityChecker,
            IntervalAnalysisInterpolantManager.getInstance(),
            initialState,
            IntervalAnalysisCPA.class,
            pConfig,
            pShutdownNotifier,
            pCfa),
        pFeasibilityChecker,
        pPrefixProvider,
        IntervalAnalysisInterpolantManager.getInstance(),
        pConfig,
        pLogger,
        pShutdownNotifier,
        pCfa);

    pConfig.inject(this);
    cfa = pCfa;
    interpolantManager = IntervalAnalysisInterpolantManager.getInstance();
  }

  @Override
  public Map<ARGState, IntervalAnalysisInterpolant> performInterpolation(
      final ARGPath errorPath, final IntervalAnalysisInterpolant interpolant)
      throws CPAException, InterruptedException {
    return super.performInterpolation(errorPath, interpolant);
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  public Map<ARGState, IntervalAnalysisInterpolant> determinePrecisionIncrement(ARGPath errorPath)
      throws CPAException, InterruptedException {

    assignments =
        AbstractStates.extractStateByType(
            errorPath.getLastState(), UniqueAssignmentsInPathConditionState.class);

    Map<ARGState, IntervalAnalysisInterpolant> itps =
        performInterpolation(errorPath, interpolantManager.createInitialInterpolant());

    return itps;
  }


  /**
   * This method determines the new refinement root.
   *
   * @param errorPath the error path from where to determine the refinement root
   * @param increment the current precision increment
   * @return the new refinement root
   * @throws RefinementFailedException if no refinement root can be determined
   */
  public Pair<ARGState, CFAEdge> determineRefinementRoot(
      ARGPath errorPath) throws RefinementFailedException {

    if (interpolationOffset == -1) {
      throw new RefinementFailedException(Reason.InterpolationFailed, errorPath);
    }

    // if doing lazy abstraction, use the node closest to the root node where new information is
    // present
    PathIterator it = errorPath.pathIterator();
    for (int i = 0; i < interpolationOffset; i++) {
      it.advance();
    }
    return Pair.of(it.getAbstractState(), it.getIncomingEdge());
  }
}
