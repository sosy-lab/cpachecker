// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.common.collect.Collections3.listAndElement;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Decides whether a call to <code>__VERIFIER_nondet_memory</code> havocs a block that is not
 * accessed between its allocation in memory and the call to <code>__VERIFIER_nondet_memory</code>.
 */
final class UntouchedAllocationCheck {

  /**
   * A function activation on the walk's call chain. The call site is empty if the walk does not
   * know the caller yet, which is only the case for the outermost frame.
   */
  private record Frame(String function, Optional<FunctionSummaryEdge> callSite) {}

  /**
   * A point of the backward walk: the walk is at <code>node</code>, the havoc target is held by
   * <code>trackedLocation</code>, and <code>callStack</code> holds the function activations of the
   * walk, outermost first and innermost last. The call stack is never empty, and its last frame is
   * the function of <code>node</code>.
   */
  private record WalkState(
      CFANode node, MemoryLocation trackedLocation, ImmutableList<Frame> callStack) {}

  /** The outcome of walking backward over one edge. */
  private sealed interface StepResult {}

  /**
   * Some path that reaches the target __VERIFIER_nondet_memory may access the relevant memory or
   * not start from an allocation.
   */
  private record DirtyPath() implements StepResult {}

  /**
   * Confirmed that the currently considered path is clean from the last memory allocation to the
   * target __VERIFIER_nondet_memory.
   */
  private record ConfirmedCleanPath() implements StepResult {}

  /** The walk continues before the edge at the given state. */
  private record PotentialCleanPath(WalkState predecessor) implements StepResult {}

  private final Predicate<String> isAllocationFunction;

  UntouchedAllocationCheck(Predicate<String> pIsAllocationFunction) {
    isAllocationFunction = pIsAllocationFunction;
  }

  /**
   * Returns whether the given call of <code>__VERIFIER_nondet_memory</code> havocs a block that is
   * not accessed between its allocation in memory and the call to <code>__VERIFIER_nondet_memory
   * </code>.
   *
   * <p>The most trivial case for this:
   *
   * <pre>
   * int * p = malloc(4 * sizeof(int));
   * __VERIFIER_nondet_memory(p, 4);
   * </pre>
   *
   * This method returns <code>false</code> if there is a code path to the <code>
   * __VERIFIER_nondet_memory</code> that does not allocate the havocked memory. The result is not
   * sound for multi-threaded programs, because another thread may access the block in between.
   * Recursive calls through function pointers are not supported yet: the result is not sound for
   * analyses that skip such calls, or that skip recursive calls in general, because a skipped call
   * may assign the havocked pointer.
   *
   * @throws IllegalArgumentException if edge 'pNondetMemoryCall' is not a call to <code>
   *     __VERIFIER_nondet_memory</code>
   */
  boolean isHavocOfUntouchedAllocation(CStatementEdge pNondetMemoryCall) {
    Optional<MemoryLocation> target = getTargetOfNondetMemory(pNondetMemoryCall);
    if (target.isEmpty()) {
      return false;
    }
    CFANode startNode = pNondetMemoryCall.getPredecessor();
    return doAllPathsBackwardReachAllocationWithoutUse(startNode, target.orElseThrow());
  }

  private Optional<MemoryLocation> getTargetOfNondetMemory(CStatementEdge pNondetMemoryCall) {
    checkArgument(
        CFAUtils.getFunctionCallName(pNondetMemoryCall)
            .equals(Optional.of(VerifierNondetFunctions.getFunctionNameNondetMemory())),
        "Edge is not a call of %s: %s",
        VerifierNondetFunctions.getFunctionNameNondetMemory(),
        pNondetMemoryCall);

    List<CExpression> nondetMemoryCallArguments =
        ((CFunctionCall) pNondetMemoryCall.getStatement())
            .getFunctionCallExpression()
            .getParameterExpressions();
    checkArgument(
        !nondetMemoryCallArguments.isEmpty(), "Call without arguments: %s", pNondetMemoryCall);

    return getLocationIfCastIdExpression(nondetMemoryCallArguments.getFirst());
  }

