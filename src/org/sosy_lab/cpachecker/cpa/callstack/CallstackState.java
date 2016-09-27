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

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
  private transient CallstackWrapper equivalenceWrapper = null;

  /**
   * Callstack wrapper which provides comparison based on stored data.
   */
  public static final class CallstackWrapper {
    private final CallstackState wrapped;

    private CallstackWrapper(CallstackState pWrapped) {
      wrapped = pWrapped;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof CallstackWrapper)) {
        return false;
      }
      CallstackWrapper other = (CallstackWrapper) o;
      return wrapped.currentFunction.equals(other.wrapped.currentFunction)
          && wrapped.callerNode.equals(other.wrapped.callerNode)
          && wrapped.depth == other.wrapped.depth
          && (
            (wrapped.previousState == null && other.wrapped.previousState == null)
              ||
            (wrapped.previousState != null && other.wrapped.previousState != null && Objects.equals(
                wrapped.previousState.getEquivalenceWrapper(),
                other.wrapped.previousState.getEquivalenceWrapper()
            )
          ));
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          wrapped.currentFunction,
          wrapped.callerNode,
          wrapped.depth,
          wrapped.previousState != null ?
          wrapped.previousState.getEquivalenceWrapper() : null);
    }
  }

  public CallstackWrapper getEquivalenceWrapper() {
    if (equivalenceWrapper == null) {
      equivalenceWrapper = new CallstackWrapper(this);
    }
    return equivalenceWrapper;
  }


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
