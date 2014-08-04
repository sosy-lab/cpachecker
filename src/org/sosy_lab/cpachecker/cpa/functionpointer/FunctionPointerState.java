/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.functionpointer;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.Serializable;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import com.google.common.base.Joiner;

/**
 * Represents one abstract state of the FunctionPointer CPA.
 */
class FunctionPointerState implements AbstractState, Serializable {

  private static final long serialVersionUID = -1951853216031911649L;

  // java reference counting + immutable objects should help us
  // to reduce memory consumption.
  static abstract class FunctionPointerTarget {
  }

  static final class UnknownTarget extends FunctionPointerTarget {
    private static final UnknownTarget instance = new UnknownTarget();

    private UnknownTarget() { }

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
      return toString().hashCode();
    }
  }

  static final class InvalidTarget extends FunctionPointerTarget implements Serializable {
    private static final long serialVersionUID = 7067934518471075538L;
    private static final InvalidTarget instance = new InvalidTarget();

    private InvalidTarget() { }

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
      return toString().hashCode();
    }
  }

  static final class NamedFunctionTarget extends FunctionPointerTarget implements Serializable{

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
      return firstNonNull(values.get(variableName), UnknownTarget.getInstance());
    }

    void setTarget(String variableName, FunctionPointerTarget target) {
      if (target == UnknownTarget.getInstance()) {
        values = values.removeAndCopy(variableName);
      } else {
        values = values.putAndCopy(variableName, target);
      }
    }

    void clearVariablesWithPrefix(String prefix) {
      for (String var : values.keySet()) {
        if (var.startsWith(prefix)) {
          values = values.removeAndCopy(var);
        }
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

  private FunctionPointerState() {
    pointerVariableValues = PathCopyingPersistentTreeMap.of();
  }

  private FunctionPointerState(PersistentSortedMap<String, FunctionPointerTarget> pValues) {
    pointerVariableValues = pValues;
  }

  private static final FunctionPointerState EMPTY_STATE = new FunctionPointerState(PathCopyingPersistentTreeMap.<String, FunctionPointerState.FunctionPointerTarget>of());

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

  public FunctionPointerTarget getTarget(String variableName) {
    // default to UNKNOWN
    return firstNonNull(pointerVariableValues.get(variableName), UnknownTarget.getInstance());
  }

  public boolean isLessOrEqualThan(FunctionPointerState pElement) {
    // check if the other map is a subset of this map

    if (this.pointerVariableValues.size() < pElement.pointerVariableValues.size()) {
      return false;
    }

    return this.pointerVariableValues.entrySet().containsAll(pElement.pointerVariableValues.entrySet());
  }


  // Needed for NoOpReducer
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
