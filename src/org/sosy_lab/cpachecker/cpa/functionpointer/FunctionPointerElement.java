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
 * @author Andreas Stahlbauer <stahlbau fim uni-passau de>
 */
public class FunctionPointerElement implements AbstractElement {

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

  private Map<String,AbstractFunctionPointerTarget> pointerVariableValues = new HashMap<String,AbstractFunctionPointerTarget>();

  public FunctionPointerElement() {
  }

  public FunctionPointerElement(FunctionPointerElement pCopyFromPreviousState) {
    if (pCopyFromPreviousState != null) {
      // should be ok because the values of the map are immutable.
      this.pointerVariableValues.putAll(pCopyFromPreviousState.pointerVariableValues);
    }
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
   * Returns the unique, scoped identifier of a variable.
   * @param declaredInsideFunctionName
   * @param localVariableName
   * @return
   */
  public String getUniqueVariableIdentifier(String declaredInsideFunctionName, String localVariableName) {
    if (declaredInsideFunctionName == null || declaredInsideFunctionName.isEmpty())
      return localVariableName;
    else
      return String.format("%s::%s", declaredInsideFunctionName, localVariableName);
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

  public void declareNewVariable(String inScopeOfFunction, String variableName) {
    String uniqueVarIdent = getUniqueVariableIdentifier(inScopeOfFunction, variableName);
    if (!pointerVariableValues.containsKey(uniqueVarIdent)) {
      pointerVariableValues.put(uniqueVarIdent, createUnknownTarget());
    }
  }

  public boolean isDeclaredLocalVariable (String inScopeOfFunction, String variableName) {
    // TODO: Enhance. This is kind of a hack.
    String uniqueLocalVarIdent = getUniqueVariableIdentifier(inScopeOfFunction, variableName);
    return pointerVariableValues.containsKey(uniqueLocalVarIdent);
  }

  public boolean isGlobalVariable (String inScopeOfFunction, String variableName) {
    // TODO: Enhance. This is kind of a hack.
    return !isDeclaredLocalVariable(inScopeOfFunction, variableName);
  }

  /**
   * Return the global identificator of a declared variable.
   * Take into account that a variable with the same name may be declared
   * global and local in a function.
   * We only have to care about function scope (because of CIL).
   * @param accessInScope
   * @param variableName
   * @return
   */
  public String getUniqueIdOfAccessedVariable (String accessInScope, String variableName) {
    if (isDeclaredLocalVariable(accessInScope, variableName))
      return getUniqueVariableIdentifier(accessInScope, variableName);
    else
      return getUniqueVariableIdentifier("", variableName);
  }

  /**
   * Add a function to the set of possible functions a specified pointer variable can point to.
   * @param assignementInsideFunction
   * @param pVariableName
   * @param pFunctionName
   */
  public void setVariablePointsTo(String assignementInsideFunction, String pVariableName, String pFunctionName) {
    String uniqueVarIdent = getUniqueIdOfAccessedVariable(assignementInsideFunction, pVariableName);
    this.pointerVariableValues.put(uniqueVarIdent, new NamedFunctionTarget(pFunctionName));
  }

  public void setVariableToBottom (String assignementInsideFunction, String variableName) {
    String uniqueVarIdent = getUniqueIdOfAccessedVariable(assignementInsideFunction, variableName);
    this.pointerVariableValues.put(uniqueVarIdent, BottomTarget.getInstance());
  }

  public void setVariableToTop (String assignementInsideFunction, String variableName) {
    String uniqueVarIdent = getUniqueIdOfAccessedVariable(assignementInsideFunction, variableName);
    this.pointerVariableValues.put(uniqueVarIdent, TopTarget.getInstance());
  }

  public void setVariableToUndefined (String assignementInsideFunction, String variableName) {
    if (handleUndefinedAsTop)
      this.setVariableToTop(assignementInsideFunction, variableName);
    else
      this.setVariableToBottom(assignementInsideFunction, variableName);
  }

  public boolean getPointsToBottom(String accessInsideFunction, String variableName) {
    String uniqueVarIdent = this.getUniqueIdOfAccessedVariable(accessInsideFunction, variableName);
    AbstractFunctionPointerTarget target = this.pointerVariableValues.get(uniqueVarIdent);
    return (target == null && !handleUndefinedAsTop) || target instanceof BottomTarget;
  }

  public boolean getPointsToTop(String accessInsideFunction, String variableName) {
    String uniqueVarIdent = this.getUniqueIdOfAccessedVariable(accessInsideFunction, variableName);
    AbstractFunctionPointerTarget target = this.pointerVariableValues.get(uniqueVarIdent);
    return ((target == null && handleUndefinedAsTop) || target instanceof TopTarget);
  }

  public void assignVariableValueFromVariable(String inScopeOfFunction, String targetVariable, String sourceVariable) {
    assignVariableValueFromVariable(inScopeOfFunction, targetVariable, inScopeOfFunction, sourceVariable);
  }

  public void assignVariableValueFromVariable(String targetFunctionScope, String targetVariable, String sourceFunctionScope, String sourceVariable) {
    if (getPointsToBottom(sourceFunctionScope, sourceVariable)) {
      setVariableToBottom(targetFunctionScope, targetVariable);
    } else if (getPointsToTop(sourceFunctionScope, sourceVariable)) {
      setVariableToTop(targetFunctionScope, targetVariable);
    } else {
      String uniqueTgtVarIdent = getUniqueIdOfAccessedVariable(targetFunctionScope, targetVariable);
      String uniqueSrcVarIdent = getUniqueIdOfAccessedVariable(sourceFunctionScope, sourceVariable);

      this.pointerVariableValues.put(uniqueTgtVarIdent, this.pointerVariableValues.get(uniqueSrcVarIdent));
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
