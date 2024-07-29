// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.algorithm.instrumentation.InstrumentationAutomaton.InstrumentationOrder;

public class InstrumentationTransition {
  private InstrumentationState source;
  private InstrumentationState destination;
  /**
   * TODO: Implement pattern class and matching (look for possible regexes?)
   */
  private String pattern;
  private String operation;
  private InstrumentationOrder order;

  public InstrumentationTransition(InstrumentationState pSource,
                                   String pPattern,
                                   String pOperation,
                                   InstrumentationOrder pOrder,
                                   InstrumentationState pDestination) {
    this.source = pSource;
    this.operation = pOperation;
    this.pattern = pPattern;
    this.order = pOrder;
    this.destination = pDestination;
  }

  public boolean transitionMatchesCfaEdge(CFAEdge pCFAEdge) {
    // TODO: Once there is a pattern class for matching, move this there
    switch (pattern) {
      case "true" :
        return true;
      case "[cond]" :
        return isOriginalCond(pCFAEdge);
      case "[!cond]" :
        return isNegatedCond(pCFAEdge);
      default :
        return false;
    }
  }

  @Override
  public String toString() {
    return source.toString() +
        " | " + pattern +
        " | " + operation +
        " | " + order.name() +
        " | " + destination.toString();
  }

  public String getOrderAsString() {
    return order.name();
  }

  public String getOperation() {
    return operation;
  }

  public InstrumentationState getSource() {
    return source;
  }

  public InstrumentationState getDestination() {
    return destination;
  }

  private boolean isOriginalCond(CFAEdge pCFAEdge) {
    if (pCFAEdge instanceof CAssumeEdge) {
      return ((CAssumeEdge) pCFAEdge).getTruthAssumption();
    }
    return false;
  }

  private boolean isNegatedCond(CFAEdge pCFAEdge) {
    if (pCFAEdge instanceof CAssumeEdge) {
      return !((CAssumeEdge) pCFAEdge).getTruthAssumption();
    }
    return false;
  }
}
