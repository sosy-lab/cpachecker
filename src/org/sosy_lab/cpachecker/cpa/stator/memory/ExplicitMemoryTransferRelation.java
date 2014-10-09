package org.sosy_lab.cpachecker.cpa.stator.memory;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;
import java.util.List;

public class ExplicitMemoryTransferRelation extends SingleEdgeTransferRelation
    implements TransferRelation {
  private final MachineModel machineModel;
  private final LogManager logger;

  public ExplicitMemoryTransferRelation(
      MachineModel machineModel,
      LogManager logger) {
    this.machineModel = machineModel;
    this.logger = logger;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge) throws
          CPATransferException, InterruptedException {
    AliasState prevState = (AliasState) state;
    return getSuccessors(prevState, precision, cfaEdge);
  }


  public Collection<? extends AliasState> getSuccessors(
      AliasState prevAbstractValue,
      Precision precision,
      CFAEdge edge
  ) {
    ExpressionTranslator expressionTranslator = new ExpressionTranslator(
      prevAbstractValue, machineModel, edge, precision,
      edge.getPredecessor(), logger
    );

    return expressionTranslator.getAbstractSuccessors();
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      List<AbstractState> otherStates,
      CFAEdge cfaEdge,
      Precision precision) throws CPATransferException, InterruptedException {
    return null;
  }
}
