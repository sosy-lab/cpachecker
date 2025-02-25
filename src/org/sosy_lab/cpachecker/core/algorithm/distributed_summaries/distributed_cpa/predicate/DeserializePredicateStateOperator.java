// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.base.Splitter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryErrorConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryPostConditionMessage;
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
    variableTypes = pVariableTypes;
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

    return PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
        abstraction, previousState);
  }

  @Override
  public PredicateAbstractState deserializeFromFormula(
      org.sosy_lab.java_smt.api.BooleanFormula pFormula) {
    SSAMapBuilder ssaMapBuilder = SSAMap.emptySSAMap().builder();
    for (Entry<String, Formula> entry : formulaManagerView.extractVariables(pFormula).entrySet()) {
      String qualifiedVariableName =
          formulaManagerView.dumpArbitraryFormula(
              formulaManagerView.uninstantiate(entry.getValue()));
      // String qualifiedVariableName =
      // formulaManagerView.uninstantiate(entry.getValue()).toString();
      String instantiatedVariable = entry.getValue().toString();

      CType variableType =
          variableTypes.get(MemoryLocation.fromQualifiedName(qualifiedVariableName));
      if (variableType == null) {
        throw new IllegalArgumentException(
            "Variable " + qualifiedVariableName + " not found in variable types map.");
      }
      int ssaIndex = 0;
      if (!qualifiedVariableName.equals(instantiatedVariable)) {
        List<String> parts = Splitter.on("@").splitToList(instantiatedVariable);
        ssaIndex = Integer.parseInt(parts.get(1));
      }
      if (ssaMapBuilder.build().containsVariable(qualifiedVariableName)) {
        int currentIndex = ssaMapBuilder.getIndex(qualifiedVariableName);
        ssaMapBuilder.setIndex(
            qualifiedVariableName, variableType, Integer.max(currentIndex, ssaIndex));
      } else {
        ssaMapBuilder.setIndex(qualifiedVariableName, variableType, ssaIndex);
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
