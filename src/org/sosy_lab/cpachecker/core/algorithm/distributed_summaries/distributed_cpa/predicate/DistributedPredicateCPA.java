// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.AlwaysProceed;
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
  private final ProceedOperator proceed;

  public DistributedPredicateCPA(
      PredicateCPA pPredicateCPA,
      BlockNode pNode,
      AnalysisDirection pDirection) {
    predicateCPA = pPredicateCPA;
    serialize =
        new SerializePredicateStateOperator(
            predicateCPA.getPathFormulaManager(),
            predicateCPA.getSolver().getFormulaManager(),
            pDirection);
    deserialize =
        new DeserializePredicateStateOperator(
            predicateCPA,
            predicateCPA.getSolver().getFormulaManager(),
            predicateCPA.getPathFormulaManager(),
            pNode);
    proceed = new AlwaysProceed();
    serializePrecisionOperator =
        new SerializePredicatePrecisionOperator(pPredicateCPA.getSolver().getFormulaManager());
    deserializePrecisionOperator =
        new DeserializePredicatePrecisionOperator(
            predicateCPA.getAbstractionManager(), predicateCPA.getSolver(), pNode::getNodeWithNumber);
  }

  public SerializePrecisionOperator getSerializePrecisionOperator() {
    return serializePrecisionOperator;
  }

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
}
