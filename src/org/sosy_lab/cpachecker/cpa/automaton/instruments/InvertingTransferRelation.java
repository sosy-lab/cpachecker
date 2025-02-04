package org.sosy_lab.cpachecker.cpa.automaton.instruments;

import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.InvertableState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.collect.FluentIterable;

public class InvertingTransferRelation implements TransferRelation{
    private final TransferRelation transferRelation;
    public InvertingTransferRelation(TransferRelation p){
        transferRelation = p;
    }

    @Override
    public Collection<? extends AbstractState>
            getAbstractSuccessors(AbstractState pState, Precision pPrecision)
                    throws CPATransferException, InterruptedException {
        throw new UnsupportedOperationException("Inverting requires edge");
    }

    @Override
    public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
            AbstractState pState,
            Precision pPrecision,
            CFAEdge pCfaEdge)
            throws CPATransferException, InterruptedException {
        Collection<? extends AbstractState> result = transferRelation.getAbstractSuccessorsForEdge(pState, pPrecision, pCfaEdge);
        assert result.stream().allMatch(a -> a instanceof InvertableState);
        return FluentIterable.from(result).filter(InvertableState.class).transform(i -> i.flip()).toList();
    }
}
