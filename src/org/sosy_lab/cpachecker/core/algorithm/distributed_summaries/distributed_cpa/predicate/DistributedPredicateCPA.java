// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

public class DistributedPredicateCPA implements DistributedConfigurableProgramAnalysis {

  private final PredicateCPA predicateCPA;

  private final SerializeOperator serialize;
  private final DeserializePredicateStateOperator deserialize;
  private final ProceedPredicateStateOperator proceed;

  public DistributedPredicateCPA(
      PredicateCPA pPredicateCPA, BlockNode pNode, AnalysisDirection pDirection) {
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
    proceed = new ProceedPredicateStateOperator(predicateCPA.getSolver(), pNode, pDirection);
  }

  public Solver getSolver() {
    return predicateCPA.getSolver();
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
  public AbstractState getInfeasibleState() throws InterruptedException {
    PathFormulaManager manager = predicateCPA.getPathFormulaManager();
    BooleanFormulaManager fmgr =
        predicateCPA.getSolver().getFormulaManager().getBooleanFormulaManager();
    PathFormula falseFormula = manager.makeAnd(manager.makeEmptyPathFormula(), fmgr.makeFalse());
    return PredicateAbstractState.mkAbstractionState(
        falseFormula,
        predicateCPA.getPredicateManager().asAbstraction(fmgr.makeFalse(), falseFormula),
        PathCopyingPersistentTreeMap.of());
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return predicateCPA.getAbstractDomain();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return predicateCPA.getTransferRelation();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return predicateCPA.getMergeOperator();
  }

  @Override
  public StopOperator getStopOperator() {
    return predicateCPA.getStopOperator();
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return predicateCPA.getInitialState(node, partition);
  }
}
