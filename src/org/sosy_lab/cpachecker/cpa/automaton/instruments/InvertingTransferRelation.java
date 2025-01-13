package org.sosy_lab.cpachecker.cpa.automaton.instruments;

import java.util.Collection;
import java.util.List;


import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.InvertableState;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class InvertingTransferRelation {
    public InvertingTransferRelation(TransferRelation p) throws CPATransferException, InterruptedException{
        getAbstractSuccessorsForEdge(p);
    }

    public List<AbstractState> getAbstractSuccessorsForEdge(TransferRelation p) throws CPATransferException, InterruptedException {
     Collection<? extends AbstractState> result = p.getAbstractSuccessorsForEdge(null, null, null);
     assert result.stream().allMatch(a -> a instanceof InvertableState);
     return result.stream().map(InvertableState.class::cast).map(InvertableState::flip).toList();
  }
}