  private boolean doAllPathsBackwardReachAllocationWithoutUse(
      CFANode pStartNode, MemoryLocation target) {
    WalkState start =
        new WalkState(
            pStartNode,
            target,
            ImmutableList.of(new Frame(pStartNode.getFunctionName(), Optional.empty())));
    Deque<WalkState> worklist = new ArrayDeque<>();
    Set<WalkState> visited = new HashSet<>();

    visited.add(start);
    worklist.push(start);
    while (!worklist.isEmpty()) {
      WalkState state = worklist.pop();
      FluentIterable<CFAEdge> enteringEdges = getRelevantEnteringEdges(state);
      // A node without predecessors can not lead to an allocation anymore.
      // If we don't have a memory allocation to the target that is set with
      // __VERIFIER_nondet_memory,
      // then we return false.
      if (enteringEdges.isEmpty()) {
        return false;
      }
      for (CFAEdge edge : enteringEdges) {
        switch (backwardsStep(edge, state)) {
          case DirtyPath foundDirtyPath -> {
            return false;
          }
          case ConfirmedCleanPath confirmedCleanPath -> {
            // Only one path is considered clean yet. We do not need to continue
            // this path, but still have to consider the other paths in the waitlist
          }
          case PotentialCleanPath(WalkState predecessor) -> {
            // The current path is clean so far, but hasn't reached a memory allocation yet.
            if (visited.add(predecessor)) {
              worklist.push(predecessor);
            }
          }
        }
      }
    }
    return true;
  }

  /** Returns the edges into the state's node that lie on a path the walk can be on. */
  private FluentIterable<CFAEdge> getRelevantEnteringEdges(WalkState pState) {
    Optional<FunctionSummaryEdge> callSite = pState.callStack().getLast().callSite();
    return pState
        .node()
        .getEnteringEdges()
        // A summary statement edge is taken instead of the call only for thread creation and for
        // skipped recursive calls; we don't consider both cases yet, so we always ignore these
        // edges.
        .filter(edge -> !(edge instanceof CFunctionSummaryStatementEdge))
        // Make sure we only consider a function-call edge (going backwards, to the caller)
        // if this matches our call-stack
        .filter(
            edge ->
                !(edge instanceof CFunctionCallEdge callEdge)
                    || callSite.isEmpty()
                    || callSite.orElseThrow().equals(callEdge.getSummaryEdge()));
  }

  private StepResult backwardsStep(CFAEdge pEdge, WalkState pCurrentState) {
    if (mayAccessMemory(pEdge)) {
      return new DirtyPath();
    }
    return switch (pEdge) {
      case CFunctionReturnEdge returnEdge ->
          backwardsStepIntoReturningFunction(returnEdge, pCurrentState);
      case CFunctionCallEdge callEdge -> backwardsStepOutOfCall(callEdge, pCurrentState);
      case CReturnStatementEdge returnStatementEdge ->
          backwardsStepOverReturnStatement(returnStatementEdge, pCurrentState);
      case CStatementEdge statementEdge -> backwardsStepOverStatement(statementEdge, pCurrentState);
      case CDeclarationEdge declarationEdge ->
          backwardsStepOverDeclaration(declarationEdge, pCurrentState);
      default ->
          cleanPathToPredecessor(pEdge, pCurrentState.trackedLocation(), pCurrentState.callStack());
    };
  }

  /**
   * Returns whether a call on a statement edge may access memory. After call edges are inserted,
   * CFunctionCallExpressions are only functions that have no body in the program, or go through a
   * function pointer and may reach any function. For these functions we can't determine their
   * behavior, so we count any potential memory access. But: We still assume that functions have no
   * side-effects.
   */
  private boolean mayAccessMemory(CFunctionCallExpression pCall) {
    if (!(pCall.getFunctionNameExpression() instanceof CIdExpression functionName)) {
      return true;
    }
    if (!(functionName.getDeclaration() instanceof CFunctionDeclaration)) {
      return true;
    }
    if (isAllocationCall(pCall)) {
      return false;
    }
    return pCall.getParameterExpressions().stream()
        .anyMatch(argument -> isPointerOrArray(argument.getExpressionType()));
  }

  private boolean isAllocationCall(CRightHandSide pValue) {
    if (!(pValue instanceof CFunctionCallExpression call)) {
      return false;
    }
    return call.getFunctionNameExpression() instanceof CIdExpression functionName
        && functionName.getDeclaration() instanceof CFunctionDeclaration
        && isAllocationFunction.test(functionName.getName());
  }

  private boolean isPointerOrArray(CType pType) {
    CType type = pType.getCanonicalType();
    return type instanceof CPointerType || type instanceof CArrayType;
  }

