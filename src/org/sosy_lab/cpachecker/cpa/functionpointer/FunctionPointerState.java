/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.IOException;
import java.io.NotSerializableException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * Represents one abstract state of the FunctionPointer CPA.
 */
class FunctionPointerState extends AbstractSingleWrapperState  {
  /* Boilerplate code to avoid serializing this class */
  private static final long serialVersionUID = 0xDEADBEEF;
  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    throw new NotSerializableException();
  }

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

  static final class InvalidTarget extends FunctionPointerTarget {
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

  static final class NamedFunctionTarget extends FunctionPointerTarget {

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

    private final AbstractState wrappedElement;
    private Map<String,FunctionPointerTarget> values = null;
    private final ImmutableMap<String,FunctionPointerTarget> oldValues;

    private Builder(ImmutableMap<String, FunctionPointerTarget> pOldValues, AbstractState pWrappedElement) {
      oldValues = pOldValues;
      wrappedElement = pWrappedElement;
    }

    private void setupMaps() {
      if (values == null) {
        values = Maps.newHashMap(oldValues);
      }
    }

    public FunctionPointerTarget getTarget(String variableName) {
      // default to UNKNOWN
      Map<String, FunctionPointerTarget> map = Objects.firstNonNull(values, oldValues);
      return Objects.firstNonNull(map.get(variableName), UnknownTarget.getInstance());
    }

    void setTarget(String variableName, FunctionPointerTarget target) {
      setupMaps();

      if (target == UnknownTarget.getInstance()) {
        values.remove(target);
      } else {
        values.put(variableName, target);
      }
    }

    void clearVariablesWithPrefix(String prefix) {
      setupMaps();

      Iterator<String> it = values.keySet().iterator();

      while (it.hasNext()) {
        if (it.next().startsWith(prefix)) {
          it.remove();
        }
      }
    }

    FunctionPointerState build() {
      Map<String, FunctionPointerTarget> map = Objects.firstNonNull(values, oldValues);
      return new FunctionPointerState(wrappedElement, map);
    }
  }

  // This map should never contain UnknownTargets.
  private final ImmutableMap<String,FunctionPointerTarget> pointerVariableValues;

  private FunctionPointerState(AbstractState pWrappedElement) {
    super(pWrappedElement);
    pointerVariableValues = ImmutableMap.of();
  }

  private FunctionPointerState(AbstractState pWrappedElement, Map<String, FunctionPointerTarget> pValues) {
    super(pWrappedElement);
    pointerVariableValues = ImmutableMap.copyOf(pValues);
  }

  public static FunctionPointerState createEmptyState(AbstractState pWrappedElement) {
    return new FunctionPointerState(pWrappedElement);
  }

  public FunctionPointerState.Builder createBuilderWithNewWrappedState(AbstractState pElement) {
    return new Builder(this.pointerVariableValues, pElement);
  }

  public FunctionPointerState createDuplicateWithNewWrappedState(AbstractState pElement) {
    return new FunctionPointerState(pElement, this.pointerVariableValues);
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    str.append("\n FunctionPointerState: [");
    Joiner.on(", ").withKeyValueSeparator("=").appendTo(str, pointerVariableValues);
    str.append("]\n ");
    str.append(getWrappedState());
    return str.toString();
  }

  public FunctionPointerTarget getTarget(String variableName) {
    // default to UNKNOWN
    return Objects.firstNonNull(pointerVariableValues.get(variableName), UnknownTarget.getInstance());
  }

  public boolean isLessOrEqualThan(FunctionPointerState pElement) {
    // check if the other map is a subset of this map

    if (this.pointerVariableValues.size() < pElement.pointerVariableValues.size()) {
      return false;
    }

    for (Entry<String, FunctionPointerTarget> entry : pElement.pointerVariableValues.entrySet()) {
      FunctionPointerTarget thisTarget = this.pointerVariableValues.get(entry.getKey());

      if (!entry.getValue().equals(thisTarget)) {
        return false;
      }
    }

    return true;
  }

  Map<String, FunctionPointerTarget> getTargetMap() {
    return pointerVariableValues;
  }
}
