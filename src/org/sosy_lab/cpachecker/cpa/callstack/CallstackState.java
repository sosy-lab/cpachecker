/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

/**
 * Abstract state that stores callstack information by maintaning a single-linked list of states
 * that represents the current callstack.
 *
 * Note that whenever a new state is created, this represents a new, unique, entry of a function.
 * Two separate entries of the same function are not considered equal,
 * even if the function names and call nodes of the two callstacks match.
 * Cf. {@link CallstackTest#testCallstackPreventsUndesiredCoverage()} for an example.
 * (Because of this this class must inherit the identity-based
 * {@link #equals(Object)} and {@link #hashCode()} from Object.)
 */
public class CallstackState
    implements AbstractState, Partitionable, AbstractQueryableState, Serializable {

  private static final long serialVersionUID = 3629687385150064994L;

  protected final @Nullable CallstackState previousState;
  protected final String currentFunction;
  protected transient CFANode callerNode;
  private final int depth;

  public CallstackState(
      @Nullable CallstackState pPreviousElement,
      @Nonnull String pFunction,
      @Nonnull CFANode pCallerNode) {

    previousState = pPreviousElement;
    currentFunction = checkNotNull(pFunction);
    callerNode = checkNotNull(pCallerNode);

    if (pPreviousElement == null) {
      depth = 1;
    } else {
      depth = pPreviousElement.getDepth() + 1;
    }
  }

  public CallstackState getPreviousState() {
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

  /** for logging and debugging */
  private List<String> getStack() {
    final List<String> stack = new ArrayList<>();
    CallstackState state = this;
    while (state != null) {
      stack.add(state.getCurrentFunction());
      state = state.getPreviousState();
    }
    return Lists.reverse(stack);
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
        + " [" + Integer.toHexString(super.hashCode())
        + "], stack " + getStack();
  }

  public boolean sameStateInProofChecking(CallstackState pOther) {
    if (pOther.callerNode == callerNode
        && pOther.depth == depth
        && pOther.currentFunction.equals(currentFunction)
        && (pOther.previousState == previousState || (previousState != null && pOther.previousState != null && previousState
            .sameStateInProofChecking(pOther.previousState)))) { return true; }
    return false;
  }

  @Override
  public String getCPAName() {
    return "Callstack";
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

  public int hashCodeWithoutNode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((currentFunction == null) ? 0 : currentFunction.hashCode());
    result = prime * result + depth;
    result = prime * result + ((previousState == null) ? 0 : previousState.hashCodeWithoutNode());
    return result;
  }

  public boolean equalsWithoutNode(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    CallstackState other = (CallstackState) obj;
    if (currentFunction == null) {
      if (other.currentFunction != null) {
        return false;
      }
    } else if (!currentFunction.equals(other.currentFunction)) {
      return false;
    }
    if (depth != other.depth) {
      return false;
    }
    if (previousState == null) {
      if (other.previousState != null) {
        return false;
      }
    } else if (!previousState.equalsWithoutNode(other.previousState)) {
      return false;
    }
    return true;
  }

  @Override
  public CallstackState clone() {
    if (this.previousState != null) {
      return new CallstackState(this.previousState.clone(), this.currentFunction, this.callerNode);
    } else {
      return new CallstackState(null, this.currentFunction, this.callerNode);
    }
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
