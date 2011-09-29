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

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

/**
 * Represents one abstract state of the FunctionPointer CPA.
 */
class FunctionPointerElement implements AbstractElement {

  // java reference counting + immutable objects should help us
  // to reduce memory consumption.
  private static abstract class AbstractFunctionPointerTarget {
  }

  public static boolean handleUndefinedAsTop = true;

  private static final class BottomTarget extends AbstractFunctionPointerTarget {
    private static final BottomTarget instance = new BottomTarget();

    @Override
    public String toString() {
      return "BOTTOM";
    }

    public static BottomTarget getInstance() {
      return instance;
    }
  }

  private static final class TopTarget extends AbstractFunctionPointerTarget {
    private static final TopTarget instance = new TopTarget();

    @Override
    public String toString() {
      return "TOP";
    }

    public static TopTarget getInstance() {
      return instance;
    }
  }

  private static final class NamedFunctionTarget extends AbstractFunctionPointerTarget {
    private final String functionName;
    public NamedFunctionTarget(String pFunctionName) {
      this.functionName = pFunctionName;
    }
    public String getFunctionName() {
      return this.functionName;
    }
    @Override
    public String toString() {
      return this.getFunctionName();
    }
  }

  private final Map<String,AbstractFunctionPointerTarget> pointerVariableValues = new HashMap<String,AbstractFunctionPointerTarget>();

  public FunctionPointerElement() {
  }

  private FunctionPointerElement(FunctionPointerElement pCopyFromPreviousState) {
    this.pointerVariableValues.putAll(pCopyFromPreviousState.pointerVariableValues);
  }

  public FunctionPointerElement createDuplicate() {
    return new FunctionPointerElement(this);
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    for (String variableName : pointerVariableValues.keySet()) {
      AbstractFunctionPointerTarget target = pointerVariableValues.get(variableName);
      str.append(String.format("%s <= %s\n", variableName, target));
    }

    return str.toString();
  }

  /**
   * If a function pointer is not defined explicit it could point to any function.
   * @return
   */
  private AbstractFunctionPointerTarget createUnknownTarget() {
    if (handleUndefinedAsTop)
      return TopTarget.getInstance();
    else
      return BottomTarget.getInstance();
  }

  public void declareNewVariable(String variableName) {
    if (!pointerVariableValues.containsKey(variableName)) {
      pointerVariableValues.put(variableName, createUnknownTarget());
    }
  }

  /**
   * Add a function to the set of possible functions a specified pointer variable can point to.
   * @param assignementInsideFunction
   * @param pVariableName
   * @param pFunctionName
   */
  public void setVariablePointsTo(String pVariableName, String pFunctionName) {
    this.pointerVariableValues.put(pVariableName, new NamedFunctionTarget(pFunctionName));
  }

  public void setVariableToBottom(String variableName) {
    this.pointerVariableValues.put(variableName, BottomTarget.getInstance());
  }

  public void setVariableToTop(String variableName) {
    this.pointerVariableValues.put(variableName, TopTarget.getInstance());
  }

  public void setVariableToUndefined(String variableName) {
    if (handleUndefinedAsTop)
      this.setVariableToTop(variableName);
    else
      this.setVariableToBottom(variableName);
  }

  public boolean getPointsToBottom(String variableName) {
    AbstractFunctionPointerTarget target = this.pointerVariableValues.get(variableName);
    return (target == null && !handleUndefinedAsTop) || target instanceof BottomTarget;
  }

  public boolean getPointsToTop(String variableName) {
    AbstractFunctionPointerTarget target = this.pointerVariableValues.get(variableName);
    return ((target == null && handleUndefinedAsTop) || target instanceof TopTarget);
  }

  public void assignVariableValueFromVariable(String targetVariable, String sourceVariable) {
    AbstractFunctionPointerTarget target = pointerVariableValues.get(sourceVariable);
    if (target != null) {
      pointerVariableValues.put(targetVariable, target);
    } else {
      pointerVariableValues.remove(targetVariable);
    }
  }

  public boolean isLessOrEqualThan(FunctionPointerElement pElement) {
    //TODO: Test this method!

    for (String uniqueFnId: this.pointerVariableValues.keySet()) {
      if (!pElement.pointerVariableValues.containsKey(uniqueFnId)) {
        return false;
      } else {
        AbstractFunctionPointerTarget thisPointsTo = this.pointerVariableValues.get(uniqueFnId);
        AbstractFunctionPointerTarget otherPointsTo = pElement.pointerVariableValues.get(uniqueFnId);
        boolean thisPointsToTop = thisPointsTo instanceof TopTarget || (thisPointsTo == null && handleUndefinedAsTop);
        boolean otherPointsToBottom = otherPointsTo instanceof BottomTarget || (otherPointsTo == null && !handleUndefinedAsTop);

        if (thisPointsToTop && otherPointsTo instanceof NamedFunctionTarget)
          return false;

        if (thisPointsTo instanceof NamedFunctionTarget && otherPointsToBottom)
          return false;
      }
    }


    return true;
  }
}
