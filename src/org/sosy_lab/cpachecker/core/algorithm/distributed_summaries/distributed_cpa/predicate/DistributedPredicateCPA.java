// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.VerificationConditionException;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.PredicateOperatorUtil.UniqueIndexProvider;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class DistributedPredicateCPA implements ForwardingDistributedConfigurableProgramAnalysis {

  private final PredicateCPA predicateCPA;

  private final SerializeOperator serialize;

  private final SerializePrecisionOperator serializePrecisionOperator;
  private final DeserializePredicateStateOperator deserialize;

  private final DeserializePrecisionOperator deserializePrecisionOperator;
  private final UniqueIndexProvider indexProvider;

  public DistributedPredicateCPA(
      PredicateCPA pPredicateCPA, BlockNode pNode, CFA pCFA, Map<Integer, CFANode> pIdToNodeMap) {
    predicateCPA = pPredicateCPA;
    serialize = new SerializePredicateStateOperator(predicateCPA, pCFA);
    deserialize = new DeserializePredicateStateOperator(predicateCPA, pCFA, pNode);
    serializePrecisionOperator =
        new SerializePredicatePrecisionOperator(pPredicateCPA.getSolver().getFormulaManager());
    indexProvider = new UniqueIndexProvider(pNode.getId());
    ImmutableMap<Integer, CFANode> threadSafeCopy = ImmutableMap.copyOf(pIdToNodeMap);
    deserializePrecisionOperator =
        new DeserializePredicatePrecisionOperator(
            predicateCPA.getAbstractionManager(), predicateCPA.getSolver(), threadSafeCopy::get);
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

  @Override
  public AbstractState computeVerificationCondition(ARGPath pARGPath, ARGState pPreviousCondition)
      throws CPATransferException,
          InterruptedException,
          VerificationConditionException,
          SolverException {
    PredicateAbstractState counterexampleState =
        Objects.requireNonNull(
            AbstractStates.extractStateByType(pPreviousCondition, PredicateAbstractState.class));
    PathFormula previousCounterexample;
    if (counterexampleState.isAbstractionState()) {
      previousCounterexample = counterexampleState.getAbstractionFormula().getBlockFormula();
    } else {
      previousCounterexample = counterexampleState.getPathFormula();
    }
    PathFormula counterexample =
        predicateCPA.getPathFormulaManager().makeFormulaForPath(pARGPath.getFullPath());
    counterexample =
        PredicateOperatorUtil.makeAndByShiftingSecond(
            predicateCPA.getSolver().getFormulaManager(),
            counterexample,
            previousCounterexample,
            indexProvider);
    if (predicateCPA.getSolver().isUnsat(counterexample.getFormula())) {
      throw new VerificationConditionException(
          "Infeasible counterexample in verification condition");
    }
    return PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
        counterexample,
        (PredicateAbstractState)
            getInitialState(
                Objects.requireNonNull(AbstractStates.extractLocation(pARGPath.getFirstState())),
                StateSpacePartition.getDefaultPartition()));
  }
}
