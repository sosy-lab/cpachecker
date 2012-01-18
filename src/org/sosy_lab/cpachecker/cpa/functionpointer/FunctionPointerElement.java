/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;

/**
 * Represents one abstract state of the FunctionPointer CPA.
 */
class FunctionPointerElement extends AbstractSingleWrapperElement  {

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

  // this map should never contain UnknownTargets
  private final Map<String,FunctionPointerTarget> pointerVariableValues;

  private FunctionPointerElement(AbstractElement pWrappedElement) {
    super(pWrappedElement);
    pointerVariableValues = new HashMap<String,FunctionPointerTarget>(0);
  }

  private FunctionPointerElement(FunctionPointerElement pCopyFromPreviousState, AbstractElement pWrappedElement) {
    super(pWrappedElement);
    pointerVariableValues = new HashMap<String,FunctionPointerTarget>(pCopyFromPreviousState.pointerVariableValues);
  }

  public static FunctionPointerElement createEmptyElement(AbstractElement pWrappedElement) {
    return new FunctionPointerElement(pWrappedElement);
  }

  public FunctionPointerElement createDuplicateWithNewWrappedElement(AbstractElement pElement) {
    return new FunctionPointerElement(this, pElement);
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    str.append("\n FunctionPointerElement: [");
    Joiner.on(", ").withKeyValueSeparator("=").appendTo(str, pointerVariableValues);
    str.append("]\n ");
    str.append(getWrappedElement());
    return str.toString();
  }

  public FunctionPointerTarget getTarget(String variableName) {
    // default to UNKNOWN
    return Objects.firstNonNull(pointerVariableValues.get(variableName), UnknownTarget.getInstance());
  }

  public void setTarget(String variableName, FunctionPointerTarget target) {
    if (target == UnknownTarget.getInstance()) {
      pointerVariableValues.remove(variableName);
    } else {
      pointerVariableValues.put(variableName, target);
    }
  }

  public void clearVariablesWithPrefix(String prefix) {
    Iterator<String> it = pointerVariableValues.keySet().iterator();

    while (it.hasNext()) {
      if (it.next().startsWith(prefix)) {
        it.remove();
      }
    }
  }

  public boolean isLessOrEqualThan(FunctionPointerElement pElement) {
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
