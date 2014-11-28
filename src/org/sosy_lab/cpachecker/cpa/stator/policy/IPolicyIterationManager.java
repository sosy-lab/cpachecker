package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Interface for the processing functions for policy iteration.
 */
public interface IPolicyIterationManager {
  PolicyAbstractState join(
      PolicyAbstractState oldState,
      PolicyAbstractState newState
  ) throws CPAException, InterruptedException;

  boolean isLessOrEqual(
      PolicyAbstractState state1,
      PolicyAbstractState state2
  ) throws CPAException, InterruptedException;

  Collection<PolicyAbstractState> getAbstractSuccessors(
      PolicyAbstractState state, CFAEdge edge
  ) throws CPATransferException, InterruptedException;

  Collection<PolicyAbstractState> strengthen(
      PolicyAbstractState state,
      List<AbstractState> otherStates,
      @Nullable CFAEdge pCFAEdge
  ) throws CPATransferException, InterruptedException;

  PolicyAbstractState getInitialState(CFANode node);
}
