package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.dataflow;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.VerificationConditionException;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.java_smt.api.SolverException;

public class DistributedDataFlowAnalysisCPA
    implements ForwardingDistributedConfigurableProgramAnalysis {

  private final InvariantsCPA invariantsCPA;
  private final SerializeOperator serializeOperator;
  private final DeserializeOperator deserializeOperator;
  private final SerializePrecisionOperator serializePrecisionOperator;
  private final DeserializePrecisionOperator deserializePrecisionOperator;

  public DistributedDataFlowAnalysisCPA(InvariantsCPA pInvariantsCPA) {
    invariantsCPA = pInvariantsCPA;
    serializeOperator = new SerializeDataflowAnalysisStateOperator();
    deserializeOperator = new DeserializeDataflowAnalysisStateOperator();
    serializePrecisionOperator = new SerializeDataflowAnalysisPrecisionOperator();
    deserializePrecisionOperator = new DeserializeDataflowAnalyisPrecisionOperator();
  }

  @Override
  public SerializeOperator getSerializeOperator() {
    return serializeOperator;
  }

  @Override
  public DeserializeOperator getDeserializeOperator() {
    return deserializeOperator;
  }

  @Override
  public ProceedOperator getProceedOperator() {
    return ProceedOperator.always();
  }

  @Override
  public Class<? extends AbstractState> getAbstractStateClass() {
    return InvariantsState.class;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return invariantsCPA;
  }

  @Override
  public boolean isTop(AbstractState pAbstractState) {
    throw new UnsupportedOperationException("Unimplemented method 'isTop'");
  }

  @Override
  public AbstractState computeVerificationCondition(ARGPath pARGPath, ARGState pPreviousCondition)
      throws CPATransferException,
          InterruptedException,
          VerificationConditionException,
          SolverException {
    throw new UnsupportedOperationException("Unimplemented method 'computeVerificationCondition'");
  }
}
