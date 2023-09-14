// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.PredicateOperatorUtil.SubstitutedBooleanFormula;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryErrorConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryPostConditionMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.globalinfo.SerializationInfoStorage;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

public class DeserializePredicateStateOperator implements DeserializeOperator {

  private final PredicateCPA predicateCPA;
  private final FormulaManagerView formulaManagerView;
  private final PathFormulaManager pathFormulaManager;
  private final CFA cfa;

  private final PredicateAbstractState previousState;

  public DeserializePredicateStateOperator(
      PredicateCPA pPredicateCPA, CFA pCFA, BlockNode pBlockNode) {
    predicateCPA = pPredicateCPA;
    formulaManagerView = predicateCPA.getSolver().getFormulaManager();
    pathFormulaManager = pPredicateCPA.getPathFormulaManager();
    previousState =
        (PredicateAbstractState)
            predicateCPA.getInitialState(
                pBlockNode.getFirst(), StateSpacePartition.getDefaultPartition());
    cfa = pCFA;
  }

  @Override
  public AbstractState deserialize(BlockSummaryMessage pMessage) throws InterruptedException {
    String formula =
        PredicateOperatorUtil.extractFormulaString(
            pMessage, predicateCPA.getClass(), formulaManagerView);
    SSAMap map = SSAMap.emptySSAMap();
    PointerTargetSet pts = PointerTargetSet.emptyPointerTargetSet();
    SerializationInfoStorage.storeSerializationInformation(predicateCPA, cfa);
    try {
      if (pMessage instanceof BlockSummaryPostConditionMessage bspcm) {
        map = bspcm.getSSAMap();
        pts = bspcm.getPointerTargetSet();
      } else if (pMessage instanceof BlockSummaryErrorConditionMessage bsecm) {
        map = bsecm.getSSAMap();
        pts = bsecm.getPointerTargetSet();
      }
    } finally {
      SerializationInfoStorage.clear();
    }

    PathFormula abstraction =
        PredicateOperatorUtil.getPathFormula(
            formula, pathFormulaManager, formulaManagerView, pts, map);

    PredicateAbstractState deserialized;
    if (pMessage.getType() == MessageType.ERROR_CONDITION) {
      deserialized =
          PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
              abstraction, previousState);
    } else {
      SubstitutedBooleanFormula uninstantiated =
          PredicateOperatorUtil.uninstantiate(abstraction, formulaManagerView);
      deserialized =
          PredicateAbstractState.mkAbstractionState(
              pathFormulaManager.makeEmptyPathFormula(),
              predicateCPA
                  .getPredicateManager()
                  .asAbstraction(
                      uninstantiated.booleanFormula(),
                      pathFormulaManager
                          .makeEmptyPathFormulaWithContext(
                              uninstantiated.ssaMap(), PointerTargetSet.emptyPointerTargetSet())
                          .withFormula(uninstantiated.booleanFormula())),
              PathCopyingPersistentTreeMap.of(),
              previousState);
    }

    return deserialized;
  }
}
