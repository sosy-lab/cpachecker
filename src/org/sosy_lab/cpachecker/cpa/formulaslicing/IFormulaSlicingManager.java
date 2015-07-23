package org.sosy_lab.cpachecker.cpa.formulaslicing;

import java.util.Collection;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.base.Optional;

public interface IFormulaSlicingManager {
  SlicingState join(
      SlicingState oldState,
      SlicingState newState
  ) throws CPAException, InterruptedException;

  Collection<? extends SlicingState> getAbstractSuccessors(
      SlicingState state,
      CFAEdge edge
  ) throws CPATransferException, InterruptedException;

  Collection<? extends SlicingState> strengthen(
      SlicingState state,
      List<AbstractState> otherState,
      CFAEdge pCFAEdge
  ) throws CPATransferException, InterruptedException;

  SlicingState getInitialState(CFANode node);

  boolean isLessOrEqual(SlicingState pState1,
      SlicingState pState2);

  Optional<PrecisionAdjustmentResult> prec(SlicingState pState, UnmodifiableReachedSet pStates,
      AbstractState pFullState);
}
