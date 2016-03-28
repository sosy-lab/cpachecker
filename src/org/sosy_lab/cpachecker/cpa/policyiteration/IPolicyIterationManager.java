package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;

/**
 * Interface for policy iteration.
 */
public interface IPolicyIterationManager {

  Collection<? extends PolicyState> getAbstractSuccessors(
      PolicyState state,
      CFAEdge edge
  ) throws CPATransferException, InterruptedException;

  PolicyState getInitialState(CFANode node);

  Optional<PrecisionAdjustmentResult> precisionAdjustment(
      PolicyState state,
      PolicyPrecision precision,
      UnmodifiableReachedSet states,
      AbstractState pArgState) throws CPAException, InterruptedException;

  boolean adjustPrecision();

  void adjustReachedSet(ReachedSet pReachedSet);

  boolean isLessOrEqual(PolicyState pState1, PolicyState pState2) throws CPAException;

  PolicyState merge(
      PolicyState state1, PolicyState state2,
      PolicyPrecision precision)
      throws CPAException, InterruptedException;

  Optional<PrecisionAdjustmentResult> postAdjustmentStrengthen(
      PolicyState result,
      PolicyPrecision precision,
      Iterable<AbstractState> otherStates,
      Iterable<Precision> otherPrecisions,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState resultFullState) throws CPAException, InterruptedException;
}
