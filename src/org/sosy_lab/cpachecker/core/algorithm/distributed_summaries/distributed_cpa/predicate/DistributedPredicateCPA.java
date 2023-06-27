// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;

public class DistributedPredicateCPA implements ForwardingDistributedConfigurableProgramAnalysis {

  private final PredicateCPA predicateCPA;

  private final SerializeOperator serialize;

  private final SerializePrecisionOperator serializePrecisionOperator;
  private final DeserializePredicateStateOperator deserialize;

  private final DeserializePrecisionOperator deserializePrecisionOperator;

  public DistributedPredicateCPA(
      PredicateCPA pPredicateCPA, BlockNode pNode, CFA pCFA, AnalysisDirection pDirection) {
    predicateCPA = pPredicateCPA;
    serialize = new SerializePredicateStateOperator(predicateCPA, pCFA, pDirection);
    deserialize = new DeserializePredicateStateOperator(predicateCPA, pCFA, pNode);
    serializePrecisionOperator =
        new SerializePredicatePrecisionOperator(pPredicateCPA.getSolver().getFormulaManager());
    ImmutableMap.Builder<Integer, CFANode> idToNodeMap = ImmutableMap.builder();
    for (CFANode cfaNode : pCFA.nodes()) {
      idToNodeMap.put(cfaNode.getNodeNumber(), cfaNode);
    }
    deserializePrecisionOperator =
        new DeserializePredicatePrecisionOperator(
            predicateCPA.getAbstractionManager(),
            predicateCPA.getSolver(),
            idToNodeMap.buildOrThrow()::get);
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
  public SerializeOperator getSerializeOperator() {
    return serialize;
  }

  @Override
  public DeserializeOperator getDeserializeOperator() {
    return deserialize;
  }

  @Override
  public ProceedOperator getProceedOperator() {
    return ProceedOperator.always();
  }

  @Override
  public Class<? extends AbstractState> getAbstractStateClass() {
    return PredicateAbstractState.class;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return predicateCPA;
  }

  @Override
  public boolean isTop(AbstractState pAbstractState) {
    PredicateAbstractState predicateAbstractState = (PredicateAbstractState) pAbstractState;
    if (predicateAbstractState.isAbstractionState()) {
      return predicateAbstractState.getAbstractionFormula().isTrue();
    }
    return predicateCPA
        .getSolver()
        .getFormulaManager()
        .getBooleanFormulaManager()
        .isTrue(predicateAbstractState.getPathFormula().getFormula());
  }
}
