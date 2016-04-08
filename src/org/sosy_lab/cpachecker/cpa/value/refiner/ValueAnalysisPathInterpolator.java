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
import java.util.Set;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssignmentsInPathCondition.UniqueAssignmentsInPathConditionState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.UseDefBasedInterpolator;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisEdgeInterpolator;
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
import org.sosy_lab.cpachecker.util.refinement.UseDefRelation;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.value.refinement")
public class ValueAnalysisPathInterpolator
    extends GenericPathInterpolator<ValueAnalysisState, ValueAnalysisInterpolant> {

  @Option(secure=true, description="whether to perform (more precise) edge-based interpolation or (more efficient) path-based interpolation")
  private boolean performEdgeBasedInterpolation = true;

  /**
   * whether or not to do lazy-abstraction, i.e., when true, the re-starting node
   * for the re-exploration of the ARG will be the node closest to the root
   * where new information is made available through the current refinement
   */
  @Option(secure=true, description="whether or not to do lazy-abstraction")
  private boolean doLazyAbstraction = true;

  /**
   * a reference to the assignment-counting state, to make the precision increment aware of thresholds
   */
  private UniqueAssignmentsInPathConditionState assignments = null;

  private final CFA cfa;

  private final ValueAnalysisInterpolantManager interpolantManager;

  public ValueAnalysisPathInterpolator(
      final FeasibilityChecker<ValueAnalysisState> pFeasibilityChecker,
      final StrongestPostOperator<ValueAnalysisState> pStrongestPostOperator,
      final GenericPrefixProvider<ValueAnalysisState> pPrefixProvider,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa)
      throws InvalidConfigurationException {

    super(new ValueAnalysisEdgeInterpolator(pFeasibilityChecker,
            pStrongestPostOperator,
            pConfig,
            pShutdownNotifier,
            pCfa),
        pFeasibilityChecker,
        pPrefixProvider,
        ValueAnalysisInterpolantManager.getInstance(),
        pConfig,
        pLogger,
        pShutdownNotifier,
        pCfa);

    pConfig.inject(this);
    cfa = pCfa;
    interpolantManager = ValueAnalysisInterpolantManager.getInstance();
  }

  @Override
  public Map<ARGState, ValueAnalysisInterpolant> performInterpolation(
      final ARGPath errorPath,
      final ValueAnalysisInterpolant interpolant
  ) throws CPAException, InterruptedException {

    if (performEdgeBasedInterpolation) {
      return super.performInterpolation(errorPath, interpolant);

    } else {
      totalInterpolations.inc();

      ARGPath errorPathPrefix = performRefinementSelection(errorPath, interpolant);

      timerInterpolation.start();

      Map<ARGState, ValueAnalysisInterpolant> interpolants =
          performPathBasedInterpolation(errorPathPrefix);

      timerInterpolation.stop();

      propagateFalseInterpolant(errorPath, errorPathPrefix, interpolants);

      return interpolants;
    }
  }

  /**
   * This method performs interpolation on the complete path, based on the
   * use-def-relation. It creates fake interpolants that are not inductive.
   *
   * @param errorPathPrefix the error path prefix to interpolate
   */
  private Map<ARGState, ValueAnalysisInterpolant> performPathBasedInterpolation(ARGPath errorPathPrefix) {

    Set<String> booleanVariables = cfa.getVarClassification().isPresent()
      ? cfa.getVarClassification().get().getIntBoolVars()
      : Collections.<String>emptySet();

    UseDefRelation useDefRelation = new UseDefRelation(errorPathPrefix,
        booleanVariables,
        !isRefinementSelectionEnabled());

    Map<ARGState, ValueAnalysisInterpolant> interpolants = new UseDefBasedInterpolator(
        errorPathPrefix,
        useDefRelation,
        cfa.getMachineModel()).obtainInterpolantsAsMap();

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

  public Multimap<CFANode, MemoryLocation> determinePrecisionIncrement(ARGPath errorPath)
      throws CPAException, InterruptedException {

    assignments = AbstractStates.extractStateByType(errorPath.getLastState(),
                                                    UniqueAssignmentsInPathConditionState.class);

    Multimap<CFANode, MemoryLocation> increment = HashMultimap.create();

    Map<ARGState, ValueAnalysisInterpolant> itps =
        performInterpolation(errorPath, interpolantManager.createInitialInterpolant());

    for (Map.Entry<ARGState, ValueAnalysisInterpolant> itp : itps.entrySet()) {
      addToPrecisionIncrement(increment, AbstractStates.extractLocation(itp.getKey()),
          itp.getValue());
    }

    return increment;
  }

  /**
   * This method adds the given variable at the given location to the increment.
   *
   * @param increment the current increment
   * @param currentNode the current node for which to add a new variable
   * @param itp the interpolant to add to the precision increment
   */
  private void addToPrecisionIncrement(
      final Multimap<CFANode, MemoryLocation> increment,
      final CFANode currentNode,
      final ValueAnalysisInterpolant itp
  ) {

    for (MemoryLocation memoryLocation : itp.getMemoryLocations()) {
      if (assignments == null || !assignments.exceedsThreshold(memoryLocation)) {
        increment.put(currentNode, memoryLocation);
      }
    }
  }

  /**
   * This method determines the new refinement root.
   *
   * @param errorPath the error path from where to determine the refinement root
   * @param increment the current precision increment
   * @param isRepeatedRefinement the flag to determine whether or not this is a repeated refinement
   * @return the new refinement root
   * @throws RefinementFailedException if no refinement root can be determined
   */
  public Pair<ARGState, CFAEdge> determineRefinementRoot(
      ARGPath errorPath,
      Multimap<CFANode, MemoryLocation> increment,
      boolean isRepeatedRefinement
  ) throws RefinementFailedException {

    if (interpolationOffset == -1) {
      throw new RefinementFailedException(Reason.InterpolationFailed, errorPath);
    }

    // if doing lazy abstraction, use the node closest to the root node where new information is present
    if (doLazyAbstraction) {
      PathIterator it = errorPath.pathIterator();
      for (int i = 0; i < interpolationOffset; i++) {
        it.advance();
      }
      return Pair.of(it.getAbstractState(), it.getIncomingEdge());
    }

    // otherwise, just use the successor of the root node
    else {
      PathIterator firstElem = errorPath.pathIterator();
      firstElem.advance();
      return Pair.of(firstElem.getAbstractState(), firstElem.getOutgoingEdge());
    }
  }
}
