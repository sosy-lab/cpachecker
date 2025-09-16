// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.DssSerializeObjectUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentReader;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class DeserializeValueAnalysisStateOperator implements DeserializeOperator {
  final Map<String, Type> accessedVariables;

  public DeserializeValueAnalysisStateOperator(BlockNode pBlockNode) {
    accessedVariables = getAccessedVariables(pBlockNode);
  }

  @Override
  public AbstractState deserialize(DssMessage pMessage) throws InterruptedException {
    ContentReader valueContent = pMessage.getAbstractStateContent(ValueAnalysisState.class);
    String serializedValue = valueContent.get(STATE_KEY);
    Preconditions.checkNotNull(serializedValue, "Value state must be provided");

    ValueAnalysisState state;
    try {
      state = DssSerializeObjectUtil.deserialize(serializedValue, ValueAnalysisState.class);
      havocVariables(state, accessedVariables);
      return state;

    } catch (ClassCastException e) {
      throw new RuntimeException("Could not deserialize constraints");
    }
  }

  public static void havocVariables(
      ValueAnalysisState pState, Map<String, Type> pAccessedVariables) {

    for (Map.Entry<String, Type> entry : pAccessedVariables.entrySet()) {
      MemoryLocation mL = MemoryLocation.fromQualifiedName(entry.getKey());
      if (pState.contains(mL)) continue;
      pState.assignConstant(
          mL, SymbolicValueFactory.getInstance().newIdentifier(mL), entry.getValue());
    }
  }

  public static Map<String, Type> getAccessedVariables(BlockNode pBlockNode) {
    HashMap<String, Type> accessedVariables = new HashMap<>();
    ImmutableSet<CFAEdge> edges = pBlockNode.getEdges();
    for (CFAEdge edge : edges) {
      // TODO other types of edges?
      if (edge instanceof CAssumeEdge cAssumeEdge) {
        accessedVariables.putAll(
            CFAUtils.getIdExpressionsOfExpression(cAssumeEdge.getExpression()).stream()
                .collect(
                    Collectors.toMap(
                        id -> id.getDeclaration().getQualifiedName(),
                        id -> id.getDeclaration().getType())));
      }
      if (!(edge instanceof AStatementEdge aStatementEdge)
          || !(aStatementEdge.getStatement() instanceof CAssignment cAssignment)) continue;

      if (cAssignment instanceof CFunctionCallAssignmentStatement cFunAssignment) {
        for (CExpression expr :
            cFunAssignment.getFunctionCallExpression().getParameterExpressions())
          accessedVariables.putAll(
              CFAUtils.getIdExpressionsOfExpression(expr).stream()
                  .collect(
                      Collectors.toMap(
                          id -> id.getDeclaration().getQualifiedName(),
                          id -> id.getDeclaration().getType())));
      }
      if (cAssignment instanceof CExpressionAssignmentStatement cExprAssignment) {
        accessedVariables.putAll(
            CFAUtils.getIdExpressionsOfExpression(cExprAssignment.getRightHandSide()).stream()
                .collect(
                    Collectors.toMap(
                        id -> id.getDeclaration().getQualifiedName(),
                        id -> id.getDeclaration().getType())));
      }
    }
    return accessedVariables;
  }
}
