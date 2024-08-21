// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;

public class DistributedCompositeCPA implements DistributedConfigurableProgramAnalysis {

  private final CompositeCPA compositeCPA;
  private final SerializeOperator serialize;
  private final DeserializeOperator deserialize;
  private final CombineOperator combine;
  private final ProceedOperator proceed;

  public DistributedCompositeCPA(
      CompositeCPA pCompositeCPA,
      BlockNode pNode,
      AnalysisDirection pDirection,
      Map<Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          registered) {
    compositeCPA = pCompositeCPA;
    serialize = new SerializeCompositeStateOperator(registered);
    deserialize = new DeserializeCompositeStateOperator(compositeCPA, pNode, registered);
    combine = new CombineCompositeStateOperator(registered);
    proceed = new ProceedCompositeStateOperator(registered, pDirection);
  }

  @Override
  public SerializeOperator getSerializeOperator() {
    return serialize;
  }

  @Override
  public CombineOperator getCombineOperator() {
    return combine;
  }

  @Override
  public DeserializeOperator getDeserializeOperator() {
    return deserialize;
  }

  @Override
  public ProceedOperator getProceedOperator() {
    return proceed;
  }

  @Override
  public Class<? extends AbstractState> getAbstractStateClass() {
    return CompositeState.class;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return compositeCPA.getAbstractDomain();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return compositeCPA.getTransferRelation();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return compositeCPA.getMergeOperator();
  }

  @Override
  public StopOperator getStopOperator() {
    return compositeCPA.getStopOperator();
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return compositeCPA.getInitialState(node, partition);
  }
}
