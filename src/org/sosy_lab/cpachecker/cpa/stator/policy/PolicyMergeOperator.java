package org.sosy_lab.cpachecker.cpa.stator.policy;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class PolicyMergeOperator implements MergeOperator{
  private final PolicyAbstractDomain domain;
  private final PolicyIterationStatistics statistics;

  public PolicyMergeOperator(PolicyAbstractDomain d, PolicyIterationStatistics pStatistics) {
    domain = d;
    statistics = pStatistics;
  }

  @Override
  public AbstractState merge(AbstractState newState, AbstractState prevState, Precision p)
      throws CPAException, InterruptedException {

    statistics.timeInMerge.start();
    try {
      AbstractState out = domain.join(
          (PolicyState)newState,
          (PolicyState)prevState,
          (PolicyPrecision)p);

      // Note: returning an exactly same state is important, due to the issues
      // with .equals() handling.
      if (out.equals(prevState)) {
        return prevState;
      }
      return out;
    } finally {
      statistics.timeInMerge.stop();
    }
  }
}
