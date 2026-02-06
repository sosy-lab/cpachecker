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
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.DssSerializeObjectUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentReader;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage.DssMessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
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
  private final BlockNode blockNode;

  public DeserializePredicateStateOperator(
      PredicateCPA pPredicateCPA, CFA pCFA, BlockNode pBlockNode) {
    predicateCPA = pPredicateCPA;
    formulaManagerView = predicateCPA.getSolver().getFormulaManager();
    pathFormulaManager = pPredicateCPA.getPathFormulaManager();
    cfa = pCFA;
    blockNode = pBlockNode;
  }

  @Override
  public AbstractState deserialize(DssMessage pMessage) throws InterruptedException {
    SerializationInfoStorage.storeSerializationInformation(predicateCPA, cfa);
    ContentReader predicateContent = pMessage.getAbstractStateContent(PredicateAbstractState.class);
    try {
      String serializedSsaMap = predicateContent.get(SerializePredicateStateOperator.SSA_KEY);
      Preconditions.checkNotNull(serializedSsaMap, "SSA Map must be provided");
      SSAMap map = DssSerializeObjectUtil.deserialize(serializedSsaMap, SSAMap.class);

      String serializedPts = predicateContent.get(SerializePredicateStateOperator.PTS_KEY);
      Preconditions.checkNotNull(serializedPts, "Pointer target set (PTS) must be provided");
      PointerTargetSet pts =
          DssSerializeObjectUtil.deserialize(serializedPts, PointerTargetSet.class);

      String serializedState = predicateContent.get(STATE_KEY);
      Preconditions.checkNotNull(serializedState, "State must be provided");

      PathFormula abstraction =
          PredicateOperatorUtil.getPathFormula(
              serializedState, pathFormulaManager, formulaManagerView, pts, map);

      if (pMessage.getType() == DssMessageType.VIOLATION_CONDITION) {
        return PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
            abstraction,
            (PredicateAbstractState)
                predicateCPA.getInitialState(
                    blockNode.getInitialLocation(), StateSpacePartition.getDefaultPartition()));
      } else {
        return PredicateAbstractState.mkAbstractionState(
            abstraction,
            predicateCPA
                .getPredicateManager()
                .asAbstraction(
                    formulaManagerView.uninstantiate(abstraction.getFormula()), abstraction),
            PathCopyingPersistentTreeMap.copyOf(
                ImmutableMap.<CFANode, Integer>builder()
                    .put(blockNode.getInitialLocation(), 1)
                    .buildOrThrow()));
      }
    } finally {
      SerializationInfoStorage.clear();
    }
  }
}
