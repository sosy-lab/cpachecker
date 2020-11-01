// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.cpa.pendingException;

import java.io.PrintStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class PendingExceptionCPAStatistics implements Statistics {
  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    for (AbstractState currentAbstractState : reached) {
      PendingExceptionState currentState =
          AbstractStates.extractStateByType(currentAbstractState, PendingExceptionState.class);
      if (currentState == null) {
        return;
      }
      out.println("Number of exceptions caught: " + currentState.getCounterExceptionsCaught());
      out.println(
          "Number of method invocations tested for null pointer: "
              + currentState.getCounterMethodInvocationsTested());
      break;
    }
  }

  @Override
  public @Nullable String getName() {
    String simpleName = this.getClass().getSimpleName();
    return simpleName.substring(0, simpleName.length() - "Statistics".length());
  }
}
