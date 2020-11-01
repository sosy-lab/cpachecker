// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pendingException;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.Appenders.AbstractAppender;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class PendingExceptionState extends AbstractAppender
    implements LatticeAbstractState<PendingExceptionState> {

  public static final String PENDING_EXCEPTION = "pending_exception";

  // Name of variable, RunTimeType
  private final Map<String, String> pendingExceptions;

  // Name of Array and length
  private final Map<String, List<BigInteger>> arrays;

  public PendingExceptionState() {
    pendingExceptions = new HashMap<>();
    arrays = new HashMap<>();
  }

  public Map<String, String> getPendingExceptions() {
    return pendingExceptions;
  }

  Map<String, List<BigInteger>> getArrays() {
    return arrays;
  }

  @Override
  public void appendTo(Appendable appendable) {
    // TODO
  }

  @Override
  public PendingExceptionState join(PendingExceptionState other)
      throws CPAException, InterruptedException {
    return null; // TODO
  }

  @Override
  public boolean isLessOrEqual(PendingExceptionState other)
      throws CPAException, InterruptedException {
    return false; // TODO
  }
}
