/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.callstackPCC;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.Serializable;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;


public class CallstackPccState implements AbstractState, Partitionable, AbstractQueryableState, Serializable {

  private static final long serialVersionUID = -7480771410097904647L;
  private final CallstackPccState previousState;
  private final String currentFunction;
  private transient CFANode callerNode;
  private final int depth;

  public CallstackPccState(CallstackPccState previousElement, String function, CFANode callerNode) {
    this.previousState = previousElement;
    this.currentFunction = checkNotNull(function);
    this.callerNode = checkNotNull(callerNode);
    if (previousElement == null) {
      depth = 1;
    } else {
      depth = previousElement.getDepth() + 1;
    }
  }

  public CallstackPccState getPreviousState() {
    return previousState;
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
        + " [" + Integer.toHexString(hashCode()) + "]";
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther == null) {
      return false;
    }
    if (pOther instanceof CallstackPccState) {
      CallstackPccState other = (CallstackPccState) pOther;
      if (other.callerNode == callerNode
          && other.depth == depth
          && other.currentFunction.equals(currentFunction)
          && (other.previousState == previousState || (previousState != null && other.previousState != null && previousState
              .equals(other.previousState)))) { return true; }
    }
    return false;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((callerNode == null) ? 0 : callerNode.hashCode());
    result = prime * result + ((currentFunction == null) ? 0 : currentFunction.hashCode());
    result = prime * result + depth;
    result = prime * result + ((previousState == null) ? 0 : previousState.hashCode());
    return result;
  }

  @Override
  public String getCPAName() {
    return "CallstackPCC";
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    return false;
  }

  @Override
  public Object evaluateProperty(String pProperty) throws InvalidQueryException {
    if (pProperty.compareToIgnoreCase("caller") == 0) {
      if (callerNode != null) {
        return this.callerNode.getFunctionName();
      } else {
        return "";
      }
    }

    throw new InvalidQueryException(String.format("Evaluating %s not supported by %s", pProperty, this.getClass()
        .getCanonicalName()));
  }

  @Override
  public void modifyProperty(String pModification) throws InvalidQueryException {
    throw new InvalidQueryException("modifyProperty not implemented by " + this.getClass().getCanonicalName());
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    out.writeInt(callerNode.getNodeNumber());
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    int nodeNumber = in.readInt();
    callerNode = GlobalInfo.getInstance().getCFAInfo().get().getNodeByNodeNumber(nodeNumber);
  }

}
