// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.refiner;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssignmentsInPathCondition.UniqueAssignmentsInPathConditionState;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.refinement.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.GenericPathInterpolator;
import org.sosy_lab.cpachecker.util.refinement.GenericPrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;
import org.sosy_lab.cpachecker.util.refinement.UseDefRelation;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

@Options(prefix = "cpa.smg2.refinement")
public class SMGPathInterpolator extends GenericPathInterpolator<SMGState, SMGInterpolant> {

  @Option(
      secure = true,
      description =
          "whether to perform (more precise) edge-based interpolation or (more efficient)"
              + " path-based interpolation")
  private boolean performEdgeBasedInterpolation = true;

  /**
   * whether or not to do lazy-abstraction, i.e., when true, the re-starting node for the
   * re-exploration of the ARG will be the node closest to the root where new information is made
   * available through the current refinement
   */
  @Option(secure = true, description = "whether or not to do lazy-abstraction")
  private boolean doLazyAbstraction = true;

  /**
   * a reference to the assignment-counting state, to make the precision increment aware of
   * thresholds
   */
  private UniqueAssignmentsInPathConditionState assignments = null;

  private final CFA cfa;

  private final SMGInterpolantManager interpolantManager;

  private final Configuration config;
  private final LogManager logger;

  public SMGPathInterpolator(
      final FeasibilityChecker<SMGState> pFeasibilityChecker,
      final StrongestPostOperator<SMGState> pStrongestPostOperator,
      final GenericPrefixProvider<SMGState> pPrefixProvider,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa)
      throws InvalidConfigurationException {

    super(
        new SMGEdgeInterpolator(
            pFeasibilityChecker, pStrongestPostOperator, pConfig, pShutdownNotifier, pCfa, pLogger),
        pFeasibilityChecker,
        pPrefixProvider,
        SMGInterpolantManager.getInstance(
            new SMGOptions(pConfig), pCfa.getMachineModel(), pLogger, pCfa),
        pConfig,
        pLogger,
        pShutdownNotifier,
        pCfa);

    pConfig.inject(this);
    cfa = pCfa;
    interpolantManager =
        SMGInterpolantManager.getInstance(
            new SMGOptions(pConfig), pCfa.getMachineModel(), pLogger, pCfa);
    config = pConfig;
    logger = pLogger;
  }

  @Override
  public Map<ARGState, SMGInterpolant> performInterpolation(
      final ARGPath errorPath, final SMGInterpolant interpolant)
      throws CPAException, InterruptedException {

    if (performEdgeBasedInterpolation) {
      return super.performInterpolation(errorPath, interpolant);

    } else {
      totalInterpolations.inc();

      ARGPath errorPathPrefix = performRefinementSelection(errorPath, interpolant);

      timerInterpolation.start();

      Map<ARGState, SMGInterpolant> interpolants = performPathBasedInterpolation(errorPathPrefix);

      timerInterpolation.stop();

      propagateFalseInterpolant(errorPath, errorPathPrefix, interpolants);

      return interpolants;
    }
  }

