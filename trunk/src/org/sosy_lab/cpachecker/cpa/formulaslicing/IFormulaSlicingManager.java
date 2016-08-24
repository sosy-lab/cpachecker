package org.sosy_lab.cpachecker.cpa.formulaslicing;

import java.util.Optional;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;

public interface IFormulaSlicingManager extends StatisticsProvider {
  Collection<? extends SlicingState> getAbstractSuccessors(
      SlicingState state,
      CFAEdge edge
  ) throws CPATransferException, InterruptedException;

  SlicingState getInitialState(CFANode node);

  boolean isLessOrEqual(SlicingState pState1,
      SlicingState pState2) throws InterruptedException, CPAException;

  Optional<PrecisionAdjustmentResult> prec(SlicingState pState, UnmodifiableReachedSet pStates,
      AbstractState pFullState) throws CPAException, InterruptedException;

  SlicingState merge(SlicingState pState1, SlicingState pState2) throws InterruptedException;
}
