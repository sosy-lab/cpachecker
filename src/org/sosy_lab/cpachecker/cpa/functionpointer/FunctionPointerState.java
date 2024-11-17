// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.functionpointer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.base.Joiner;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.smg.datastructures.PersistentStack;

/** Represents one abstract state of the FunctionPointer CPA. */
public class FunctionPointerState
    implements LatticeAbstractState<FunctionPointerState>, Serializable, Graphable {

  @Serial private static final long serialVersionUID = -1951853216031911649L;

  public interface FunctionPointerTarget {}

  public static final class UnknownTarget implements FunctionPointerTarget {
    private static final UnknownTarget instance = new UnknownTarget();

    private UnknownTarget() {}

    @Override
    public String toString() {
      return "UNKNOWN";
    }

    public static UnknownTarget getInstance() {
      return instance;
    }

    @Override
    public boolean equals(Object pObj) {
      return pObj instanceof UnknownTarget;
    }

    @Override
    public int hashCode() {
      return 0; // some constant value is sufficient
    }
  }

  public static final class InvalidTarget implements FunctionPointerTarget, Serializable {
    @Serial private static final long serialVersionUID = 7067934518471075538L;
    private static final InvalidTarget instance = new InvalidTarget();

    private InvalidTarget() {}

    @Override
    public String toString() {
      return "INVALID";
    }

    public static InvalidTarget getInstance() {
      return instance;
    }

    @Override
    public boolean equals(Object pObj) {
      return pObj instanceof InvalidTarget;
    }

    @Override
    public int hashCode() {
      return 1; // some constant value is sufficient
    }
  }

  public static final class NamedFunctionTarget implements FunctionPointerTarget, Serializable {

    @Serial private static final long serialVersionUID = 9001748459212617220L;
    private final String functionName;

    public NamedFunctionTarget(String pFunctionName) {
      checkArgument(!isNullOrEmpty(pFunctionName));
      functionName = pFunctionName;
    }

    public String getFunctionName() {
      return functionName;
    }

    @Override
    public String toString() {
      return getFunctionName();
    }

    @Override
    public boolean equals(Object pObj) {
      return pObj instanceof NamedFunctionTarget
          && ((NamedFunctionTarget) pObj).functionName.equals(functionName);
    }

    @Override
    public int hashCode() {
      return functionName.hashCode();
    }
  }

  public static final class NullTarget implements FunctionPointerTarget, Serializable {
    @Serial private static final long serialVersionUID = 4816211908759149770L;
    private static final NullTarget instance = new NullTarget();

    private NullTarget() {}

    @Override
    public String toString() {
      return "0";
    }

    public static NullTarget getInstance() {
      return instance;
    }

    @Override
    public boolean equals(Object pObj) {
      return pObj instanceof NullTarget;
    }

    @Override
    public int hashCode() {
      return toString().hashCode();
    }
  }

  public static class Builder {
    private final FunctionPointerState oldState;

    private PersistentSortedMap<String, FunctionPointerTarget> values;
    private PersistentStack<FunctionPointerTarget> stack;

    private Builder(FunctionPointerState pOldState) {
      oldState = checkNotNull(pOldState);
      values = oldState.pointerVariableValues;
      stack = oldState.atExitStack;
    }

    /** Returns all tracked variables */
    public Set<String> getValues() {
      return values.keySet();
    }

    /**
     * Returns target for a (function pointer) variable
     *
     * <p>Default to {@link UnknownTarget} if the variable is not tracked
     */
    public FunctionPointerTarget getTarget(String variableName) {
      return values.getOrDefault(variableName, UnknownTarget.getInstance());
    }

    /* Set the target of a (function pointer) variable */
    public void setTarget(String variableName, FunctionPointerTarget target) {
      if (target == UnknownTarget.getInstance()) {
        values = values.removeAndCopy(variableName);
      } else {
        values = values.putAndCopy(variableName, target);
      }
    }

    /** Remove all local variables */
    void clearVariablesForFunction(String function) {
      for (String var : CFAUtils.filterVariablesOfFunction(values.keySet(), function)) {
        values = values.removeAndCopy(var);
      }
    }

    /** Returns <code>true</code> if the <code>atexit()</code> stack is empty */
    public boolean isStackEmpty() {
      return stack.isEmpty();
    }

    /** Push a function pointer onto the <code>atexit()</code> stack */
    public void pushTarget(FunctionPointerTarget pFunction) {
      stack = stack.pushAndCopy(pFunction);
    }

    /**
     * Pop a function pointer from the <code>atexit()</code> stack
     *
     * <p>Returns {@link InvalidTarget} if the stack is empty
     */
    public FunctionPointerTarget popTarget() {
      if (stack.isEmpty()) {
        return InvalidTarget.getInstance();
      } else {
        FunctionPointerTarget r = stack.peek();
        stack = stack.popAndCopy();
        return r;
      }
    }

    public FunctionPointerState build() {
      if (values == oldState.pointerVariableValues && stack == oldState.atExitStack) {
        return oldState;
      } else if (values.isEmpty() && stack.isEmpty()) {
        return EMPTY_STATE;
      }
      return new FunctionPointerState(values, stack);
    }
  }

  /**
   * Tracked variables with their targets
   *
   * <p>This map should never contain {@link UnknownTarget} values
   */
  private final PersistentSortedMap<String, FunctionPointerTarget> pointerVariableValues;

  /** Stack with <code>atexit</code> handlers */
  private final PersistentStack<FunctionPointerTarget> atExitStack;

  /** Cached value of {@link #hashCode()} */
  private transient int hashCode;

  private FunctionPointerState(
      PersistentSortedMap<String, FunctionPointerTarget> pValues,
      PersistentStack<FunctionPointerTarget> pAtExitStack) {
    pointerVariableValues = pValues;
    atExitStack = pAtExitStack;
  }

  private static final FunctionPointerState EMPTY_STATE =
      new FunctionPointerState(PathCopyingPersistentTreeMap.of(), PersistentStack.of());

  public static FunctionPointerState createEmptyState() {
    return EMPTY_STATE;
  }

  public FunctionPointerState.Builder createBuilder() {
    return new Builder(this);
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    str.append("{");
    Joiner.on(", ").withKeyValueSeparator("=").appendTo(str, pointerVariableValues);
    str.append("}");
    str.append("[");
    Joiner.on(", ").appendTo(str, atExitStack);
    str.append("]");
    return str.toString();
  }

  @Override
  public String toDOTLabel() {
    return pointerVariableValues.isEmpty() && atExitStack.isEmpty() ? "" : toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  /**
   * Return the target of a (function pointer) variable
   *
   * <p>Defaults to {@link FunctionPointerState.UnknownTarget} if the variable is unknown
   */
  public FunctionPointerTarget getTarget(String variableName) {
    return pointerVariableValues.getOrDefault(variableName, UnknownTarget.getInstance());
  }

  Set<String> getTrackedVariables() {
    return pointerVariableValues.keySet();
  }

  private boolean subsumes(FunctionPointerTarget t1, FunctionPointerTarget t2) {
    return t1.equals(t2) || t2.equals(UnknownTarget.getInstance());
  }

  @Override
  public boolean isLessOrEqual(FunctionPointerState pElement) {
    // check if the other map is a subset of this map
    if (pointerVariableValues.size() < pElement.pointerVariableValues.size()
        || !pointerVariableValues
            .entrySet()
            .containsAll(pElement.pointerVariableValues.entrySet())) {
      return false;
    }

    // Now compare the atexit stacks and see if state1.atExitStack < s2.atExitStack holds
    PersistentStack<FunctionPointerTarget> s1 = atExitStack;
    PersistentStack<FunctionPointerTarget> s2 = pElement.atExitStack;

    // We use "unknown target" as a top element for function pointers and then assume that there is
    // a trivial function in the program with an empty body. This allows us to compare function
    // pointer stacks of different sizes if the last elements are all unknown.
    // TODO: Is this actually correct?

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

  @Override
  public FunctionPointerState join(FunctionPointerState other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    return pObj instanceof FunctionPointerState otherState
        && pointerVariableValues.equals(otherState.pointerVariableValues)
        && atExitStack.equals(otherState.atExitStack);
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      hashCode = Objects.hash(pointerVariableValues, atExitStack);
    }
    return hashCode;
  }
}
