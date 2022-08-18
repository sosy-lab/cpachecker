// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryErrorConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryPostConditionMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class DeserializePredicateStateOperator implements DeserializeOperator {

  private final PredicateCPA predicateCPA;
  private final FormulaManagerView formulaManagerView;
  private final PathFormulaManager pathFormulaManager;
  private final BlockNode block;
  private BooleanFormula errorCondition;

  public DeserializePredicateStateOperator(
      PredicateCPA pPredicateCPA,
      FormulaManagerView pFormulaManagerView,
      PathFormulaManager pPathFormulaManager,
      BlockNode pBlockNode) {
    predicateCPA = pPredicateCPA;
    formulaManagerView = pFormulaManagerView;
    pathFormulaManager = pPathFormulaManager;
    block = pBlockNode;
    errorCondition = pFormulaManagerView.getBooleanFormulaManager().makeFalse();
  }

  @Override
  public AbstractState deserialize(BlockSummaryMessage pMessage) throws InterruptedException {
    String formula =
        PredicateOperatorUtil.extractFormulaString(
            pMessage, predicateCPA.getClass(), formulaManagerView);
    SSAMap map = SSAMap.emptySSAMap();
    PointerTargetSet pts = PointerTargetSet.emptyPointerTargetSet();
    if (pMessage instanceof BlockSummaryPostConditionMessage) {
      map = ((BlockSummaryPostConditionMessage) pMessage).getSSAMap();
      pts = ((BlockSummaryPostConditionMessage) pMessage).getPointerTargetSet();
    } else if (pMessage instanceof BlockSummaryErrorConditionMessage) {
      map = ((BlockSummaryErrorConditionMessage) pMessage).getSSAMap();
      pts = ((BlockSummaryErrorConditionMessage) pMessage).getPointerTargetSet();
    }

    PathFormula abstraction =
        PredicateOperatorUtil.getPathFormula(
            formula, pathFormulaManager, formulaManagerView, pts, map);

    PredicateAbstractState previousState =
        (PredicateAbstractState)
            predicateCPA.getInitialState(
                block.getNodeWithNumber(pMessage.getTargetNodeNumber()),
                StateSpacePartition.getDefaultPartition());

    PredicateAbstractState deserialized;
    if (pMessage.getType() == MessageType.ERROR_CONDITION) {
      deserialized =
          PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
              abstraction, previousState);
    } else {
      deserialized =
          PredicateAbstractState.mkAbstractionState(
              abstraction,
              predicateCPA
                  .getPredicateManager()
                  .asAbstraction(abstraction.getFormula(), abstraction),
              PathCopyingPersistentTreeMap.of(),
              previousState);
    }

    if (pMessage instanceof BlockSummaryErrorConditionMessage) {
      errorCondition =
          formulaManagerView.uninstantiate(
              PredicateOperatorUtil.uninstantiate(
                      abstraction, formulaManagerView, pathFormulaManager)
                  .getFormula());
    }
    return deserialized;
  }

  void updateErrorCondition(BlockSummaryErrorConditionMessage pMessage) {
    String formula =
        PredicateOperatorUtil.extractFormulaString(
            pMessage, predicateCPA.getClass(), formulaManagerView);
    SSAMap map = pMessage.getSSAMap();
    PointerTargetSet pts = pMessage.getPointerTargetSet();
    PathFormula abstraction =
        PredicateOperatorUtil.getPathFormula(
            formula, pathFormulaManager, formulaManagerView, pts, map);
    errorCondition =
        formulaManagerView.uninstantiate(
            PredicateOperatorUtil.uninstantiate(abstraction, formulaManagerView, pathFormulaManager)
                .getFormula());
  }

  public BooleanFormula getErrorCondition(FormulaManagerView pFormulaManagerView) {
    return pFormulaManagerView.translateFrom(errorCondition, formulaManagerView);
  }

  public void setErrorCondition(BooleanFormula pErrorCondition) {
    if (pErrorCondition != null) {
      errorCondition = pErrorCondition;
    }
  }
}
