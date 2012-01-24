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
package org.sosy_lab.cpachecker.cpa.callstack;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableElement;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

public class CallstackElement implements AbstractElement, Partitionable, AbstractQueryableElement {

  private final CallstackElement previousElement;
  private final String currentFunction;
  private final CFANode callerNode;
  private final int depth;

  public CallstackElement(CallstackElement previousElement, String function, CFANode callerNode) {
    this.previousElement = previousElement;
    this.currentFunction = checkNotNull(function);
    this.callerNode = checkNotNull(callerNode);
    if (previousElement == null) {
      depth = 1;
    } else {
      depth = previousElement.getDepth() + 1;
    }
  }

  public CallstackElement getPreviousElement() {
    return previousElement;
  }

  public String getCurrentFunction() {
    return currentFunction;
  }

  public CFANode getCallNode() {
    return callerNode;
  }

  public int getDepth() {
    return depth;
  }

  @Override
  public Object getPartitionKey() {
    return this;
  }

  @Override
  public String toString() {
    return "Function " + getCurrentFunction()
         + " called from node " + getCallNode()
         + ", stack depth " + getDepth()
         + " [" + Integer.toHexString(super.hashCode()) + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof CallstackElement)) {
      return false;
    }

    CallstackElement other = (CallstackElement)obj;
    return (this.previousElement == other.previousElement)
        && (this.currentFunction.equals(other.currentFunction))
        && (this.callerNode.equals(other.callerNode));
  }

  @Override
  public int hashCode() {
    return callerNode.hashCode();
  }

  @Override
  public String getCPAName() {
    return "Callstack";
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    return false;
  }

  @Override
  public Object evaluateProperty(String pProperty) throws InvalidQueryException {
    if (pProperty.compareToIgnoreCase("caller") == 0) {
      if (callerNode != null)
        return this.callerNode.getFunctionName();
      else
        return "";
    }

    throw new InvalidQueryException(String.format("Evaluating %s not supported by %s", pProperty, this.getClass().getCanonicalName()));
  }

  @Override
  public void modifyProperty(String pModification) throws InvalidQueryException {
    throw new InvalidQueryException("modifyProperty not implemented by " + this.getClass().getCanonicalName());
  }
}
