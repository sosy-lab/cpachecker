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
import java.io.Serializable;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.util.CFAUtils;

/** Represents one abstract state of the FunctionPointer CPA. */
public class FunctionPointerState
    implements LatticeAbstractState<FunctionPointerState>, Serializable, Graphable {

  private static final long serialVersionUID = -1951853216031911649L;

  interface FunctionPointerTarget {}

  static final class UnknownTarget implements FunctionPointerTarget {
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

  static final class InvalidTarget implements FunctionPointerTarget, Serializable {
    private static final long serialVersionUID = 7067934518471075538L;
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

  static final class NamedFunctionTarget implements FunctionPointerTarget, Serializable {

    private static final long serialVersionUID = 9001748459212617220L;
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
          && ((NamedFunctionTarget)pObj).functionName.equals(this.functionName);
    }

    @Override
    public int hashCode() {
      return functionName.hashCode();
    }
  }

  static class Builder {

    private final FunctionPointerState oldState;
    private PersistentSortedMap<String, FunctionPointerTarget> values;

    private Builder(FunctionPointerState pOldState) {
      oldState = checkNotNull(pOldState);
      values = oldState.pointerVariableValues;
    }

    public FunctionPointerTarget getTarget(String variableName) {
      // default to UNKNOWN
      return values.getOrDefault(variableName, UnknownTarget.getInstance());
    }

    void setTarget(String variableName, FunctionPointerTarget target) {
      if (target == UnknownTarget.getInstance()) {
        values = values.removeAndCopy(variableName);
      } else {
        values = values.putAndCopy(variableName, target);
      }
    }

    void clearVariablesForFunction(String function) {
      for (String var : CFAUtils.filterVariablesOfFunction(values.keySet(), function)) {
        values = values.removeAndCopy(var);
      }
    }

    FunctionPointerState build() {
      if (values == oldState.pointerVariableValues) {
        return oldState;
      } else if (values.isEmpty()) {
        return EMPTY_STATE;
      }
      return new FunctionPointerState(values);
    }
  }

  // This map should never contain UnknownTargets.
  private final PersistentSortedMap<String, FunctionPointerTarget> pointerVariableValues;

  // cache hashCode
  private transient int hashCode;

  private FunctionPointerState(PersistentSortedMap<String, FunctionPointerTarget> pValues) {
    pointerVariableValues = pValues;
  }

  private static final FunctionPointerState EMPTY_STATE =
      new FunctionPointerState(PathCopyingPersistentTreeMap.of());

  public static FunctionPointerState createEmptyState() {
    return EMPTY_STATE;
  }

  public FunctionPointerState.Builder createBuilder() {
    return new Builder(this);
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    str.append("[");
    Joiner.on(", ").withKeyValueSeparator("=").appendTo(str, pointerVariableValues);
    str.append("]");
    return str.toString();
  }

  @Override
  public String toDOTLabel() {
    return pointerVariableValues.isEmpty() ? "" : toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  public FunctionPointerTarget getTarget(String variableName) {
    // default to UNKNOWN
    return pointerVariableValues.getOrDefault(variableName, UnknownTarget.getInstance());
  }

  Set<String> getTrackedVariables() {
    return pointerVariableValues.keySet();
  }

  @Override
  public boolean isLessOrEqual(FunctionPointerState pElement) {
    // check if the other map is a subset of this map

    if (this.pointerVariableValues.size() < pElement.pointerVariableValues.size()) {
      return false;
    }
    return this.pointerVariableValues.entrySet().containsAll(pElement.pointerVariableValues.entrySet());
  }

  @Override
  public FunctionPointerState join(FunctionPointerState other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    } else if (!(pObj instanceof FunctionPointerState)) {
      return false;
    }
    return this.pointerVariableValues.equals(((FunctionPointerState)pObj).pointerVariableValues);
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      hashCode = pointerVariableValues.hashCode();
    }
    return hashCode;
  }


}
