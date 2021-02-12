// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.loopInformation;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class LoopVariables {

  private AExpression variable;
  private CFANode variableNode;
  private boolean isArray;
  private Integer arrayLength;
  private Integer initializationLine;

  public LoopVariables(
      AExpression variable,
      CFANode variableNode,
      boolean isArray,
      Integer arrayLength,
      Integer initializationLine) {
    this.variable = variable;
    this.variableNode = variableNode;
    this.isArray = isArray;
    this.arrayLength = arrayLength;
    this.initializationLine = initializationLine;
  }

  public AExpression getVariableExpression() {
    return variable;
  }

  public String getVariableNameAsString() {
    String name = null;
    if (variable instanceof CIdExpression) {
      name = ((CIdExpression) variable).getName();
    } else if (variable instanceof CArraySubscriptExpression) {
      name = ((CArraySubscriptExpression) variable).getArrayExpression().toString();
    }
    return name;
  }

  public String getVariableTypeAsString() {
    String type = null;
    if(variable instanceof CIdExpression) {
      type = ((CIdExpression)variable).getExpressionType().toString();
    }else if(variable instanceof CArraySubscriptExpression) {
      type = ((CArraySubscriptExpression) variable).getExpressionType().toString();
    }
    return type;
  }

  public CFANode getVariableNode() {
    return variableNode;
  }

  public boolean getIsArray() {
    return isArray;
  }

  public Integer getArrayLength() {
    return arrayLength;
  }

  public Integer getInitializationLine() {
    return initializationLine;
  }

  public void setVariableExpression(AExpression pVariableName) {
    variable = pVariableName;
  }

  public void setVariableNode(CFANode pVariableNode) {
    variableNode = pVariableNode;
  }

  public void setArray(boolean pIsArray) {
    isArray = pIsArray;
  }

  public void setArrayLength(Integer pArrayLength) {
    arrayLength = pArrayLength;
  }

  public void setInitializationLine(Integer pInitializationLine) {
    initializationLine = pInitializationLine;
  }

  @Override
  public String toString() {
    return "LoopVariables [variableName="
        + getVariableNameAsString()
        + ", variableType="
        + getVariableTypeAsString()
        + ", isArray="
        + isArray
        + ", arrayLength="
        + arrayLength
        + ", initializationLine="
        + initializationLine
        + "]";
  }
}
