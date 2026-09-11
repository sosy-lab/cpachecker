// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.execution;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.execution.ExecutionState.StackFrame;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation.ValueTransferOptions;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Transfer relation of the {@link ExecutionCPA}. It executes the program edge by edge and checks
 * after every step that the wrapped CPA produced exactly one or zero successors. If a state has
 * more than one successor, the program makes a nondeterministic choice, which this analysis does
 * not support, and an {@link UnsupportedCodeException} is thrown.
 *
 * <p>Because there is at most one successor, several execution steps can be performed within a
 * single call of this transfer relation. The intermediate states are then never added to the
 * reached set and can be garbage collected immediately, which is what makes this analysis use only
 * a constant amount of memory for the state space (cf. {@code cpa.execution.stepsPerTransfer}).
 */
class ExecutionTransferRelation extends AbstractSingleWrapperTransferRelation {

  private final ShutdownNotifier shutdownNotifier;
  private final LogManagerWithoutDuplicates logger;
  private final ExecutionStatistics stats;
  private final ExecutionWitnessExporter witnessExporter;
  private final ExecutionSampler sampler;
  private final @Nullable ValueTransferOptions valueTransferOptions;
  private final boolean collectInvariants;
  private final int stepsPerTransfer;
  private final boolean restoreCallerValues;

  ExecutionTransferRelation(
      TransferRelation pWrapped,
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogger,
      ExecutionStatistics pStats,
      ExecutionWitnessExporter pWitnessExporter,
      ExecutionSampler pSampler,
      @Nullable ValueTransferOptions pValueTransferOptions,
      int pStepsPerTransfer,
      boolean pRestoreCallerValues) {
    super(pWrapped);
    shutdownNotifier = pShutdownNotifier;
    logger = new LogManagerWithoutDuplicates(pLogger);
    stats = pStats;
    witnessExporter = pWitnessExporter;
    sampler = pSampler;
    valueTransferOptions = pValueTransferOptions;
    collectInvariants = pWitnessExporter.collectsInvariants();
    stepsPerTransfer = pStepsPerTransfer;
    restoreCallerValues = pRestoreCallerValues;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pState, Precision pPrecision)
      throws CPATransferException, InterruptedException {

    ExecutionState current = (ExecutionState) pState;
    for (int steps = 0; stepsPerTransfer < 0 || steps < stepsPerTransfer; steps++) {
      shutdownNotifier.shutdownIfNecessary();

      ExecutionState next = executeStep(current, pPrecision);
      if (next == null) {
        // The program ends here (or the specification excludes this path),
        // so this execution has no successor at all.
        return ImmutableList.of();
      }
      current = next;

      if (AbstractStates.isTargetState(current)) {
        // Stop here such that the target state is added to the reached set
        // and the analysis reports a property violation.
        break;
      }
    }
    return ImmutableList.of(current);
  }

  /**
   * Execute a single program step.
   *
   * @return the successor state, or {@code null} if the execution ends at the given state
   * @throws UnsupportedCodeException if the given state has more than one successor
   */
  private @Nullable ExecutionState executeStep(ExecutionState pState, Precision pPrecision)
      throws CPATransferException, InterruptedException {

    final AbstractState wrappedState = pState.getWrappedState();
    final AbstractStateWithLocations locationState =
        AbstractStates.extractStateByType(wrappedState, AbstractStateWithLocations.class);
    if (locationState == null) {
      throw new CPATransferException(
          "ExecutionCPA needs a CPA that tracks the program location, e.g., LocationCPA");
    }

    if (collectInvariants) {
      // Remember the assignments of this execution for the invariants of a correctness witness.
      for (CFANode location : locationState.getLocationNodes()) {
        witnessExporter.observe(location, wrappedState);
      }
    }

    AbstractState state = wrappedState;
    AlgorithmStatus status = pState.getStatus();
    if (valueTransferOptions != null) {
      for (CFAEdge edge : locationState.getOutgoingEdges()) {
        if (edge instanceof CStatementEdge statementEdge
            && !(edge instanceof CFunctionSummaryStatementEdge)
            && statementEdge.getStatement() instanceof CFunctionCall functionCall) {
          // Account for calls even if the specification subsequently removes their successors.
          status =
              status.update(
                  ValueAnalysisTransferRelation.handleUnknownOrUnhandledFunctionCalls(
                      statementEdge, functionCall, valueTransferOptions, logger));
        }
      }
    }
    List<ExecutionStep> steps =
        computeSteps(
            state,
            pState.getCallStack(),
            locationState,
            pPrecision,
            /* pAllowSeveralSuccessors= */ sampler.isEnabled());

    if (steps.size() > 1) {
      // The execution depends on an input of the program. Choose values for the inputs and
      // continue with the successor that these values determine.
      AbstractState sampledState = sampler.sample(state, steps);
      if (sampledState == null) {
        throw nondeterminismException(steps.get(0).edge(), steps.get(1).edge());
      }
      state = sampledState;
      status = status.withSound(false);
      steps =
          computeSteps(
              state,
              pState.getCallStack(),
              locationState,
              pPrecision,
              /* pAllowSeveralSuccessors= */ false);
    }

    if (steps.isEmpty()) {
      new ExecutionState(wrappedState, pState.getCallStack(), status).checkSoundness();
      return null;
    }
    ExecutionStep step = steps.getFirst();
    ExecutionState successor =
        new ExecutionState(step.successor(), updateCallStack(pState, step.edge(), state), status);
    if (AbstractStates.isTargetState(successor)) {
      successor.checkTargetState();
      // Remember where the specification was violated for the violation witness.
      witnessExporter.reportViolation(step.edge());
    }
    stats.executedSteps.inc();
    return successor;
  }

