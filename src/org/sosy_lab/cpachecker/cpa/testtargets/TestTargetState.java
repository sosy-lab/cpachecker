/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.testtargets;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
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

    private Status(final boolean isTarget) {
      this.isConsideredTarget = isTarget;
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
  public @NonNull Set<Property> getViolatedProperties() throws IllegalStateException {
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
            && this.currentState == Status.STOP_POSSIBLY_INFEASIBLE_TARGET);
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
