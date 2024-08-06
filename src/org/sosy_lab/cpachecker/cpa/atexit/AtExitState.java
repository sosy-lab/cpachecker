// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.atexit;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.FunctionPointerTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.UnknownTarget;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.smg.datastructures.PersistentStack;

/**
 * State for the atexit CPA
 *
 * <p>Keeps track of the functions that were registered with atexit() by storing them on a stack.
 */
public class AtExitState implements LatticeAbstractState<AtExitState>, Graphable {
  private final PersistentStack<FunctionPointerTarget> stack;

  private AtExitState(PersistentStack<FunctionPointerTarget> pStack) {
    stack = pStack;
  }

  public static AtExitState createEmptyState() {
    return new AtExitState(PersistentStack.of());
  }

  /** Add a new atexit handler to the stack and return the updated state. */
  public AtExitState push(FunctionPointerTarget pFunction) {
    return new AtExitState(stack.pushAndCopy(pFunction));
  }

  public FunctionPointerTarget peek() {
    return stack.peek();
  }

  public AtExitState pop() {
    return new AtExitState(stack.popAndCopy());
  }

  public boolean isEmpty() {
    return stack.isEmpty();
  }

  @Override
  public boolean equals(@Nullable Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof AtExitState other && stack.equals(other.stack);
  }

  @Override
  public int hashCode() {
    return stack.hashCode();
  }

  @Override
  public String toString() {
    return stack.toString();
  }

  @Override
  public String toDOTLabel() {
    return stack.isEmpty() ? "" : toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public AtExitState join(AtExitState other) throws CPAException, InterruptedException {
    // TODO: Return the largest common prefix?
    throw new UnsupportedOperationException();
  }

  private boolean subsumes(FunctionPointerTarget t1, FunctionPointerTarget t2) {
    return t1.equals(t2) || t2.equals(UnknownTarget.getInstance());
  }

  @Override
  public boolean isLessOrEqual(AtExitState other) throws CPAException, InterruptedException {
    // We use "unknown target" as a top element for function pointers and then assume that there is
    // a trivial function in the program with an empty body. This allows us to compare function
    // pointer stacks of different sizes if the last elements are all unknown.
    // FIXME: Make sure that this works with the heuristic that the function pointer CPA uses to
    //   pick its possible targets.

    // We need to check if s1 < s2 holds
    PersistentStack<FunctionPointerTarget> s1 = stack;
    PersistentStack<FunctionPointerTarget> s2 = other.stack;

    // Remove "unknown" pointers at the end of s1
    while (!s1.isEmpty() && s1.peek().equals(UnknownTarget.getInstance())) {
      s1 = s1.popAndCopy();
    }

    // If the remaining prefix of s1 is still greater than s2, return false
    if (s1.size() > s2.size()) {
      return false;
    }

    // Otherwise, trim s2 down to the size of s1. If any of the "excess" pointers is not the
    // "unknown" target, return false
    while (s1.size() < s2.size()) {
      if (!s2.peek().equals(UnknownTarget.getInstance())) {
        return false;
      }
      s2 = s2.popAndCopy();
    }

    // Check the remaining sequence and return false if any of the pointers in s1 is not subsumed
    // by the corresponding pointer in s2
    while (!s1.isEmpty()) {
      if (!subsumes(s1.peek(), s2.peek())) {
        return false;
      }
      s1 = s1.popAndCopy();
      s2 = s2.popAndCopy();
    }

    // If we got through all this, return true
    return true;
  }
}
