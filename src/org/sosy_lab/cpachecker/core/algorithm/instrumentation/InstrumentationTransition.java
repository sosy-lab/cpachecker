// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import org.sosy_lab.cpachecker.core.algorithm.instrumentation.InstrumentationAutomaton.InstrumentationOrder;

public class InstrumentationTransition {
  private InstrumentationState source;
  private InstrumentationState destination;
  private InstrumentationPattern pattern;
  private InstrumentationOperation operation;
  private InstrumentationOrder order;

  public InstrumentationTransition(
      InstrumentationState pSource,
      InstrumentationPattern pPattern,
      InstrumentationOperation pOperation,
      InstrumentationOrder pOrder,
      InstrumentationState pDestination) {
    this.source = pSource;
    this.operation = pOperation;
    this.pattern = pPattern;
    this.order = pOrder;
    this.destination = pDestination;
  }

  @Override
  public String toString() {
    return source.toString()
        + " | "
        + pattern
        + " | "
        + operation
        + " | "
        + order.name()
        + " | "
        + destination;
  }

  public String getOrderAsString() {
    return order.name();
  }

  public InstrumentationPattern getPattern() {
    return pattern;
  }

  public InstrumentationOperation getOperation() {
    return operation;
  }

  public InstrumentationState getSource() {
    return source;
  }

  public InstrumentationState getDestination() {
    return destination;
  }
}