  private StepResult backwardsStepIntoReturningFunction(
      CFunctionReturnEdge pEdge, WalkState pState) {
    CFunctionSummaryEdge summaryEdge = pEdge.getSummaryEdge();
    // Qualified names cannot distinguish two frames of the same function, so we can't handle
    // recursive calls here -> we give up and assume the path is dirty.
    String currentFunction = summaryEdge.getFunctionEntry().getFunctionName();
    if (isOnCallChain(currentFunction, pState)) {
      return new DirtyPath();
    }
    MemoryLocation trackedLocation = pState.trackedLocation();
    if (summaryEdge.getExpression() instanceof CFunctionCallAssignmentStatement assignment) {
      // If the return value is assigned to some value, check whether this is a pointer dereference
      // -> may change our target between allocation and __VERIFIER_nondet_memory
      CLeftHandSide assigneeOfFunctionCall = assignment.getLeftHandSide();
      if (containsDereference(assigneeOfFunctionCall)) {
        return new DirtyPath();
      }

      if (refersToLocation(assigneeOfFunctionCall, trackedLocation)) {
        // the tracked variable is directly assigned the return value of the called function
        // -> because we are going backwards, we are now interested in how the return variable is
        // assigned
        // inside the function we're about to enter
        Optional<CVariableDeclaration> returnVariable =
            summaryEdge.getFunctionEntry().getReturnVariable();
        if (returnVariable.isEmpty()) {
          // Not sure when this would happen, but I also don't want to crash.
          // Instead let's be lenient and just say that the path is dirty.
          return new DirtyPath();
        }
        trackedLocation = MemoryLocation.forDeclaration(returnVariable.orElseThrow());
      }
    }
    Frame calleeFrame =
        new Frame(summaryEdge.getFunctionEntry().getFunctionName(), Optional.of(summaryEdge));
    return cleanPathToPredecessor(
        pEdge, trackedLocation, listAndElement(pState.callStack(), calleeFrame));
  }

  private boolean isOnCallChain(String pFunctionName, WalkState pState) {
    return pState.callStack().stream().anyMatch(frame -> frame.function().equals(pFunctionName));
  }

  private boolean containsDereference(AAstNode pAstNode) {
    return containsDereference(ImmutableList.of(pAstNode));
  }

  private boolean containsDereference(Iterable<? extends AAstNode> pAstNodes) {
    return FluentIterable.from(pAstNodes)
        .transformAndConcat(astNode -> CFAUtils.traverseRecursively(astNode))
        .anyMatch(
            astNode ->
                astNode instanceof CPointerExpression
                    || astNode instanceof CArraySubscriptExpression
                    || (astNode instanceof CFieldReference fieldReference
                        && fieldReference.isPointerDereference()));
  }

  private StepResult backwardsStepOutOfCall(CFunctionCallEdge pEdge, WalkState pState) {
    ImmutableList<Frame> callStack = pState.callStack();
    Optional<MemoryLocation> maybeTrackedLocation =
        resolveLocationToAssignmentOutsideOfFunctionCall(pState.trackedLocation(), pEdge);
    if (maybeTrackedLocation.isEmpty()) {
      // We were not able to directly map the tracked variable to its parameter
      // -> over-approximate by saying the path is dirty.
      return new DirtyPath();
    }
    MemoryLocation trackedLocation = maybeTrackedLocation.orElseThrow();

    ImmutableList<Frame> poppedCallStack = callStack.subList(0, callStack.size() - 1);
    // Leaving the outermost frame reveals its caller, which becomes the new outermost frame.
    // If the popped call stack is empty, we didn't know the caller yet because we started
    // exploration inside some called function.
    // In this case, we can not know the full call stack, but we can at least retrieve the function
    // we are about to enter.
    // We put that in the callstack to keep track of our current frame.
    if (poppedCallStack.isEmpty()) {
      poppedCallStack =
          ImmutableList.of(new Frame(pEdge.getPredecessor().getFunctionName(), Optional.empty()));
    }
    return cleanPathToPredecessor(pEdge, trackedLocation, poppedCallStack);
  }

  private Optional<MemoryLocation> resolveLocationToAssignmentOutsideOfFunctionCall(
      MemoryLocation pLocation, CFunctionCallEdge pFunctionCall) {
    List<CParameterDeclaration> parameters = pFunctionCall.getSuccessor().getFunctionParameters();
    List<CExpression> arguments = pFunctionCall.getArguments();
    for (int i = 0; i < parameters.size(); i++) {
      if (MemoryLocation.forDeclaration(parameters.get(i)).equals(pLocation)) {
        return getLocationIfCastIdExpression(arguments.get(i));
      }
    }
    // If the variable is scoped to the function but not one of the function's parameters,
    // then it was declared inside the function and we have no assignment from the outside.
    if (pLocation.isOnFunctionStack(pFunctionCall.getSuccessor().getFunctionName())) {
      return Optional.empty();
    }
    // If the variable is not scoped to the function, we just continue tracking it directly
    return Optional.of(pLocation);
  }

