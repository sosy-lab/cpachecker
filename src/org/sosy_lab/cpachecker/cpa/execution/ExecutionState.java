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

  void checkSoundness() throws CPATransferException {
    if (!status.isSound()) {
      throw new CPATransferException(
          "The execution ended without violating the specification, but it is no longer sound"
              + " because function side effects were ignored or inputs were sampled. ExecutionCPA"
              + " cannot prove that the program is safe.");
    }
  }

  void checkTargetState() throws CPATransferException {
    if (isTarget() && !status.isPrecise()) {
      throw new CPATransferException(
          "The execution reached a target state, but it is no longer precise because a return"
              + " value of an unhandled function call was overapproximated. ExecutionCPA cannot"
              + " report a property violation.");
    }
  }

  @Nullable StackFrame getCallStack() {
    return callStack;
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
   */
  static final class StackFrame {

    private final @Nullable StackFrame parent;
    private final String functionName;

    /**
     * The values of the caller that the callee will overwrite because it uses the same memory
     * locations, or {@code null} if this call is not recursive and thus nothing gets overwritten.
     */
    private final @Nullable ImmutableMap<MemoryLocation, ValueAndType> shadowedValues;

    private final int depth;

    StackFrame(
        @Nullable StackFrame pParent,
        String pFunctionName,
        @Nullable ImmutableMap<MemoryLocation, ValueAndType> pShadowedValues) {
      parent = pParent;
      functionName = checkNotNull(pFunctionName);
      shadowedValues = pShadowedValues;
      depth = pParent == null ? 1 : pParent.depth + 1;
    }

    @Nullable StackFrame getParent() {
      return parent;
    }

    String getFunctionName() {
      return functionName;
    }

    @Nullable ImmutableMap<MemoryLocation, ValueAndType> getShadowedValues() {
      return shadowedValues;
    }

    int getDepth() {
      return depth;
    }

    /** Whether the given function is already active, i.e., whether calling it means recursion. */
    boolean contains(String pFunctionName) {
      for (StackFrame frame = this; frame != null; frame = frame.parent) {
        if (frame.functionName.equals(pFunctionName)) {
          return true;
        }
      }
      return false;
    }
  }
}
