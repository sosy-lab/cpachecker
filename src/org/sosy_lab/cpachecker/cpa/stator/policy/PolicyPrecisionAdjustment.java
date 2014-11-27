package org.sosy_lab.cpachecker.cpa.stator.policy;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;


public class PolicyPrecisionAdjustment implements PrecisionAdjustment {
  @Override
  public PrecisionAdjustmentResult prec(AbstractState state,
      Precision precision, UnmodifiableReachedSet states, AbstractState fullState)
      throws CPAException, InterruptedException {

    return PrecisionAdjustmentResult.create(
        state, new PolicyPrecision(states), Action.CONTINUE);
  }
}