  private StepResult backwardsStepOverReturnStatement(
      CReturnStatementEdge pEdge, WalkState pState) {
    Optional<CAssignment> maybeReturnAssignment = pEdge.asAssignment();
    if (maybeReturnAssignment.isPresent()) {
      CAssignment returnAssignment = maybeReturnAssignment.orElseThrow();
      CLeftHandSide returnVariable = returnAssignment.getLeftHandSide();
      if (refersToLocation(returnVariable, pState.trackedLocation())) {
        return backwardsStepForAssignment(
            pEdge, returnAssignment.getRightHandSide(), pState.callStack());
      }
    }
    return cleanPathToPredecessor(pEdge, pState.trackedLocation(), pState.callStack());
  }

  private StepResult backwardsStepOverStatement(CStatementEdge pEdge, WalkState pState) {
    if (pEdge.getStatement() instanceof CAssignment assignment
        && refersToLocation(assignment.getLeftHandSide(), pState.trackedLocation())) {
      if (assignment instanceof CFunctionCallAssignmentStatement callAssignment) {
        // The path ends successfully at an allocation and fails at any other call.
        return isAllocationCall(callAssignment.getRightHandSide())
            ? new ConfirmedCleanPath()
            : new DirtyPath();
      }
      return backwardsStepForAssignment(pEdge, assignment.getRightHandSide(), pState.callStack());
    }
    return cleanPathToPredecessor(pEdge, pState.trackedLocation(), pState.callStack());
  }

  private StepResult backwardsStepOverDeclaration(CDeclarationEdge pEdge, WalkState pState) {
    if (pEdge.getDeclaration() instanceof CVariableDeclaration declaration
        && MemoryLocation.forDeclaration(declaration).equals(pState.trackedLocation())) {
      return declaration.getInitializer() instanceof CInitializerExpression initializer
          ? backwardsStepForAssignment(pEdge, initializer.getExpression(), pState.callStack())
          : new DirtyPath();
    }
    return cleanPathToPredecessor(pEdge, pState.trackedLocation(), pState.callStack());
  }

  private StepResult backwardsStepForAssignment(
      CFAEdge pEdge, CRightHandSide pValue, ImmutableList<Frame> pCallStack) {
    Optional<MemoryLocation> source =
        pValue instanceof CExpression value
            ? getLocationIfCastIdExpression(value)
            : Optional.empty();
    return source.isPresent()
        ? cleanPathToPredecessor(pEdge, source.orElseThrow(), pCallStack)
        : new DirtyPath();
  }

  private StepResult cleanPathToPredecessor(
      CFAEdge pEdge, MemoryLocation pTrackedLocation, ImmutableList<Frame> pCallStack) {
    return new PotentialCleanPath(
        new WalkState(pEdge.getPredecessor(), pTrackedLocation, pCallStack));
  }

  /**
   * Returns whether the edge may access memory. This over-approximates strongly: any dereference
   * counts, whatever its target, and so does any call of an undefined function that receives a
   * pointer.
   */
  private boolean mayAccessMemory(CFAEdge pEdge) {
    return containsDereference(CFAUtils.getAstNodesFromCfaEdge(pEdge))
        || (pEdge instanceof CStatementEdge statementEdge
            && statementEdge.getStatement() instanceof CFunctionCall call
            && mayAccessMemory(call.getFunctionCallExpression()));
  }

  private boolean refersToLocation(CLeftHandSide pLeftHandSide, MemoryLocation pLocation) {
    return pLeftHandSide instanceof CIdExpression variable
        && MemoryLocation.forDeclaration(variable.getDeclaration()).equals(pLocation);
  }

  /**
   * Returns the memory location that the expression reads. Empty if the expression is not an
   * identifier that we can map to a memory location (ignoring casts).
   */
  private Optional<MemoryLocation> getLocationIfCastIdExpression(CExpression pExpression) {
    if (pExpression instanceof CCastExpression cast) {
      return getLocationIfCastIdExpression(cast.getOperand());
    }
    if (pExpression instanceof CIdExpression idExpression
        && (idExpression.getDeclaration() instanceof CVariableDeclaration
            || idExpression.getDeclaration() instanceof CParameterDeclaration)) {
      return Optional.of(MemoryLocation.forDeclaration(idExpression.getDeclaration()));
    }
    return Optional.empty();
  }
}
