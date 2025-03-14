package org.sosy_lab.cpachecker.cpa.automaton.instruments;

import com.google.common.collect.FluentIterable;
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

public class SelfloopAutomatonTransferRelation extends AutomatonTransferRelation {

  public SelfloopAutomatonTransferRelation(
      ControlAutomatonCPA pCpa,
      LogManager pLogger,
      MachineModel pMachineModel,
      AutomatonStatistics pStats) {
    super(pCpa, pLogger, pMachineModel, pStats);
  }

  @Override
  public Collection<AutomatonState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) throws CPATransferException {
    Collection<AutomatonState> result =
        super.getAbstractSuccessorsForEdge(pState, pPrecision, pCfaEdge);
    return FluentIterable.from(result).transform(i -> i.addselfloop()).toList();
  }
}
