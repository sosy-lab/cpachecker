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
package org.sosy_lab.cpachecker.util.cwriter;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;

class BasicBlock {

  private static final String SINGLE_INDENT = "  ";

  // element id of the art element that has the conditional statement
  private final int elementId;
  // true for if, false for else
  private boolean condition;
  // was this condition closed by another merge node before?
  private boolean isClosedBefore = false;

  // this is the code of this element
  private final String firstCodeLine;
  private final List<Object> codeList;

  public BasicBlock(int pElementId, String pFunctionName) {
    elementId = pElementId;
    codeList = new ArrayList<Object>();
    firstCodeLine = pFunctionName;
  }

  public BasicBlock(int pElementId, AssumeEdge pEdge, String pConditionString) {
    elementId = pElementId;
    codeList = new ArrayList<Object>();
    boolean truthAssumption = pEdge.getTruthAssumption();
    condition = truthAssumption;
    firstCodeLine = pConditionString;
  }

  public int getElementId() {
    return elementId;
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
    if (   !(pStatement instanceof String)
        || !((String)pStatement).isEmpty()) {
      codeList.add(pStatement);
    }
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

    for (Object obj: codeList) {
      // check whether we have a simple statement
      // or a conditional statement
      if (obj instanceof String) {
        ret.append(indent);
        ret.append((String)obj);
      } else if (obj instanceof BasicBlock) {
        ret.append(((BasicBlock)obj).getCode(indent));
      } else {
        assert false;
      }
      ret.append("\n");
    }

    ret.append(pIndent);
    ret.append("}\n");
    return ret;
  }

  @Override
  public String toString() {
    return "Element id: " + elementId + " Condition: " + condition + " .. is closed " + isClosedBefore;
  }
}