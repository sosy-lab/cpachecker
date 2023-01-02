// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.refiner;

import static org.sosy_lab.cpachecker.util.Precisions.extractPrecisionByType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.path.PathPosition;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionBlock;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGStateInformation;
import org.sosy_lab.cpachecker.cpa.smg.UnmodifiableSMGState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class SMGEdgeInterpolator {

  /** the postOperator relation in use */
  private final SMGStrongestPostOperator postOperator;

  /** the precision for the Feasability check */
  private final SMGPrecision strongPrecision;

  private final SMGInterpolantManager interpolantManager;

  /** the error path checker to be used for feasibility checks */
  private final SMGFeasibilityChecker checker;

  private int numberOfInterpolationQueries = 0;

  //  private final SMGState initialState;

  private final SMGEdgeHeapAbstractionInterpolator heapAbstractionInterpolator;

  private final ShutdownNotifier shutdownNotifier;
  private final BlockOperator blockOperator;

  public SMGEdgeInterpolator(
      SMGFeasibilityChecker pFeasibilityChecker,
      SMGStrongestPostOperator pStrongestPostOperator,
      SMGInterpolantManager pInterpolantManager,
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogger,
      BlockOperator pBlockOperator) {

    checker = pFeasibilityChecker;
    postOperator = pStrongestPostOperator;
    interpolantManager = pInterpolantManager;

    strongPrecision = SMGPrecision.createStaticPrecision(false);
    shutdownNotifier = pShutdownNotifier;
    blockOperator = pBlockOperator;
    heapAbstractionInterpolator =
        new SMGEdgeHeapAbstractionInterpolator(pLogger, pFeasibilityChecker, pBlockOperator);
  }

  public List<SMGInterpolant> deriveInterpolant(
      CFAEdge pCurrentEdge,
      PathPosition pOffset,
      SMGInterpolant pInputInterpolant,
      boolean pAllTargets,
      ARGReachedSet pReached,
      ARGState pSuccessorARGstate)
      throws CPAException, InterruptedException {
    numberOfInterpolationQueries = 0;

    // create initial state, based on input interpolant, and create initial successor by consuming
    // the next edge
    CFAEdge currentEdge = pCurrentEdge;
    Set<SMGState> initialStates = pInputInterpolant.reconstructState();

    if (currentEdge == null) {
      PathIterator it = pOffset.fullPathIterator();
      Collection<SMGState> intermediate = initialStates;

      while (intermediate.isEmpty() || !it.isPositionWithState()) {

        Collection<SMGState> newIntermediate = new ArrayList<>();
        for (SMGState state : intermediate) {
          newIntermediate.addAll(getInitialSuccessor(state, it.getOutgoingEdge()));
        }

        intermediate = newIntermediate;
        it.advance();
      }
      initialStates = new HashSet<>(intermediate);
      currentEdge = it.getOutgoingEdge();

      if (initialStates.isEmpty()) {
        return Collections.singletonList(interpolantManager.getFalseInterpolant());
      }
    }

    Set<SMGState> successors = new HashSet<>();
    for (SMGState state : initialStates) {
      successors.addAll(getInitialSuccessor(state, currentEdge));
    }

    if (successors.isEmpty()) {
      return Collections.singletonList(interpolantManager.getFalseInterpolant());
    }

    // if initial state and successor are equal, return the input interpolant
    // in general, this returned interpolant might be stronger than needed, but only in very rare
    // cases, the weaker interpolant would be different from the input interpolant, so we spare the
    // effort
    // Implementation detail: we compare 'Sets' for equality!
    boolean noChange = initialStates.equals(successors) || isFunctionOrTypeDeclaration(currentEdge);
    SMGPrecision currentPrecision =
        extractPrecisionByType(
            pReached.asReachedSet().getPrecision(pSuccessorARGstate), SMGPrecision.class);
    if (noChange
        && !currentPrecision.allowsHeapAbstractionOnNode(
            currentEdge.getPredecessor(), blockOperator)) {
      return Collections.singletonList(pInputInterpolant);
    }

    // if the current edge just changes the names of variables
    // (e.g. function arguments, returned variables)
    // then return the input interpolant with those renamings
    boolean onlySuccessor = successors.size() == 1;
    if (onlySuccessor
        && isOnlyVariableRenamingEdge(pCurrentEdge)
        && !currentPrecision.allowsHeapAbstractionOnNode(
            currentEdge.getPredecessor(), blockOperator)) {
      return Collections.singletonList(interpolantManager.createInterpolant(successors));
    }

    ImmutableList.Builder<SMGInterpolant> resultingInterpolants =
        ImmutableList.builderWithExpectedSize(successors.size());
    ARGPath remainingErrorPath = pOffset.iterator().getSuffixExclusive();

    for (SMGState state : successors) {

      if (currentPrecision.getAbstractionOptions().allowsStackAbstraction()) {
        interpolateStackVariables(state, remainingErrorPath, currentEdge, pAllTargets);
      }

      if (currentPrecision.getAbstractionOptions().allowsFieldAbstraction()) {
        interpolateFields(state, remainingErrorPath, currentEdge, pAllTargets);
      }

      Set<SMGAbstractionBlock> abstractionBlocks = ImmutableSet.of();
      if (currentPrecision.getAbstractionOptions().allowsHeapAbstraction()) {
        SMGState originalState = state.copyOf();
        SMGHeapAbstractionInterpoaltionResult heapInterpoaltionResult =
            interpolateHeapAbstraction(
                state,
                remainingErrorPath,
                currentEdge.getPredecessor(),
                currentEdge,
                pAllTargets,
                currentPrecision);

        if (heapInterpoaltionResult.isChanged()) {
          resultingInterpolants.add(
              interpolantManager.createInterpolant(Collections.singleton(originalState)));
          abstractionBlocks = heapInterpoaltionResult.getBlocks();
        }
      }

      SMGInterpolant result = interpolantManager.createInterpolant(state, abstractionBlocks);
      resultingInterpolants.add(result);
    }

    return resultingInterpolants.build();
  }

  private boolean isFunctionOrTypeDeclaration(CFAEdge pCurrentEdge) {

    if (pCurrentEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
      CDeclarationEdge dclEdge = (CDeclarationEdge) pCurrentEdge;
      CDeclaration dcl = dclEdge.getDeclaration();
      return dcl instanceof CFunctionDeclaration || dcl instanceof CTypeDeclaration;
    }

    return false;
  }

  private UnmodifiableSMGState interpolateStackVariables(
      SMGState pState, ARGPath pRemainingErrorPath, CFAEdge currentEdge, boolean pAllTargets)
      throws CPAException, InterruptedException {

    SMGState state = pState;

    for (Entry<MemoryLocation, SMGRegion> memoryLocationEntry :
        state.getStackVariables().entrySet()) {
      shutdownNotifier.shutdownIfNecessary();
      MemoryLocation memoryLocation = memoryLocationEntry.getKey();
      SMGRegion region = memoryLocationEntry.getValue();

      // temporarily remove the hve edge of the current memory path from the candidate
      // interpolant
      SMGStateInformation info = state.forgetStackVariable(memoryLocation);

      if (isRemainingPathFeasible(pRemainingErrorPath, state, currentEdge, pAllTargets)) {
        state.remember(memoryLocation, region, info);
      }
    }

    return state;
  }

  private UnmodifiableSMGState interpolateFields(
      SMGState pState, ARGPath pRemainingErrorPath, CFAEdge currentEdge, boolean pAllTargets)
      throws CPAException, InterruptedException {

    SMGState state = pState;

    for (SMGEdgeHasValue currentHveEdge : state.getHeap().getHVEdges()) {
      shutdownNotifier.shutdownIfNecessary();

      // TODO Robust heap abstracion?
      if (currentHveEdge.getObject().isAbstract()) {
        continue;
      }

      // temporarily remove the hve edge of the current memory path from the candidate
      // interpolant
      state.forget(currentHveEdge);

      if (isRemainingPathFeasible(pRemainingErrorPath, state, currentEdge, pAllTargets)) {
        state.remember(currentHveEdge);
      }
    }

    return state;
  }

  private SMGHeapAbstractionInterpoaltionResult interpolateHeapAbstraction(
      SMGState pInitialSuccessor,
      ARGPath pRemainingErrorPath,
      CFANode pStateLocation,
      CFAEdge pCurrentEdge,
      boolean pAllTargets,
      SMGPrecision pCurrentPrecision)
      throws CPAException, InterruptedException {

    return heapAbstractionInterpolator.calculateHeapAbstractionBlocks(
        pInitialSuccessor,
        pRemainingErrorPath,
        pCurrentPrecision,
        pStateLocation,
        pCurrentEdge,
        pAllTargets);
  }

  private Collection<SMGState> getInitialSuccessor(SMGState pState, CFAEdge pCurrentEdge)
      throws CPAException, InterruptedException {
    return getInitialSuccessor(pState, pCurrentEdge, strongPrecision);
  }

  /**
   * This method gets the initial successor, i.e. the state following the initial state.
   *
   * @param pInitialState the initial state, i.e. the state represented by the input interpolant.
   * @param pInitialEdge the initial edge of the error path
   * @return the initial successor
   */
  private Collection<SMGState> getInitialSuccessor(
      final SMGState pInitialState, final CFAEdge pInitialEdge, final SMGPrecision precision)
      throws CPAException, InterruptedException {

    SMGState oldState = pInitialState;

    return postOperator.getStrongestPost(Collections.singleton(oldState), precision, pInitialEdge);
  }

  /**
   * This method checks, if the given edge is only renaming variables.
   *
   * @param cfaEdge the CFA edge to check
   * @return true, if the given edge is only renaming variables
   */
  private boolean isOnlyVariableRenamingEdge(CFAEdge cfaEdge) {
    return
    // if the edge is null this is a dynamic multi edge
    cfaEdge != null

        // renames from calledFn::___cpa_temp_result_var_ to callerFN::assignedVar
        // if the former is relevant, so is the latter
        && cfaEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge;
  }

  /**
   * This method checks, whether or not the (remaining) error path is feasible when starting with
   * the given (pseudo) initial state.
   *
   * @param remainingErrorPath the error path to check feasibility on
   * @param state the (pseudo) initial state
   * @param pCurrentEdge if the remaining error path has only 1 state and no edges, the edge leading
   *     to the state is necessary to check if it is a target of an automaton, and therefore
   *     feasible.
   * @param pAllTargets should we check for all possible errors, or only the error specified in the
   *     Target State
   * @return true, it the path is feasible, else false
   */
  public boolean isRemainingPathFeasible(
      ARGPath remainingErrorPath,
      UnmodifiableSMGState state,
      CFAEdge pCurrentEdge,
      boolean pAllTargets)
      throws CPAException, InterruptedException {
    numberOfInterpolationQueries++;
    return checker.isRemainingPathFeasible(remainingErrorPath, state, pCurrentEdge, pAllTargets);
  }

  public int getNumberOfInterpolationQueries() {
    return numberOfInterpolationQueries;
  }

  public static class SMGHeapAbstractionInterpoaltionResult {

    private static final SMGHeapAbstractionInterpoaltionResult EMPTY_AND_UNCHANGED =
        new SMGHeapAbstractionInterpoaltionResult(ImmutableSet.of(), false);
    private final Set<SMGAbstractionBlock> blocks;
    private final boolean change;

    public SMGHeapAbstractionInterpoaltionResult(
        Set<SMGAbstractionBlock> pBlocks, boolean pChange) {
      blocks = pBlocks;
      change = pChange;
    }

    public Set<SMGAbstractionBlock> getBlocks() {
      return blocks;
    }

    public boolean isChanged() {
      return change;
    }

    @Override
    public String toString() {
      return "SMGHeapAbstractionInterpoaltionResult [blocks=" + blocks + ", change=" + change + "]";
    }

    public static SMGHeapAbstractionInterpoaltionResult emptyAndUnchanged() {
      return EMPTY_AND_UNCHANGED;
    }
  }
}
