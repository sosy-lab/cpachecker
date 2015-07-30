package org.sosy_lab.cpachecker.cpa.formulaslicing;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.base.Preconditions;

public class SlicingMergeOperator implements MergeOperator {
  private final IFormulaSlicingManager manager;
  private final boolean joinOnMerge;

  public SlicingMergeOperator(IFormulaSlicingManager pManager,
      boolean pJoinOnMerge) {
    manager = pManager;
    joinOnMerge = pJoinOnMerge;
  }

  @Override
  public AbstractState merge(AbstractState state1, AbstractState state2,
      Precision precision) throws CPAException, InterruptedException {
    if (joinOnMerge) {
      return mergeJoin((SlicingState) state1, (SlicingState) state2);
    } else {
      return mergeABE((SlicingState) state1, (SlicingState) state2);
    }
  }

  private SlicingState mergeABE(SlicingState pState1, SlicingState pState2)
      throws CPAException, InterruptedException {
    Preconditions.checkState(pState1.isAbstracted() == pState2.isAbstracted());

    if (pState1.isAbstracted()) {

      // No merge.
      return pState2;
    } else {
      SlicingIntermediateState iState1 = pState1.asIntermediate();
      SlicingIntermediateState iState2 = pState2.asIntermediate();

      if (!iState1.getAbstractParent().equals(iState2.getAbstractParent())) {

        // No merge.
        return iState2;
      }
    }


    return manager.join(pState1, pState2);
  }

  private SlicingState mergeJoin(SlicingState pState1, SlicingState pState2)
      throws CPAException, InterruptedException {
    Preconditions.checkState(pState1.isAbstracted() == pState2.isAbstracted());

    if (!pState1.isAbstracted()) {
      SlicingIntermediateState iState1 = pState1.asIntermediate();
      SlicingIntermediateState iState2 = pState2.asIntermediate();

      if (!iState1.getAbstractParent().equals(iState2.getAbstractParent())) {

        // No merge.
        return iState2;
      }
    }
    return manager.join(pState1, pState2);
  }
}
