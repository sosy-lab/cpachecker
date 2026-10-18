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
import java.util.Map.Entry;
import java.util.Objects;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.DssSerializeObjectUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageWithStates;
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

public class DeserializePredicateStateOperator implements DeserializeOperator {

  private final PredicateCPA predicateCPA;
  private final FormulaManagerView formulaManagerView;
  private final PathFormulaManager pathFormulaManager;
  private final CFA cfa;
  private final BlockNode blockNode;

  private final ImmutableMap<String, Type> variableTypes;
  private final Map<String, CType> numericTypes;

  public DeserializePredicateStateOperator(
      PredicateCPA pPredicateCPA,
      CFA pCFA,
      BlockNode pBlockNode,
      ImmutableMap<String, Type> pVariableTypes) {
    predicateCPA = pPredicateCPA;
    variableTypes = pVariableTypes;
    formulaManagerView = predicateCPA.getSolver().getFormulaManager();
    pathFormulaManager = pPredicateCPA.getPathFormulaManager();
    cfa = pCFA;
    blockNode = pBlockNode;
    numericTypes = getNumericTypes();
  }

  private Map<String, CType> getNumericTypes() {
    ImmutableMap.Builder<String, CType> numericTypesBuilder = ImmutableMap.builder();
    try {
      for (java.lang.reflect.Field f : CNumericTypes.class.getFields()) {
        if (java.lang.reflect.Modifier.isStatic(f.getModifiers())
            && CType.class.isAssignableFrom(f.getType())) {
          numericTypesBuilder.put(((CType) f.get(null)).toString(), (CType) f.get(null));
        }
      }
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Failed to read CNumericTypes constants via reflection", e);
    }
    return numericTypesBuilder.buildKeepingLast();
  }

  @Override
  public AbstractState deserialize(DssMessageWithStates pMessage, int pStateIndex)
      throws InterruptedException {
    SerializationInfoStorage.storeSerializationInformation(predicateCPA, cfa);
    try {
      DssPredicateStatePayload payload =
          (DssPredicateStatePayload)
              Objects.requireNonNull(
                      pMessage
                          .getStates()
                          .get(pStateIndex)
                          .get(DssComponentKey.serializedNameFor(PredicateAbstractState.class)))
                  .state();

      SSAMapBuilder ssaMapBuilder = SSAMap.emptySSAMap().builder();
      for (Entry<String, DssSSAVariablePayload> entry : payload.ssaMap().entrySet()) {
        int index = entry.getValue().index();
        String serializedType = entry.getValue().serializedType();
        Type type = variableTypes.getOrDefault(entry.getKey(), numericTypes.get(serializedType));
        if (type == null) {
          type = DssSerializeObjectUtil.deserialize(serializedType, Type.class);
        }
        ssaMapBuilder.setIndex(entry.getKey(), type, index);
      }

      String serializedPts = payload.pts();
      PointerTargetSet pts =
          DssSerializeObjectUtil.deserialize(serializedPts, PointerTargetSet.class);

      String serializedState = payload.formula();

      PathFormula abstraction =
          PredicateOperatorUtil.getPathFormula(
              serializedState, pathFormulaManager, formulaManagerView, pts, ssaMapBuilder.build());

      if (pMessage.getType() == DssMessage.DssMessageType.VIOLATION_CONDITION) {
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
