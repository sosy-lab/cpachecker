// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.DssSerializeObjectUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentReader;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
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
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class DeserializePredicateStateOperator implements DeserializeOperator {

  private final PredicateCPA predicateCPA;
  private final FormulaManagerView formulaManagerView;
  private final PathFormulaManager pathFormulaManager;
  private final CFA cfa;
  private final PredicateAbstractState previousState;
  private final Map<MemoryLocation, CType> variableTypes;

  public DeserializePredicateStateOperator(
      PredicateCPA pPredicateCPA,
      CFA pCFA,
      BlockNode pBlockNode,
      Map<MemoryLocation, CType> pVariableTypes) {
    predicateCPA = pPredicateCPA;
    formulaManagerView = predicateCPA.getSolver().getFormulaManager();
    pathFormulaManager = pPredicateCPA.getPathFormulaManager();
    previousState =
        (PredicateAbstractState)
            predicateCPA.getInitialState(
                pBlockNode.getInitialLocation(), StateSpacePartition.getDefaultPartition());
    cfa = pCFA;
    variableTypes = pVariableTypes;
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
      Preconditions.checkNotNull(serializedPts, "PTS must be provided");
      PointerTargetSet pts =
          DssSerializeObjectUtil.deserialize(serializedPts, PointerTargetSet.class);

      String serializedState = predicateContent.get(STATE_KEY);
      Preconditions.checkNotNull(serializedState, "State must be provided");

      PathFormula abstraction =
          PredicateOperatorUtil.getPathFormula(
              serializedState, pathFormulaManager, formulaManagerView, pts, map);

      return PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
          abstraction, previousState);
    } finally {
      SerializationInfoStorage.clear();
    }
  }

  @Override
  public PredicateAbstractState deserializeFromFormula(BooleanFormula pFormula) {
    SSAMapBuilder ssaMapBuilder = SSAMap.emptySSAMap().builder();

    for (Entry<String, Formula> entry : formulaManagerView.extractVariables(pFormula).entrySet()) {
      String instantiatedName = entry.getKey();
      List<String> parts = Splitter.on("@").splitToList(instantiatedName);
      String qualifiedName = parts.getFirst();

      MemoryLocation memoryLocation = MemoryLocation.fromQualifiedName(qualifiedName);
      CType variableType = variableTypes.get(memoryLocation);
      if (variableType == null) {
        continue;
      }

      int ssaIndex = 0;
      if (parts.size() > 1) {
        ssaIndex = Integer.parseInt(parts.get(1));
      }

      if (ssaMapBuilder.build().containsVariable(qualifiedName)) {
        int currentIndex = ssaMapBuilder.getIndex(qualifiedName);
        ssaMapBuilder.setIndex(qualifiedName, variableType, Math.max(currentIndex, ssaIndex));
      } else {
        ssaMapBuilder.setIndex(qualifiedName, variableType, ssaIndex);
      }
    }

    PathFormula newPathFormula =
        pathFormulaManager
            .makeEmptyPathFormulaWithContext(
                ssaMapBuilder.build(), PointerTargetSet.emptyPointerTargetSet())
            .withFormula(pFormula);

    return PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
        newPathFormula, previousState);
  }
}
