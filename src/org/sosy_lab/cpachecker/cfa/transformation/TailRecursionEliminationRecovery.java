// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;
import org.sosy_lab.cpachecker.util.AbstractStates;

public record TailRecursionEliminationRecovery(
    Map<CFANode, CFANode> nodeMap,
    int parameterNum,
    CFANode exitConditionCheckNode,
    List<CFANode> parameterNodes,
    CFAEdge varDeclarationEdge,
    CFAEdge varAssignmentEdge,
    CFAEdge varReturnEdge)
    implements ProgramTransformationRecovery {

  @Override
  public void revertProgramTransformation(
      AbstractState pPreviousState,
      SubCFA pAfterProgramTransformation,
      ReachedSet reached,
      LocationStateFactory pLocationStateFactory) {

    ARGState previousARGState = (ARGState) pPreviousState;
    ARGState currentARGState = previousARGState.getChildren().getFirst();
    TailRecursionState initialFunctionState = TailRecursionState.FUNCTION_START;

    if (pAfterProgramTransformation.subCFAEntryNode()
        == AbstractStates.extractLocation(currentARGState)) {
      // remove the initial ARGState in the program transformation
      currentARGState = ProgramTransformationRecoveryUtils.handleEntry(previousARGState, currentARGState, reached);
      // in tail recursion elimination we must have only one child state, i.e. a Function start dummy edge
      assert (currentARGState.getChildren().size() == 1) : "More than one child ARG state at the start of the function!";
      ARGState childARGState = currentARGState.getChildren().getFirst();
      // recover all ARGStates belonging to this program transformation
      recoverARG(
          currentARGState,
          childARGState,
          initialFunctionState,
          pAfterProgramTransformation.allNodes(),
          pAfterProgramTransformation.subCFAExitNode(),
          reached,
          pLocationStateFactory);
    } else {
      throw new RuntimeException("Invalid use of revertProgramTransformation()!");
    }
  }

  /**
   * Function for recursive depth-first recovery of the ARG and reached set.
   *
   * @param pPreviousARGState the previous ARGState
   * @param pCurrentARGState the current ARGState
   * @param pCurrentFunctionState the current position in the function
   * @param pNodes all nodes of the program transformation
   * @param reached the reached set
   * @param pLocationStateFactory for access to LocationStates
   */
  private void recoverARG(
      ARGState pPreviousARGState,
      ARGState pCurrentARGState,
      TailRecursionState pCurrentFunctionState,
      ImmutableSet<CFANode> pNodes,
      CFANode pExitNode,
      ReachedSet reached,
      LocationStateFactory pLocationStateFactory) {
    // check for the exit node
    if (AbstractStates.extractLocation(pCurrentARGState) == pExitNode) {
      ProgramTransformationRecoveryUtils.handleExit(pPreviousARGState,pCurrentARGState,reached);
      // handle recursive return calls
      CFANode nodeBeforeTMPVarReturn = varReturnEdge.getPredecessor();
      ARGState currentARGState = pPreviousARGState.getChildren().getFirst();
      ARGState stateAfterReturns = currentARGState.getChildren().getFirst();
      CompositeState currentCompState = (CompositeState) currentARGState.getWrappedState();
      CallstackState currentCallstackState = null;
      String currentFunctionCall = pExitNode.getFunctionName();
      String previousFunctionCall = "";
      while (true) {
        for (AbstractState state : currentCompState.getWrappedStates()) {
          if (state instanceof CallstackState callStackState) {
            currentCallstackState = callStackState;
            currentFunctionCall = currentCallstackState.getCurrentFunction();
            previousFunctionCall = currentCallstackState.getPreviousState().getCurrentFunction();
            break;
          }
        }
        if (currentFunctionCall.equals(previousFunctionCall)) {
          // recursion on callstack detected - add new states for the function return
          CallstackState newCallstackState = currentCallstackState.getPreviousState();
          ARGState newARGState1 =
              ProgramTransformationRecoveryUtils.argStateWithLocation(currentARGState, pLocationStateFactory.getState(nodeBeforeTMPVarReturn), currentARGState);
          newARGState1 = newARGState1.forkWithReplacements(Collections.singleton(newCallstackState));
          ARGState newARGState2 =
              ProgramTransformationRecoveryUtils.argStateWithLocation(newARGState1, pLocationStateFactory.getState(pExitNode), newARGState1);
          reached.add(newARGState1, reached.getPrecision(currentARGState));
          reached.add(newARGState2, reached.getPrecision(currentARGState));

          // continue
          currentARGState = newARGState2;
          currentCompState = (CompositeState) currentARGState.getWrappedState();
        } else {
          // no recursion on callstack detected - return to previous function
          stateAfterReturns.addParent(currentARGState);
          stateAfterReturns.removeParent(pPreviousARGState.getChildren().getFirst());
          break;
        }
      }
      return;
    }
    // handle the current state depending on position in the program transformation
    ARGState newARGState =
        switch (pCurrentFunctionState) {
          case PARAMETER_ASSIGNMENT ->
              // map parameter assignment loop to a recursive function call
              recoverParameterAssignments(
                  pPreviousARGState, pCurrentARGState, reached, pLocationStateFactory);
          default ->
              // handle current state normally
              revertSingleState(
                  pPreviousARGState, pCurrentARGState, reached, pLocationStateFactory);
        };
    // handle child states
    ImmutableList<ARGState> children = ImmutableList.copyOf(newARGState.getChildren());
    for (int i = 0; i < children.size(); i++) {
      ARGState child = children.get(i);
      if (pNodes.contains(AbstractStates.extractLocation(child))) {
        recoverARG(
            newARGState,
            child,
            setNewFunctionState(newARGState, child, pCurrentFunctionState),
            pNodes,
            pExitNode,
            reached,
            pLocationStateFactory);
      }
    }
  }

  /**
   * Recover a single state that can simply be mapped back to another location.
   * @param pPreviousARGState previous ARGState
   * @param pCurrentARGState current ARGState
   * @param reached reached set
   * @param pLocationStateFactory access to LocationStates
   * @return the new ARGState with updated LocationState
   */
  private ARGState revertSingleState(
      ARGState pPreviousARGState,
      ARGState pCurrentARGState,
      ReachedSet reached,
      LocationStateFactory pLocationStateFactory) {
    LocationState newLocationState =
        pLocationStateFactory.getState(
            nodeMap.get(AbstractStates.extractLocation(pCurrentARGState)));
    ARGState newARGState = ProgramTransformationRecoveryUtils.argStateWithLocation(pCurrentARGState, newLocationState, pPreviousARGState);
    if (reached.contains(pCurrentARGState)) {
      reached.add(newARGState, reached.getPrecision(pCurrentARGState));
      reached.remove(pCurrentARGState);
    }
    pCurrentARGState.replaceInARGWith(newARGState);
    return newARGState;
  }

  /**
   * Helper for dealing with the states before the function parameters get assigned their new
   * values. Replaces all ARGStates before parameter assignments with ARGStates from the recursive
   * function call with new LocationStates.
   *
   * @param pStateBeforeExitCondition  the previous ARGState at the exit condition check
   * @param pStateBeforeFirstParameter the current ARGState before the first parameter gets assigned
   * @param reached reached set
   * @param pLocationStateFactory      Factory for access to LocationStates
   * @return ARGState before the new exit condition check
   */
  private ARGState recoverParameterAssignments(
      ARGState pStateBeforeExitCondition,
      ARGState pStateBeforeFirstParameter,
      ReachedSet reached,
      LocationStateFactory pLocationStateFactory) {
    // mark all states before parameter assignments, except the first, for deletion
    ARGState currentARGState = pStateBeforeFirstParameter;
    ImmutableList.Builder<ARGState> statesToBeRemoved = new ImmutableList.Builder<>();
    // after this loop currentARGState is before the exit condition check
    // this loop corresponds to one recursive function call
    while (AbstractStates.extractLocation(currentARGState) != exitConditionCheckNode) {
      if (currentARGState != pStateBeforeFirstParameter) {
        statesToBeRemoved.add(currentARGState);
      }
      currentARGState = currentARGState.getChildren().getFirst();
    }
    // create states for the recursive function call
    ARGState argStateBeforeTMPVarDeclaration =
        ProgramTransformationRecoveryUtils.argStateWithLocation(
            pStateBeforeFirstParameter,
            pLocationStateFactory.getState(varDeclarationEdge.getPredecessor()),
            pStateBeforeExitCondition);
    ARGState argStateAfterTMPVarDeclaration =
        ProgramTransformationRecoveryUtils.argStateWithLocation(
            pStateBeforeFirstParameter,
            pLocationStateFactory.getState(varDeclarationEdge.getSuccessor()),
            argStateBeforeTMPVarDeclaration);
    ARGState argStateAfterRecursiveFunctionCall =
        ProgramTransformationRecoveryUtils.argStateWithLocation(
            currentARGState,
            pLocationStateFactory.getState(
                varDeclarationEdge.getSuccessor().getLeavingEdge(0).getSuccessor()),
            argStateAfterTMPVarDeclaration);
    // add recursive function call to callstack state
    argStateAfterRecursiveFunctionCall = ProgramTransformationRecoveryUtils.addFunctionCall(argStateAfterRecursiveFunctionCall, AbstractStates.extractLocation(argStateAfterTMPVarDeclaration));
    // if value state is present set it to the value state after all parameters have been assigned
    argStateAfterRecursiveFunctionCall = ProgramTransformationRecoveryUtils.takeValueState(argStateAfterRecursiveFunctionCall, currentARGState);
    ARGState argStateAfterFSDummyEdge =
        ProgramTransformationRecoveryUtils.argStateWithLocation(
            argStateAfterRecursiveFunctionCall,
            pLocationStateFactory.getState(nodeMap.get(AbstractStates.extractLocation(currentARGState))),
            argStateAfterRecursiveFunctionCall);
    // is the state before the function call covered?
    if (currentARGState.isCovered()) {
      argStateAfterFSDummyEdge.setCovered(currentARGState.getCoveringState());
    }
    // remove the marked states and add the new states
    reached.remove(pStateBeforeFirstParameter);
    pStateBeforeFirstParameter.removeFromARG();
//    //if (reached.contains(currentARGState)) {
    reached.add(argStateBeforeTMPVarDeclaration, reached.getPrecision(currentARGState));
    reached.add(argStateAfterTMPVarDeclaration, reached.getPrecision(currentARGState));
    reached.add(argStateAfterRecursiveFunctionCall, reached.getPrecision(currentARGState));
    reached.add(argStateAfterFSDummyEdge, reached.getPrecision(currentARGState));
      reached.remove(currentARGState);
    //}
    currentARGState.replaceInARGWith(argStateAfterFSDummyEdge);
    for (ARGState toBeRemoved : statesToBeRemoved.build()) {
      reached.remove(toBeRemoved);
      toBeRemoved.removeFromARG();
    }
    // return the state at the exit condition check
    return argStateAfterFSDummyEdge;
  }

  /** Enum for tracking the current position in a transformed recursive function when traversing the ARG. */
  private enum TailRecursionState {
    FUNCTION_START,
    EXIT_CONDITION,
    EXIT_BRANCH,
    PARAMETER_ASSIGNMENT
  }

  private TailRecursionState setNewFunctionState(
      ARGState pCurrentARGState,
      ARGState pChildARGState,
      TailRecursionState pCurrentFunctionState) {
    if (AbstractStates.extractLocation(pChildARGState) == exitConditionCheckNode) {
      return TailRecursionState.EXIT_CONDITION;
    } else if (parameterNodes.contains(AbstractStates.extractLocation(pChildARGState))) {
      return TailRecursionState.PARAMETER_ASSIGNMENT;
    } else if (AbstractStates.extractLocation(pCurrentARGState) == exitConditionCheckNode
        && !parameterNodes.contains(AbstractStates.extractLocation(pChildARGState))) {
      return TailRecursionState.EXIT_BRANCH;
    }
    return pCurrentFunctionState;
  }
}
