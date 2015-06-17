package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.base.Optional;

/**
 * Interface for the processing functions for policy iteration.
 */
public interface IPolicyIterationManager {
  PolicyState join(
      PolicyState oldState,
      PolicyState newState,
      PolicyPrecision pPrecision) throws CPAException, InterruptedException;

  Collection<? extends PolicyState> getAbstractSuccessors(
      PolicyState state,
      CFAEdge edge
  ) throws CPATransferException, InterruptedException;

  Collection<? extends PolicyState> strengthen(
      PolicyState state,
      List<AbstractState> otherStates,
      @Nullable CFAEdge pCFAEdge
  ) throws CPATransferException, InterruptedException;

  PolicyState getInitialState(CFANode node);

  Optional<PrecisionAdjustmentResult> prec(
      PolicyState state,
      PolicyPrecision precision,
      UnmodifiableReachedSet states,
      AbstractState pArgState) throws CPAException, InterruptedException;

  boolean adjustPrecision();

  void adjustReachedSet(ReachedSet pReachedSet);

  boolean isLessOrEqual(PolicyState pState1, PolicyState pState2);
}
