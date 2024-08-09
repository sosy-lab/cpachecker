// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.violation_condition;

import java.util.function.Consumer;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class ViolationCondition {

  private static final ViolationCondition INFEASIBLE = new ViolationCondition(null);
  private final AbstractState violation;

  private ViolationCondition(AbstractState pViolation) {
    violation = pViolation;
  }

  public boolean isFeasible() {
    return violation != null;
  }

  public AbstractState getViolation() {
    return violation;
  }

  public void ifFeasible(Consumer<AbstractState> pConsumer) {
    if (isFeasible()) {
      pConsumer.accept(violation);
    }
  }

  public static ViolationCondition feasibleCondition(AbstractState pAbstractState) {
    return new ViolationCondition(pAbstractState);
  }

  public static ViolationCondition infeasibleCondition() {
    return INFEASIBLE;
  }
}
