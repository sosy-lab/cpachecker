package org.sosy_lab.cpachecker.cpa.legion;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;

public class StatisticsTransferRelation extends ForwardingTransferRelation {
    @Override
    public Collection getAbstractSuccessorsForEdge(AbstractState abstractState, Precision abstractPrecision, CFAEdge cfaEdge) throws CPATransferException, InterruptedException {
        return ImmutableSet.of(new StatisticsState());
    }
}