  /**
   * Compute the successors of the given state by evaluating each of the edges that leave its
   * location.
   *
   * @param pAllowSeveralSuccessors whether to return all successors instead of aborting as soon as
   *     a second one is found
   * @throws UnsupportedCodeException if there is more than one successor and {@code
   *     pAllowSeveralSuccessors} is false
   */
  private List<ExecutionStep> computeSteps(
      AbstractState pState,
      @Nullable StackFrame pCallStack,
      AbstractStateWithLocations pLocationState,
      Precision pPrecision,
      boolean pAllowSeveralSuccessors)
      throws CPATransferException, InterruptedException {

    // The state whose function-scoped values were restored, computed on demand
    // because this is necessary only when returning from a recursive function call.
    AbstractState restoredState = null;

    List<ExecutionStep> steps = new ArrayList<>(1);
    for (CFAEdge edge : pLocationState.getOutgoingEdges()) {
      AbstractState predecessor = pState;
      if (edge instanceof FunctionReturnEdge returnEdge && pCallStack != null) {
        if (restoredState == null) {
          restoredState = restoreCallerValues(pState, returnEdge, pCallStack);
        }
        predecessor = restoredState;
      }

      for (AbstractState newState :
          transferRelation.getAbstractSuccessorsForEdge(predecessor, pPrecision, edge)) {
        if (isNewSuccessor(steps, newState)) {
          if (!steps.isEmpty() && !pAllowSeveralSuccessors) {
            throw nondeterminismException(steps.getFirst().edge(), edge);
          }
          steps.add(new ExecutionStep(edge, newState));
        }
      }
    }
    return steps;
  }

