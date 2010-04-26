/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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

import java.util.Stack;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;

public class CBMCEdge implements Comparable<CBMCEdge>{

  private ARTElement parentElement;
  private ARTElement childElement;
  private CFAEdge edge;
  private Stack<Stack<CBMCStackElement>> stack;

  public CBMCEdge(ARTElement pParentElement, ARTElement pChildElement,
      CFAEdge pEdge, Stack<Stack<CBMCStackElement>> pStack) {
    parentElement = pParentElement;
    childElement = pChildElement;
    edge = pEdge;
    stack = pStack;
  }

  public ARTElement getParentElement() {
    return parentElement;
  }

  public void setParentElement(ARTElement pParentElement) {
    parentElement = pParentElement;
  }

  public ARTElement getChildElement() {
    return childElement;
  }

  public void setChildElement(ARTElement pChildElement) {
    childElement = pChildElement;
  }

  public CFAEdge getEdge() {
    return edge;
  }

  public void setEdge(CFAEdge pEdge) {
    edge = pEdge;
  }

  public void pushToStack(Stack<CBMCStackElement> pStack){
    stack.push(pStack);
  }

  public Stack<Stack<CBMCStackElement>> getStack() {
    return stack;
  }

  public void writeToTheLastStackElement(Object pStatement){
    Stack<CBMCStackElement> lastFunctionStack = stack.peek();
    CBMCStackElement lastStackElement = lastFunctionStack.peek();
    lastStackElement.write(pStatement);
  }

  @Override
  /** comparison based on the child element*/
  public int compareTo(CBMCEdge pO) {
    int otherElementId = pO.getChildElement().getElementId();
    int thisElementId = this.getChildElement().getElementId();

    if(thisElementId > otherElementId){
      return 1;
    }
    else if(thisElementId < otherElementId){
      return -1;
    }
    return 0;
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    } else if (pObj instanceof CBMCEdge) {
      int otherElementId = ((CBMCEdge)pObj).getChildElement().getElementId();
      int thisElementId = this.getChildElement().getElementId();
      return thisElementId == otherElementId;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return getChildElement().getElementId();
  }

  @Override
  public String toString() {
    return "CBMC ELEMENT > " + parentElement.getElementId() + " --> " + childElement.getElementId();
  }

}
