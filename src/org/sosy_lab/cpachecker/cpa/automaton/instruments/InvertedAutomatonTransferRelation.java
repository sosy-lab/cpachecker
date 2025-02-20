package org.sosy_lab.cpachecker.cpa.automaton.instruments;

import java.util.Collection;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonStatistics;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTransferRelation;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.collect.FluentIterable;

public class InvertedAutomatonTransferRelation extends AutomatonTransferRelation{

    public InvertedAutomatonTransferRelation(
            ControlAutomatonCPA pCpa,
            LogManager pLogger,
            MachineModel pMachineModel,
            AutomatonStatistics pStats){
        super(pCpa, pLogger, pMachineModel, pStats); 
    }
    
    @Override
        public Collection<AutomatonState> getAbstractSuccessorsForEdge(AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) throws CPATransferException{
            Collection<AutomatonState> result =
            super.getAbstractSuccessorsForEdge(pState, pPrecision, pCfaEdge);
        return FluentIterable.from(result)
            .transform(i -> i.flip())
            .toList();
        }
    
}