  private static boolean isNewSuccessor(List<ExecutionStep> pSteps, AbstractState pState) {
    for (ExecutionStep step : pSteps) {
      if (isSameState(step.successor(), pState)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Whether two successors represent the same state.
   *
   * <p>The transfer relation of a CPA returns a <em>set</em> of successors, but {@link
   * CompositeState} does not implement {@link Object#equals(Object)}, so the same successor can be
   * contained more than once. (For example {@link org.sosy_lab.cpachecker.cpa.overflow.OverflowCPA}
   * creates one state per leaving edge of the successor node, and these states are equal if the
   * edges have the same assumptions.) We therefore compare composite states component-wise, such
   * that duplicates are not mistaken for a nondeterministic choice.
   */
  private static boolean isSameState(AbstractState pState, AbstractState pOther) {
    if (pState.equals(pOther)) {
      return true;
    }
    if (pState instanceof CompositeState state && pOther instanceof CompositeState other) {
      List<AbstractState> components = state.getWrappedStates();
      List<AbstractState> otherComponents = other.getWrappedStates();
      if (components.size() != otherComponents.size()) {
        return false;
      }
      for (int i = 0; i < components.size(); i++) {
        if (!isSameState(components.get(i), otherComponents.get(i))) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  private UnsupportedCodeException nondeterminismException(
      @Nullable CFAEdge pFirstEdge, CFAEdge pSecondEdge) {
    String reason =
        pFirstEdge == pSecondEdge
            ? String.format("the edge \"%s\" has more than one successor", pSecondEdge)
            : String.format(
                "both of the edges \"%s\" and \"%s\" have a successor", pFirstEdge, pSecondEdge);
    return new UnsupportedCodeException(
        "nondeterministic choice ("
            + reason
            + "); ExecutionCPA requires that the behavior of the program is fully determined",
        pSecondEdge);
  }

  /** Push a stack frame for a function call and pop one for a function return. */
  private @Nullable StackFrame updateCallStack(
      ExecutionState pState, CFAEdge pEdge, AbstractState pCallerState) {
    final StackFrame callStack = pState.getCallStack();

    if (pEdge instanceof FunctionCallEdge callEdge) {
      FunctionEntryNode entryNode = callEdge.getSuccessor();
      boolean isRecursive = callStack != null && callStack.contains(entryNode.getFunctionName());
      if (isRecursive) {
        stats.recursiveCalls.inc();
      }
      StackFrame newFrame =
          new StackFrame(
              callStack,
              entryNode.getFunctionName(),
              isRecursive ? shadowedValues(pCallerState, entryNode) : null);
      stats.maxCallStackDepth.setNextValue(newFrame.getDepth());
      return newFrame;
    }

    if (pEdge instanceof FunctionReturnEdge && callStack != null) {
      return callStack.getParent();
    }
    return callStack;
  }

  /**
   * Compute the values of the caller that a recursive function call is about to overwrite, i.e.,
   * the values of all memory locations in the scope of the called function. This is called for
   * recursive calls only, because a caller cannot have values in the scope of a function that is
   * not active yet. Non-recursive programs are thus not affected at all, and a recursive program
   * pays one iteration over the state of the value analysis per recursive call.
   *
   * <p>Note that the value analysis cannot distinguish the memory locations of two invocations of
   * the same function, so a callee that writes to a local variable of a recursive caller through a
   * pointer is still analyzed imprecisely. Use the SMG analysis for such programs, it models the
   * stack frames explicitly.
   *
   * @return the values to restore when the call returns, or {@code null} if nothing needs to be
   *     restored
   */
  private @Nullable ImmutableMap<MemoryLocation, ValueAndType> shadowedValues(
      AbstractState pCallerState, FunctionEntryNode pEntryNode) {

    if (!restoreCallerValues) {
      return null;
    }
    ValueAnalysisState valueState =
        AbstractStates.extractStateByType(pCallerState, ValueAnalysisState.class);
    if (valueState == null) {
      // Analyses that model the function stack explicitly (e.g., the SMG analysis)
      // do not need any help here.
      return null;
    }
    final String function = pEntryNode.getFunctionName();

    final MemoryLocation returnVariable = returnVariable(pEntryNode);
    ImmutableMap.Builder<MemoryLocation, ValueAndType> shadowed = ImmutableMap.builder();
    for (Entry<MemoryLocation, ValueAndType> entry : valueState.getConstants()) {
      MemoryLocation memoryLocation = entry.getKey();
      if (memoryLocation.isOnFunctionStack(function) && !memoryLocation.equals(returnVariable)) {
        shadowed.put(memoryLocation, entry.getValue());
      }
    }
    return shadowed.buildOrThrow();
  }

  /**
   * Replace the frame of the returning function by the frame of its caller. This is the counterpart
   * of {@link #shadowedValues} and is applied to the state at the exit node of the function, i.e.,
   * before the return value is assigned to the left-hand side of the function call.
   */
  private AbstractState restoreCallerValues(
      AbstractState pState, FunctionReturnEdge pEdge, StackFrame pFrame) {

    final ImmutableMap<MemoryLocation, ValueAndType> shadowed = pFrame.getShadowedValues();
    if (shadowed == null
        || !pFrame.getFunctionName().equals(pEdge.getFunctionEntry().getFunctionName())) {
      // Nothing was overwritten by this call, or the call stack of this CPA does not match the
      // returning function (which can happen only if the analysis does not start at a function
      // entry). In both cases the state of the value analysis is already correct.
      return pState;
    }
    if (!(pState instanceof CompositeState compositeState)) {
      logRestoringNotPossible();
      return pState;
    }
    final List<AbstractState> components = new ArrayList<>(compositeState.getWrappedStates());
    final int valueStateIndex = indexOfValueState(components);
    if (valueStateIndex < 0) {
      logRestoringNotPossible();
      return pState;
    }

    final String function = pFrame.getFunctionName();
    final MemoryLocation returnVariable = returnVariable(pEdge.getFunctionEntry());
    final ValueAnalysisState restored =
        ValueAnalysisState.copyOf((ValueAnalysisState) components.get(valueStateIndex));
    // Drop the frame of the callee ...
    for (MemoryLocation memoryLocation : restored.getTrackedMemoryLocations()) {
      if (memoryLocation.isOnFunctionStack(function) && !memoryLocation.equals(returnVariable)) {
        restored.forget(memoryLocation);
      }
    }
    // ... and put the frame of the caller back in place.
    shadowed.forEach(
        (memoryLocation, valueAndType) ->
            restored.assignConstant(
                memoryLocation, valueAndType.getValue(), valueAndType.getType()));

    components.set(valueStateIndex, restored);
    return new CompositeState(components);
  }

  private static int indexOfValueState(List<AbstractState> pComponents) {
    for (int i = 0; i < pComponents.size(); i++) {
      if (pComponents.get(i) instanceof ValueAnalysisState) {
        return i;
      }
    }
    return -1;
  }

  private static @Nullable MemoryLocation returnVariable(FunctionEntryNode pEntryNode) {
    return pEntryNode
        .getReturnVariable()
        .map((AVariableDeclaration declaration) -> MemoryLocation.forDeclaration(declaration))
        .orElse(null);
  }

  private void logRestoringNotPossible() {
    logger.logOnce(
        Level.WARNING,
        "Could not find the value analysis for restoring the values of a recursive function call."
            + " ExecutionCPA needs to wrap a CompositeCPA that contains ValueAnalysisCPA directly,"
            + " otherwise recursive functions are analyzed imprecisely.");
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) {
    throw new UnsupportedOperationException(
        "ExecutionCPA does not support the computation of successors for a single edge");
  }
}
