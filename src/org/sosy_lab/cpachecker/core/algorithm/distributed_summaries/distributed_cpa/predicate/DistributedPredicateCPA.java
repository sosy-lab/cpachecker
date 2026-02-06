// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;

public class DistributedPredicateCPA implements ForwardingDistributedConfigurableProgramAnalysis {

  private final PredicateCPA predicateCPA;

  private final SerializeOperator serialize;

  private final SerializePrecisionOperator serializePrecisionOperator;
  private final DeserializePredicateStateOperator deserialize;

  private final DeserializePrecisionOperator deserializePrecisionOperator;
  private final ProceedOperator proceedOperator;
  private final ViolationConditionOperator verificationConditionOperator;
  private final CoverageOperator stateCoverageOperator;
  private final CombineOperator combineOperator;
  private final CombinePrecisionOperator combinePrecisionOperator;

  public DistributedPredicateCPA(
      PredicateCPA pPredicateCPA,
      BlockNode pNode,
      CFA pCFA,
      Configuration pConfiguration,
      DssAnalysisOptions pOptions,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      ImmutableMap<Integer, CFANode> pIdToNodeMap)
      throws InvalidConfigurationException {
    predicateCPA = pPredicateCPA;
    final boolean writeReadableFormulas = pOptions.isDebugModeEnabled();
    serialize = new SerializePredicateStateOperator(predicateCPA, pCFA, writeReadableFormulas);
    deserialize = new DeserializePredicateStateOperator(predicateCPA, pCFA, pNode);
    serializePrecisionOperator =
        new SerializePredicatePrecisionOperator(pPredicateCPA.getSolver().getFormulaManager());
    deserializePrecisionOperator =
        new DeserializePredicatePrecisionOperator(
            predicateCPA.getAbstractionManager(), predicateCPA.getSolver(), pIdToNodeMap::get);
    proceedOperator = new ProceedPredicateStateOperator(predicateCPA.getSolver());
    stateCoverageOperator = new PredicateStateCoverageOperator(predicateCPA.getSolver());
    verificationConditionOperator =
        new PredicateViolationConditionOperator(
            new PathFormulaManagerImpl(
                pPredicateCPA.getSolver().getFormulaManager(),
                pConfiguration,
                pLogManager,
                pShutdownNotifier,
                pCFA,
                AnalysisDirection.BACKWARD),
            predicateCPA,
            pNode.getPredecessorIds().isEmpty());
    combineOperator = new CombinePredicateStateOperator(predicateCPA);
    combinePrecisionOperator = new CombinePredicatePrecisionOperator();
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
  public SerializeOperator getSerializeOperator() {
    return serialize;
  }

  @Override
  public DeserializeOperator getDeserializeOperator() {
    return deserialize;
  }

  @Override
  public ProceedOperator getProceedOperator() {
    return proceedOperator;
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
  public boolean isMostGeneralBlockEntryState(AbstractState pAbstractState) {
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

  @Override
  public AbstractState reset(AbstractState pAbstractState) {
    Preconditions.checkArgument(
        pAbstractState instanceof PredicateAbstractState,
        "Expected PredicateAbstractState, but got %s",
        pAbstractState.getClass().getSimpleName());
    return pAbstractState;
  }

  @Override
  public ViolationConditionOperator getViolationConditionOperator() {
    return verificationConditionOperator;
  }

  @Override
  public CoverageOperator getCoverageOperator() {
    return stateCoverageOperator;
  }

  @Override
  public CombineOperator getCombineOperator() {
    return combineOperator;
  }
}
