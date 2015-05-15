package org.sosy_lab.cpachecker.cpa.policyiteration;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.base.Preconditions;

public class PolicyMergeOperator implements MergeOperator {
  private final IPolicyIterationManager policyIterationManager;
  private final boolean joinOnMerge;

  public PolicyMergeOperator(IPolicyIterationManager pPolicyIterationManager,
      boolean pJoinOnMerge) {
    policyIterationManager = pPolicyIterationManager;
    joinOnMerge = pJoinOnMerge;
  }

  @Override
  public AbstractState merge(AbstractState state1, AbstractState state2,
      Precision precision) throws CPAException, InterruptedException {
    if (joinOnMerge) {
      return mergeJoin((PolicyState)state1, (PolicyState)state2,
          (PolicyPrecision)precision);
    } else {
      return mergeABE((PolicyState)state1, (PolicyState)state2,
          (PolicyPrecision)precision);
    }
  }

  public PolicyState mergeABE(PolicyState state1, PolicyState state2,
      PolicyPrecision precision) throws CPAException, InterruptedException {
    Preconditions.checkState(state1.isAbstract() == state2.isAbstract());
    if (state1.isAbstract()) {

      // No merge.
      return state2;
    }
    // Now things are tricky.
    // We wish to merge if and only if both states have same versions of
    // abstract predecessors.
    PolicyIntermediateState iState1 = state1.asIntermediate();
    PolicyIntermediateState iState2 = state2.asIntermediate();
    if (iState1.getGeneratingStates().equals(iState2.getGeneratingStates())) {
      return policyIterationManager.join(iState1, iState2, precision);
    } else {
      return state2;
    }
  }

  public PolicyState mergeJoin(PolicyState state1, PolicyState state2,
      PolicyPrecision precision) throws CPAException, InterruptedException {
    return policyIterationManager.join(state1, state2, precision);
  }
}
