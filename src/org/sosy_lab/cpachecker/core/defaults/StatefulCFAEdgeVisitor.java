// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.CFAEdgeVisitor;

public class StatefulCFAEdgeVisitor<S> implements CFAEdgeVisitor<S> {

  /** the given state, casted to correct type, for local access */
  protected S state;

  /** the given precision, casted to correct type, for local access */
  protected Precision precision;

  /** the function BEFORE the current edge */
  protected String functionName;

  protected S getState() {
    return checkNotNull(state);
  }

  protected Precision getPrecision() {
    return checkNotNull(precision);
  }

  protected String getFunctionName() {
    return checkNotNull(functionName);
  }

  public StatefulCFAEdgeVisitor(
      final S pAbstractState, final Precision abstractPrecision, final CFAEdge cfaEdge) {
    state = pAbstractState;
    precision = abstractPrecision;
    functionName = getFunctionName(cfaEdge);
  }

  public static String getFunctionName(final CFAEdge cfaEdge) {
    return cfaEdge.getPredecessor().getFunctionName();
  }
}
