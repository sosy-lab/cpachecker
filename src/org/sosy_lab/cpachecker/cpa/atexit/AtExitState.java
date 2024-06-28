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

  @Override
  public boolean isLessOrEqual(AtExitState other) throws CPAException, InterruptedException {
    // We're using the prefix relation here
    // FIXME: Is this the right choice?
    PersistentStack<FunctionPointerTarget> otherStack = other.stack;
    if (stack.size() > otherStack.size()) {
      return false;
    }
    while (stack.size() < otherStack.size()) {
      otherStack = otherStack.popAndCopy();
    }
    return stack.equals(otherStack);
  }
}
