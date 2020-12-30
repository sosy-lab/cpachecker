// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.loopInformation;

import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class LoopVariables {

  private String variableName;
  private CFANode variableNode;
  private String variableType;
  private boolean isArray;
  private String arrayLength;
  private Integer initializationLine;

  public LoopVariables(
      String variableName,
      String variableType,
      CFANode variableNode,
      boolean isArray,
      String arrayLength,
      Integer initializationLine) {
    this.variableName = variableName;
    this.variableType = variableType;
    this.variableNode = variableNode;
    this.isArray = isArray;
    this.arrayLength = arrayLength;
    this.initializationLine = initializationLine;
  }

  public String getVariableName() {
    return variableName;
  }

  public String getVariableType() {
    return variableType;
  }

  public CFANode getVariableNode() {
    return variableNode;
  }

  public boolean getIsArray() {
    return isArray;
  }

  public String getArrayLength() {
    return arrayLength;
  }

  public Integer getInitializationLine() {
    return initializationLine;
  }

  public void setVariableName(String pVariableName) {
    variableName = pVariableName;
  }

  public void setVariableType(String pVariableType) {
    variableType = pVariableType;
  }

  public void setVariableNode(CFANode pVariableNode) {
    variableNode = pVariableNode;
  }

  public void setArray(boolean pIsArray) {
    isArray = pIsArray;
  }

  public void setArrayLength(String pArrayLength) {
    arrayLength = pArrayLength;
  }

  public void setInitializationLine(Integer pInitializationLine) {
    initializationLine = pInitializationLine;
  }

  @Override
  public String toString() {
    return "LoopVariables [variableName="
        + variableName
        + ", variableType="
        + variableType
        + ", isArray="
        + isArray
        + ", arrayLength="
        + arrayLength
        + ", initializationLine="
        + initializationLine
        + "]";
  }
}
