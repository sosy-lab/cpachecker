// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class TestTargetState
    implements LatticeAbstractState<TestTargetState>, Targetable, Graphable {

  private static final TestTargetState noTargetState = new TestTargetState(Status.NO_TARGET);

  public static AbstractState noTargetState() {
    return noTargetState;
  }

  enum Status {
    TARGET(true),
    NO_TARGET(false),
    STOP_POSSIBLY_INFEASIBLE_TARGET(false);

    private final boolean isConsideredTarget;

    Status(final boolean isTarget) {
      isConsideredTarget = isTarget;
    }
  }

  private Status currentState;

  TestTargetState(final Status pInitialStatus) {
    currentState = pInitialStatus;
  }

  @Override
  public boolean isTarget() {
    return currentState.isConsideredTarget;
  }

  @Override
  public @NonNull Set<TargetInformation> getTargetInformation() throws IllegalStateException {
    return ImmutableSet.of();
  }

  @Override
  public String toDOTLabel() {
    return currentState.name();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public String toString() {
    return currentState.name();
  }

  @Override
  public TestTargetState join(TestTargetState pOther) throws CPAException, InterruptedException {
    throw new UnsupportedOperationException("Join is not supported");
  }

  @Override
  public boolean isLessOrEqual(TestTargetState pOther) throws CPAException, InterruptedException {
    return equals(pOther)
        || (pOther.currentState == Status.TARGET
            && currentState == Status.STOP_POSSIBLY_INFEASIBLE_TARGET);
  }

  public void changeToStopTargetStatus() {
    if (this != noTargetState) {
      currentState = Status.STOP_POSSIBLY_INFEASIBLE_TARGET;
    }
  }

  public boolean isStop() {
    return currentState == Status.STOP_POSSIBLY_INFEASIBLE_TARGET;
  }
}
