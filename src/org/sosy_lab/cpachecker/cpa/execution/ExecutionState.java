// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.execution;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * State of the {@link ExecutionCPA}. Besides the state of the wrapped CPA it stores the stack of
 * currently active function calls, which is needed to restore the values of a caller after
 * returning from a recursive function call (cf. {@link StackFrame}), and whether the execution
 * still permits a sound proof of safety or a precise counterexample.
 */
public class ExecutionState extends AbstractSingleWrapperState {

  /** Stack of active function calls, {@code null} if no function call is active. */
  private final @Nullable StackFrame callStack;

  private final AlgorithmStatus status;

  ExecutionState(AbstractState pWrappedState, @Nullable StackFrame pCallStack) {
    this(pWrappedState, pCallStack, AlgorithmStatus.SOUND_AND_PRECISE);
  }

  ExecutionState(
      AbstractState pWrappedState, @Nullable StackFrame pCallStack, AlgorithmStatus pStatus) {
    super(checkNotNull(pWrappedState));
    callStack = pCallStack;
    status = checkNotNull(pStatus);
  }

  public AlgorithmStatus getStatus() {
    return status;
  }

  @Nullable StackFrame getCallStack() {
    return callStack;
  }

  /** Check that this execution still proves that the program is safe. */
  void checkMayProveSafety() throws CPATransferException {
    if (!status.isSound()) {
      throw new CPATransferException(
          "The execution ended without violating the specification, but it is no longer sound"
              + " because function side effects were ignored or inputs were sampled. ExecutionCPA"
              + " cannot prove that the program is safe.");
    }
  }

  /** Check that a violation of the specification in this state may be reported. */
  void checkMayReportViolation() throws CPATransferException {
    if (isTarget() && !status.isPrecise()) {
      throw new CPATransferException(
          "The execution reached a target state, but it is no longer precise because a return"
              + " value of an unhandled function call was overapproximated. ExecutionCPA cannot"
              + " report a property violation.");
    }
  }

  /**
   * One entry of the stack of active function calls.
   *
   * <p>A value analysis identifies the local variables of a function by the name of the function
   * (cf. {@link MemoryLocation#isOnFunctionStack(String)}), so all invocations of the same function
   * share the same memory locations. For a recursive call this means that the callee overwrites the
   * local variables of its caller and that returning from the callee discards them. A stack frame
   * therefore remembers the values that a recursive call is about to overwrite, so that {@link
   * ExecutionTransferRelation} can restore them when the call returns. This is done in this CPA
   * only, the value analysis itself is not affected.
   *
   * @param parent the frame of the caller, {@code null} for the outermost call
   * @param functionName the name of the function that this frame belongs to
   * @param shadowedValues the values of the caller that the callee overwrites because it uses the
   *     same memory locations; empty if this call is not recursive and thus nothing is overwritten
   * @param depth the number of frames on the stack, including this one
   */
  record StackFrame(
      @Nullable StackFrame parent,
      String functionName,
      ImmutableMap<MemoryLocation, ValueAndType> shadowedValues,
      int depth) {

    StackFrame {
      checkNotNull(functionName);
      checkNotNull(shadowedValues);
    }

    /** Push a new frame for a call of the given function onto the given stack. */
    static StackFrame push(
        @Nullable StackFrame pParent,
        String pFunctionName,
        ImmutableMap<MemoryLocation, ValueAndType> pShadowedValues) {
      return new StackFrame(
          pParent, pFunctionName, pShadowedValues, pParent == null ? 1 : pParent.depth() + 1);
    }

    /** Whether the given function is already active, i.e., whether calling it means recursion. */
    boolean contains(String pFunctionName) {
      for (StackFrame frame = this; frame != null; frame = frame.parent()) {
        if (frame.functionName().equals(pFunctionName)) {
          return true;
        }
      }
      return false;
    }
  }
}
