package org.sosy_lab.cpachecker.cpa.policyiteration;

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
  PolicyState join(
      PolicyState oldState,
      PolicyState newState
  ) throws CPAException, InterruptedException;

  Collection<PolicyState> getAbstractSuccessors(
      PolicyState state, CFAEdge edge
  ) throws CPATransferException, InterruptedException;

  Collection<PolicyState> strengthen(
      PolicyState state,
      List<AbstractState> otherStates,
      @Nullable CFAEdge pCFAEdge
  ) throws CPATransferException, InterruptedException;

  PolicyState getInitialState(CFANode node);
}
