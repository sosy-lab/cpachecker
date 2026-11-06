// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interpreter;

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
 * State of the {@link InterpreterCPA}. Besides the state of the wrapped CPA it stores the stack of
 * currently active function calls (cf. {@link StackFrame}) and whether the execution still permits
 * a sound proof of safety or a precise counterexample.
 *
 * <p>The stack is needed because a value analysis identifies local variables by the name of their
 * function (cf. {@link MemoryLocation#isOnFunctionStack(String)}), so a recursive call overwrites
 * the variables of its caller. Every frame remembers the values to restore when the call returns.
 *
 * <p>The status records whether ignored external calls have made the execution unsound or
 * imprecise. A target state may be reported only if the execution is still precise, and termination
 * without a target state proves safety only if it is still sound; otherwise the analysis aborts
 * with an exception and reports {@code UNKNOWN}.
 */
public class InterpreterState extends AbstractSingleWrapperState {

  /** Stack of active function calls, with the frame of the entry function at the bottom. */
  private final StackFrame callStack;

  private final AlgorithmStatus status;

  InterpreterState(AbstractState pWrappedState, StackFrame pCallStack) {
    this(pWrappedState, pCallStack, AlgorithmStatus.SOUND_AND_PRECISE);
  }

  InterpreterState(AbstractState pWrappedState, StackFrame pCallStack, AlgorithmStatus pStatus) {
    super(checkNotNull(pWrappedState));
    callStack = checkNotNull(pCallStack);
    status = checkNotNull(pStatus);
  }

  public AlgorithmStatus getStatus() {
    return status;
  }

  StackFrame getCallStack() {
    return callStack;
  }

  /** Check that this execution still proves that the program is safe. */
  void checkMayProveSafety() throws CPATransferException {
    if (!status.isSound()) {
      throw new CPATransferException(
          "The execution ended without violating the specification, but it is no longer sound"
              + " because function side effects were ignored or inputs were sampled. InterpreterCPA"
              + " cannot prove that the program is safe.");
    }
  }

  /** Check that a violation of the specification in this state may be reported. */
  void checkMayReportViolation() throws CPATransferException {
    if (isTarget() && !status.isPrecise()) {
      throw new CPATransferException(
          "The execution reached a target state, but it is no longer precise because a return"
              + " value of an unhandled function call was overapproximated. InterpreterCPA cannot"
              + " report a property violation.");
    }
  }

  /**
   * One entry of the stack of active function calls.
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

    /** The frame of the function that an execution starts in, i.e. the bottom of the stack. */
    static StackFrame initial(String pFunctionName) {
      return new StackFrame(null, pFunctionName, ImmutableMap.of(), 1);
    }

    /** Push a new frame for a call of the given function onto the given stack. */
    static StackFrame push(
        StackFrame pParent,
        String pFunctionName,
        ImmutableMap<MemoryLocation, ValueAndType> pShadowedValues) {
      checkNotNull(pParent);
      return new StackFrame(pParent, pFunctionName, pShadowedValues, pParent.depth() + 1);
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
