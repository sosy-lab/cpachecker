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

  private int counterMethodInvocationsTested = 0;
  private int counterExceptionsCaught = 0;

  public int getCounterMethodInvocationsTested() {
    return counterMethodInvocationsTested;
  }

  public void increaseCounterMethodInvocationsTested() {
    counterMethodInvocationsTested++;
  }

  public int getCounterExceptionsCaught() {
    return counterExceptionsCaught;
  }

  public void increaseCounterExceptionsCaught() {
    counterExceptionsCaught++;
  }

  public String getMethodInvocationObject() {
    increaseCounterMethodInvocationsTested();
    return methodInvocationObject;
  }

  public void setMethodInvocationObject(String pMethodInvocationObject) {
    this.methodInvocationObject = pMethodInvocationObject;
  }

  private String methodInvocationObject = "";

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
  public void appendTo(Appendable appendable) {}

  @Override
  public PendingExceptionState join(PendingExceptionState other)
      throws CPAException, InterruptedException {
    return this; // TODO
  }

  @Override
  public boolean isLessOrEqual(PendingExceptionState other)
      throws CPAException, InterruptedException {
    return true; // TODO
  }
}
