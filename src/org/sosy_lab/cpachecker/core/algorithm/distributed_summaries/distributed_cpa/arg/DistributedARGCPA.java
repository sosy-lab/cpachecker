// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.VerificationConditionException;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class DistributedARGCPA implements ForwardingDistributedConfigurableProgramAnalysis {

  private final ARGCPA argcpa;
  private final DistributedConfigurableProgramAnalysis wrappedCPA;
  private final ProceedOperator proceedOperator;
  private final DeserializeARGStateOperator deserializeARGStateOperator;
  private final SerializeARGStateOperator serializeARGStateOperator;
  private final SerializePrecisionOperator serializePrecisionOperator;
  private final DeserializePrecisionOperator deserializePrecisionOperator;

  public DistributedARGCPA(ARGCPA pARGCPA, DistributedConfigurableProgramAnalysis pWrapped) {
    argcpa = pARGCPA;
    wrappedCPA = pWrapped;
    proceedOperator = new ProceedARGCPAOperator(wrappedCPA);
    serializeARGStateOperator = new SerializeARGStateOperator(wrappedCPA);
    deserializeARGStateOperator = new DeserializeARGStateOperator(wrappedCPA);
    serializePrecisionOperator = new SerializeARGPrecisionOperator(wrappedCPA);
    deserializePrecisionOperator = new DeserializeARGPrecisionOperator(wrappedCPA);
  }

  @Override
  public SerializeOperator getSerializeOperator() {
    return serializeARGStateOperator;
  }

  @Override
  public DeserializeOperator getDeserializeOperator() {
    return deserializeARGStateOperator;
  }

  @Override
  public ProceedOperator getProceedOperator() {
    return proceedOperator;
  }

  @Override
  public DeserializePrecisionOperator getDeserializePrecisionOperator() {
    return deserializePrecisionOperator;
  }

  @Override
  public SerializePrecisionOperator getSerializePrecisionOperator() {
    return serializePrecisionOperator;
  }

  @Override
  public Class<? extends AbstractState> getAbstractStateClass() {
    return ARGState.class;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return argcpa;
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new ARGState(wrappedCPA.getInitialState(node, partition), null);
  }

  @Override
  public boolean isTop(AbstractState pAbstractState) {
    if (pAbstractState instanceof ARGState argstate) {
      return wrappedCPA.isTop(argstate.getWrappedState());
    }
    throw new IllegalArgumentException(
        "DistributedARGCPA can only work on " + getAbstractStateClass());
  }

  @Override
  public AbstractState computeVerificationCondition(ARGPath pARGPath, ARGState pPreviousCondition)
      throws CPATransferException, InterruptedException, VerificationConditionException {
    return new ARGState(
        wrappedCPA.computeVerificationCondition(pARGPath, pPreviousCondition), null);
  }
}