  /**
   * This method performs interpolation on the complete path, based on the use-def-relation. It
   * creates fake interpolants that are not inductive.
   *
   * @param errorPathPrefix the error path prefix to interpolate
   */
  private Map<ARGState, SMGInterpolant> performPathBasedInterpolation(ARGPath errorPathPrefix) {

    Set<String> booleanVariables =
        cfa.getVarClassification().isPresent()
            ? cfa.getVarClassification().orElseThrow().getIntBoolVars()
            : ImmutableSet.of();

    UseDefRelation useDefRelation =
        new UseDefRelation(errorPathPrefix, booleanVariables, !isRefinementSelectionEnabled());

    Map<ARGState, SMGInterpolant> interpolants =
        new SMGUseDefBasedInterpolator(
                errorPathPrefix, useDefRelation, cfa.getMachineModel(), config, logger, cfa)
            .obtainInterpolantsAsMap();

    totalInterpolationQueries.setNextValue(1);

    int size = 0;
    for (SMGInterpolant itp : interpolants.values()) {
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

    assignments =
        AbstractStates.extractStateByType(
            errorPath.getLastState(), UniqueAssignmentsInPathConditionState.class);

    Multimap<CFANode, MemoryLocation> increment = HashMultimap.create();

    Map<ARGState, SMGInterpolant> itps =
        performInterpolation(errorPath, interpolantManager.createInitialInterpolant());

    for (Map.Entry<ARGState, SMGInterpolant> itp : itps.entrySet()) {
      addToPrecisionIncrement(
          increment, AbstractStates.extractLocation(itp.getKey()), itp.getValue());
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
      final SMGInterpolant itp) {

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
   * @return the new refinement root
   * @throws RefinementFailedException if no refinement root can be determined
   */
  public Pair<ARGState, CFAEdge> determineRefinementRoot(
      ARGPath errorPath, Multimap<CFANode, MemoryLocation> increment)
      throws RefinementFailedException {

    if (interpolationOffset == -1) {
      throw new RefinementFailedException(Reason.InterpolationFailed, errorPath);
    }

    // if doing lazy abstraction, use the node closest to the root node where new information is
    // present
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

  /**
   * This method returns a sliced error path (prefix). In case the sliced error path becomes
   * feasible, i.e., because slicing is not fully precise in presence of, e.g., structs or arrays,
   * the original error path (prefix) that was given as input is returned.
   */
  @Override
  protected ARGPath sliceErrorPath(final ARGPath pErrorPathPrefix)
      throws CPAException, InterruptedException {

    if (!isPathSlicingPossible(pErrorPathPrefix)) {
      return pErrorPathPrefix;
    }

    Set<ARGState> useDefStates =
        new UseDefRelation(
                pErrorPathPrefix,
                cfa.getVarClassification().isPresent()
                    ? cfa.getVarClassification().orElseThrow().getIntBoolVars()
                    : ImmutableSet.of(),
                false)
            .getUseDefStates();

    ArrayDeque<Triple<FunctionCallEdge, Boolean, Integer>> functionCalls = new ArrayDeque<>();
    List<CFAEdge> abstractEdges = new ArrayList<>(pErrorPathPrefix.getInnerEdges());

    PathIterator iterator = pErrorPathPrefix.pathIterator();
    while (iterator.hasNext()) {
      CFAEdge originalEdge = iterator.getOutgoingEdge();

      // slice edge if there is neither a use nor a definition at the current state
      if (!useDefStates.contains(iterator.getAbstractState())) {
        CFANode startNode;
        CFANode endNode;
        if (originalEdge == null) {
          startNode = AbstractStates.extractLocation(iterator.getAbstractState());
          endNode = AbstractStates.extractLocation(iterator.getNextAbstractState());
        } else {
          startNode = originalEdge.getPredecessor();
          endNode = originalEdge.getSuccessor();
        }
        abstractEdges.set(
            iterator.getIndex(),
            new BlankEdge(
                originalEdge == null ? "" : originalEdge.getRawStatement(),
                originalEdge == null ? FileLocation.DUMMY : originalEdge.getFileLocation(),
                startNode,
                endNode,
                "sliced edge"));
      }

      if (originalEdge != null) {
        CFAEdgeType typeOfOriginalEdge = originalEdge.getEdgeType();
        /** ********************************** */
        /** assure that call stack is valid * */
        /** ********************************** */
        // when entering into a function, remember if call is relevant or not
        if (typeOfOriginalEdge == CFAEdgeType.FunctionCallEdge) {
          boolean isAbstractEdgeFunctionCall =
              abstractEdges.get(iterator.getIndex()).getEdgeType() == CFAEdgeType.FunctionCallEdge;

          functionCalls.push(
              Triple.of(
                  (FunctionCallEdge) originalEdge,
                  isAbstractEdgeFunctionCall,
                  iterator.getIndex()));
        }

        // when returning from a function, ...
        if (typeOfOriginalEdge == CFAEdgeType.FunctionReturnEdge) {
          // The original call edge, importance in relation to slicing, position in abstractEdges
          Triple<FunctionCallEdge, Boolean, Integer> functionCallInfo = functionCalls.pop();
          // ... if call is relevant and return edge is now a blank edge, restore the original
          // return edge
          if (functionCallInfo.getSecond()
              && abstractEdges.get(iterator.getIndex()).getEdgeType() == CFAEdgeType.BlankEdge) {
            abstractEdges.set(iterator.getIndex(), originalEdge);
          }

          // ... if call is irrelevant and return edge is not sliced, restore the call edge
          else if (!functionCallInfo.getSecond()
              && abstractEdges.get(iterator.getIndex()).getEdgeType()
                  == CFAEdgeType.FunctionReturnEdge) {
            abstractEdges.set(functionCallInfo.getThird(), functionCallInfo.getFirst());
            for (int j = iterator.getIndex(); j >= 0; j--) {
              if (functionCallInfo.getFirst() == abstractEdges.get(j)) {
                abstractEdges.set(j, functionCallInfo.getFirst());
                break;
              }
            }
          }
        }
      }

      iterator.advance();
    }

    // SMGs NEED the correct function calls, we need to restore ALL function calls not yet restored
    // but that are relevant (not returned)
    for (Triple<FunctionCallEdge, Boolean, Integer> functionCallInfo : functionCalls) {
      abstractEdges.set(functionCallInfo.getThird(), functionCallInfo.getFirst());
    }

    ARGPath slicedErrorPathPrefix = new ARGPath(pErrorPathPrefix.asStatesList(), abstractEdges);

    return isFeasible(slicedErrorPathPrefix) ? pErrorPathPrefix : slicedErrorPathPrefix;
  }
}
