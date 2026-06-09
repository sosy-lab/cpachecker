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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
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
    CFAEdge varReturnEdge
) implements ProgramTransformationRecovery {

  @Override
  public void revertProgramTransformation(
      AbstractState pPreviousState,
      AbstractState pCurrentARGState,
      SubCFA pBeforeProgramTransformation,
      SubCFA pAfterProgramTransformation,
      ReachedSet reached,
      LocationStateFactory pLocationStateFactory) {

    ARGState previousARGState = (ARGState) pPreviousState;
    ARGState currentARGState = (ARGState) pCurrentARGState;
    TailRecursionState initialFunctionState = TailRecursionState.FUNCTION_START;

    // first state in program transformation gets removed + children get connected to the parent state
    if (pAfterProgramTransformation.subCFAEntryNode() == AbstractStates.extractLocation(pCurrentARGState)) {
      // handle child state with previousARGState as parent
      SequencedCollection<ARGState> childStates = currentARGState.getChildren();
      assert(childStates.size() == 1);
      ARGState childARGState = childStates.getFirst();
      recoverARG(previousARGState, childARGState, initialFunctionState, pAfterProgramTransformation.allNodes(), pAfterProgramTransformation.subCFAExitNode(), reached, pLocationStateFactory);
      // remove entry state after all child states have been handled
      ((ARGState) pCurrentARGState).removeFromARG();
    } else {
      throw new RuntimeException("Invalid use of revertProgramTransformation()!");
    }
  }

  /**
   * Function for recursive depth-first recovery of the ARG.
   *
   * @param pPreviousARGState the previous ARGState
   * @param pCurrentARGState the current ARGState
   * @param pCurrentFunctionState the current position in the function
   * @param pNodes all nodes of the program transformation
   * @param reached the reached set
   * @param pLocationStateFactory for access to LocationStates
   */
  private void recoverARG(ARGState pPreviousARGState, ARGState pCurrentARGState, TailRecursionState pCurrentFunctionState, ImmutableSet<CFANode> pNodes, CFANode pExitNode, ReachedSet reached, LocationStateFactory pLocationStateFactory) {
    // check for the exit node
    if (AbstractStates.extractLocation(pCurrentARGState) == pExitNode) {
      ARGState childState = pCurrentARGState.getChildren().getFirst();
      childState.addParent(pPreviousARGState);
      pCurrentARGState.removeFromARG();
      return;
    }
    // handle the current state depending on position in the program transformation
    ARGState newARGState = switch (pCurrentFunctionState) {
      case PARAMETER_ASSIGNMENT ->
        // map parameter assignment and the following jump to function start to a recursive function call
        recoverParameterAssignments(pPreviousARGState, pCurrentARGState, pLocationStateFactory);
      default ->
        // handle current state normally
          revertSingleState(pPreviousARGState, pCurrentARGState, reached, pLocationStateFactory);
    };
    // handle child states; not in a loop for concurrency’s sake
    SequencedCollection<ARGState> children = newARGState.getChildren();
    switch (children.size()) {
      case 0:
        break;
      case 1:
        if (pNodes.contains(AbstractStates.extractLocation(children.getFirst()))) {
          recoverARG(
              newARGState,
              children.getFirst(),
              setNewFunctionState(newARGState, children.getFirst(), pCurrentFunctionState),
              pNodes,
              pExitNode,
              reached,
              pLocationStateFactory);
        }
        break;
      case 2:
        ARGState child1 = children.getFirst();
        ARGState child2 = children.getLast();
        if (pNodes.contains(AbstractStates.extractLocation(child1))) {
          recoverARG(
              newARGState,
              child1,
              setNewFunctionState(newARGState, child1, pCurrentFunctionState),
              pNodes,
              pExitNode,
              reached,
              pLocationStateFactory);
        }
        if (pNodes.contains(AbstractStates.extractLocation(child2))) {
          recoverARG(
              newARGState,
              child2,
              setNewFunctionState(newARGState, child2, pCurrentFunctionState),
              pNodes,
              pExitNode,
              reached,
              pLocationStateFactory);
        }
        break;
      default:
        throw new RuntimeException("More than two successor ARGStates!");
    }
  }

  private ARGState revertSingleState(ARGState pPreviousARGState, ARGState pCurrentARGState, ReachedSet reached, LocationStateFactory pLocationStateFactory) {
    LocationState newLocationState = pLocationStateFactory.getState(nodeMap.get(AbstractStates.extractLocation(pCurrentARGState)));

    CompositeState currentCompositeState = ((CompositeState) pCurrentARGState.getWrappedState());
    List<AbstractState> newWrappedStates = new ArrayList<>(currentCompositeState.getWrappedStates().size());
    for (AbstractState wrappedState : currentCompositeState.getWrappedStates()) {
      if (wrappedState instanceof LocationState) {
        newWrappedStates.add(newLocationState);
      } else {
        newWrappedStates.add(wrappedState);
      }
    }
    ARGState newARGState = new ARGState(new CompositeState(newWrappedStates), pPreviousARGState);
    pCurrentARGState.replaceInARGWith(newARGState);
    return newARGState;
  }

  /**
   * Helper for dealing with the states before the function parameters get assigned their new values.
   * Replaces all ARGStates before parameter assignments with ARGStates from the recursive function call with new LocationStates.
   *
   * @param pStateBeforeExitCondition the previous ARGState at the exit condition check
   * @param pStateBeforeFirstParameter the current ARGState before the first parameter gets assigned
   * @param pLocationStateFactory Factory for access to LocationStates
   * @return ARGState before the exit condition check
   */
  private ARGState recoverParameterAssignments(ARGState pStateBeforeExitCondition, ARGState pStateBeforeFirstParameter, LocationStateFactory pLocationStateFactory) {
    ARGState currentARGState = pStateBeforeFirstParameter;
    ImmutableList.Builder<ARGState> statesToBeRemoved = new ImmutableList.Builder<>();
    while (AbstractStates.extractLocation(currentARGState) != exitConditionCheckNode) {
      if (currentARGState != pStateBeforeFirstParameter) {
        statesToBeRemoved.add(currentARGState);
      }
      currentARGState = currentARGState.getChildren().getFirst();
    }
    // new ARGStates
    ARGState argStateBeforeTMPVarDeclaration = ProgramTransformationRecoveryUtils.argStateWithLocation(pStateBeforeFirstParameter, pLocationStateFactory.getState(varDeclarationEdge.getPredecessor()), pStateBeforeExitCondition);
    ARGState argStateAfterTMPVarDeclaration = ProgramTransformationRecoveryUtils.argStateWithLocation(pStateBeforeFirstParameter, pLocationStateFactory.getState(varDeclarationEdge.getSuccessor()), argStateBeforeTMPVarDeclaration);
    ARGState argStateAfterRecursiveFunctionCall = ProgramTransformationRecoveryUtils.argStateWithLocation(currentARGState, pLocationStateFactory.getState(varDeclarationEdge.getSuccessor().getLeavingEdge(0).getSuccessor()), argStateAfterTMPVarDeclaration);
    ARGState argStateAfterFSDummyEdge = ProgramTransformationRecoveryUtils.argStateWithLocation(argStateAfterRecursiveFunctionCall, pLocationStateFactory.getState(varDeclarationEdge.getPredecessor().getEnteringEdges().first().get().getPredecessor()), argStateAfterRecursiveFunctionCall);
    //currentARGState.removeParent(currentARGState.getParents().getFirst());
    //currentARGState.addParent(argStateAfterRecursiveFunctionCall);
    pStateBeforeFirstParameter.removeFromARG();
    currentARGState.replaceInARGWith(argStateAfterFSDummyEdge);
    return argStateAfterRecursiveFunctionCall;
  }

  /**
   * Enum for tracking the current position in a transformed function when traversing the ARG.
   */
  private enum TailRecursionState {
    FUNCTION_START,
    EXIT_CONDITION,
    EXIT_BRANCH,
    PARAMETER_ASSIGNMENT
  }

  private TailRecursionState setNewFunctionState(ARGState pCurrentARGState, ARGState pChildARGState, TailRecursionState pCurrentFunctionState) {
    if (AbstractStates.extractLocation(pChildARGState) == exitConditionCheckNode) {
      return TailRecursionState.EXIT_CONDITION;
    } else if(parameterNodes.contains(AbstractStates.extractLocation(pChildARGState))){
      return TailRecursionState.PARAMETER_ASSIGNMENT;
    } else if(AbstractStates.extractLocation(pCurrentARGState) == exitConditionCheckNode && !parameterNodes.contains(AbstractStates.extractLocation(pChildARGState))){
      return TailRecursionState.EXIT_BRANCH;
    }
    return pCurrentFunctionState;
  }
}
