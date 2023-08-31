// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.AlwaysProceed;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.infer.InferDeserializePredicateStateOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.infer.InferSerializePredicateStateOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;

@Options(prefix = "cpa.predicate")
public class DistributedPredicateCPA implements ForwardingDistributedConfigurableProgramAnalysis {

  private final PredicateCPA predicateCPA;

  private final SerializeOperator serialize;

  private final SerializePrecisionOperator serializePrecisionOperator;
  private final DeserializeOperator deserialize;

  private final DeserializePrecisionOperator deserializePrecisionOperator;
  private final ProceedOperator proceed;

  private final Configuration config;

  public enum Serializer {
    DEFAULT, // TODO discuss other serializers with Matthias
    INFER
  }

  @Option(
      description = "Which serializer to use for distributed CPA",
      secure = true,
      name = "serializer")
  private Serializer serializer = Serializer.DEFAULT;

  public DistributedPredicateCPA(
      PredicateCPA pPredicateCPA,
      BlockNode pNode,
      CFA pCFA,
      AnalysisDirection pDirection,
      Configuration pConfig)
      throws InvalidConfigurationException {
    config = pConfig;
    config.inject(this);
    predicateCPA = pPredicateCPA;
    serialize = initSerializer(pCFA, pDirection);
    deserialize = initDeserializer(pCFA, pNode);

    proceed = new AlwaysProceed();
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
    return proceed;
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

  private SerializeOperator initSerializer(CFA pCFA, AnalysisDirection pDirection) {
    return switch (serializer) {
      case DEFAULT -> new SerializePredicateStateOperator(predicateCPA, pCFA, pDirection);
      case INFER -> new InferSerializePredicateStateOperator(predicateCPA, pCFA);
    };
  }

  private DeserializeOperator initDeserializer(CFA pCFA, BlockNode pNode) {
    return switch (serializer) {
      case DEFAULT -> new DeserializePredicateStateOperator(predicateCPA, pCFA, pNode);
      case INFER -> new InferDeserializePredicateStateOperator(predicateCPA, pCFA, pNode);
    };
  }
}
