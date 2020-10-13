// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.callstack;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
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
  protected final int depth;

  public CallstackState(
      @Nullable CallstackState pPreviousElement,
      @Nullable String pFunction,
      @Nullable CFANode pCallerNode) {

    previousState = pPreviousElement;
    currentFunction = pFunction;
    callerNode = pCallerNode;

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
    if (pOther.callerNode.equals(callerNode)
        && pOther.depth == depth
        && pOther.currentFunction.equals(currentFunction)
        && (pOther.previousState == previousState
            || (previousState != null
                && pOther.previousState != null
                && previousState.sameStateInProofChecking(pOther.previousState)))) {
      return true;
    }
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

  @SuppressWarnings("UnusedVariable") // parameter is required by API
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    int nodeNumber = in.readInt();
    callerNode =
        GlobalInfo.getInstance().getCFAInfo().orElseThrow().getNodeByNodeNumber(nodeNumber);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof CallstackState)) {
      return false;
    }
    CallstackState other = (CallstackState) o;
    CallstackState tmp = this;
    if (other.getDepth() != tmp.getDepth()) {
      return false;
    }

    // check the whole stack
    while (tmp != null) {
      if (other == tmp) {
        return true;
      }
      if (!other.getCallNode().equals(tmp.getCallNode())
          || !other.getCurrentFunction().equals(tmp.getCurrentFunction())) {
        return false;
      }
      other = other.getPreviousState();
      tmp = tmp.getPreviousState();
    }

    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(callerNode, currentFunction, depth);
  }

}
