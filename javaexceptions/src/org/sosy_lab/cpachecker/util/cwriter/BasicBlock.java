// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;

class BasicBlock {

  private static final String SINGLE_INDENT = "  ";

  /** element id of the ARG element that has the conditional statement */
  private final int stateId;

  /** true for if, false for else */
  private boolean condition;

  /** flag to determine whether this condition was closed by another merge node before */
  private boolean isClosedBefore = false;

  /** the set of declarations already contained in this block */
  private Set<ADeclarationEdge> declarations = new HashSet<>();

  // this is the code of this element
  private final String firstCodeLine;
  private final List<Object> codeList;

  public BasicBlock(int pElementId, String pFunctionName) {
    stateId = pElementId;
    codeList = new ArrayList<>();
    firstCodeLine = pFunctionName;
  }

  public BasicBlock(int pElementId, CAssumeEdge pEdge, String pConditionString) {
    stateId = pElementId;
    codeList = new ArrayList<>();
    condition = pEdge.getTruthAssumption();
    firstCodeLine = pConditionString;
  }

  public int getStateId() {
    return stateId;
  }

  public boolean isCondition() {
    return condition;
  }

  public boolean isClosedBefore() {
    return isClosedBefore;
  }

  public void setClosedBefore(boolean pIsClosedBefore) {
    isClosedBefore = pIsClosedBefore;
  }

  public void write(Object pStatement) {
    if (!(pStatement instanceof String) || !((String) pStatement).isEmpty()) {
      codeList.add(pStatement);
    }
  }

  void addDeclaration(ADeclarationEdge declarationEdge) {
    declarations.add(declarationEdge);
  }

  /**
   * This method checks whether or nor the given declaration is already part of this block.
   *
   * <p>This is needed, as some tools (e.g. llbmc, i.e. clang) do not allow re-declaration of a
   * previously declared variable.
   *
   * @param declarationEdge the edge to check
   * @return true, if the given declaration is already part of this block, else false
   */
  boolean hasDeclaration(ADeclarationEdge declarationEdge) {
    return declarations.contains(declarationEdge);
  }

  public String getCode() {
    return getCode("").toString();
  }

  private StringBuilder getCode(String pIndent) {
    StringBuilder ret = new StringBuilder();

    ret.append(pIndent);
    ret.append(firstCodeLine);
    ret.append(" {\n");

    String indent = pIndent + SINGLE_INDENT;

    for (Object obj : codeList) {
      // check whether we have a simple statement
      // or a conditional statement
      if (obj instanceof String) {
        ret.append(indent);
        ret.append((String) obj);
      } else if (obj instanceof BasicBlock) {
        ret.append(((BasicBlock) obj).getCode(indent));
      } else {
        throw new AssertionError();
      }
      ret.append("\n");
    }

    ret.append(pIndent);
    ret.append("}\n");
    return ret;
  }

  @Override
  public String toString() {
    return "Element id: "
        + stateId
        + " Condition: "
        + condition
        + " .. is closed "
        + isClosedBefore;
  }
}
