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
package org.sosy_lab.cpachecker.core.algorithm.cbmctools;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;

public class CBMCStackElement {

  // element id of the art element that has the conditional statement
  private int elementId;
  // true for if, false for else
  private boolean condition;
  // was this condition closed by another merge node before?
  private boolean isClosedBefore = false;

  // this is the code of this element
  private List<Object> codeList;

  public CBMCStackElement(int pElementId, String pFunctionName){
    elementId = pElementId;
    codeList = new ArrayList<Object>();
    codeList.add(pFunctionName);
  }

  public CBMCStackElement(int pElementId, AssumeEdge pEdge) {
    elementId = pElementId;
    codeList = new ArrayList<Object>();
    boolean truthAssumption = pEdge.getTruthAssumption();
    condition = truthAssumption;
  }

  public int getElementId() {
    return elementId;
  }

  public void setElementId(int pElementId) {
    elementId = pElementId;
  }

  public boolean isCondition() {
    return condition;
  }

  public void setCondition(boolean pCondition) {
    condition = pCondition;
  }

  public boolean isClosedBefore() {
    return isClosedBefore;
  }

  public void setClosedBefore(boolean pIsClosedBefore) {
    isClosedBefore = pIsClosedBefore;
  }

  public void write(Object pStatement){
    codeList.add(pStatement);
  }

  public StringBuffer getCode(){
    StringBuffer ret = new StringBuffer();

    for(Object obj: codeList){
      // check whether we have a simple statement
      // or a conditional statement
      if(obj instanceof String){
        ret.append((String)obj);
        ret.append("\n");
      }
      else if(obj instanceof CBMCLabelElement){
        ret.append(((CBMCLabelElement)obj).getCode());
      }
      else if(obj instanceof CBMCStackElement){
        ret.append(((CBMCStackElement)obj).getCode());
        ret.append("\n");
      }
      else{
        assert(false);
      }
    }

    return ret;

  }

  @Override
  public String toString() {
    return "Element id: " + elementId + " Condition: " + condition + " .. is closed " + isClosedBefore;
  }

}
