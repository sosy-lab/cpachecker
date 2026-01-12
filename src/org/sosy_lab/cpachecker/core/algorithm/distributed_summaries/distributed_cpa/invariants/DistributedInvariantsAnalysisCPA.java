// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.invariants;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class DistributedInvariantsAnalysisCPA
    implements ForwardingDistributedConfigurableProgramAnalysis {

  private final InvariantsCPA invariantsCPA;
  private final SerializeOperator serializeOperator;
  private final DeserializeOperator deserializeOperator;
  private final SerializeInvariantsPrecisionOperator serializePrecisionOperator;
  private final DeserializeInvariantsPrecisionOperator deserializePrecisionOperator;
  private final CombinePrecisionOperator combinePrecisionOperator;
  private final CombineOperator combineOperator;
  private final CoverageOperator coverageOperator;
  private final BlockNode blockNode;

  public DistributedInvariantsAnalysisCPA(InvariantsCPA pInvariantsCPA, BlockNode pNode, CFA pCFA) {
    invariantsCPA = pInvariantsCPA;
    blockNode = pNode;
    serializeOperator = new SerializeDataflowAnalysisStateOperator();
    deserializeOperator = new DeserializeInvariantsStateOperator(invariantsCPA, pCFA);
    serializePrecisionOperator = new SerializeInvariantsPrecisionOperator();
    deserializePrecisionOperator = new DeserializeInvariantsPrecisionOperator(invariantsCPA, pCFA);
    combinePrecisionOperator = new CombineInvariantsPrecisionOperator();
    coverageOperator = new CoverageInvariantsStateOperator();
    combineOperator = new CombineInvariantsStateOperator();
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
  public SerializePrecisionOperator getSerializePrecisionOperator() {
    return serializePrecisionOperator;
  }

  @Override
  public DeserializePrecisionOperator getDeserializePrecisionOperator() {
    return deserializePrecisionOperator;
  }

  @Override
  public CombinePrecisionOperator getCombinePrecisionOperator() {
    return combinePrecisionOperator;
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
  public boolean isMostGeneralBlockEntryState(AbstractState pAbstractState) throws CPAException {
    try {
      return ((InvariantsState)
              getInitialState(
                  blockNode.getInitialLocation(), StateSpacePartition.getDefaultPartition()))
          .isLessOrEqual(((InvariantsState) pAbstractState));
    } catch (InterruptedException pE) {
      throw new CPAException("Interrupted while checking for most general block entry state", pE);
    }
  }

  @Override
  public AbstractState reset(AbstractState pAbstractState) {
    return pAbstractState;
  }

  @Override
  public ViolationConditionOperator getViolationConditionOperator() {
    return (pARGPath, pPreviousCondition) ->
        Optional.of(
            invariantsCPA.getInitialState(
                blockNode.getInitialLocation(), StateSpacePartition.getDefaultPartition()));
  }

  @Override
  public CoverageOperator getCoverageOperator() {
    return coverageOperator;
  }

  @Override
  public CombineOperator getCombineOperator() {
    return combineOperator;
  }
}
