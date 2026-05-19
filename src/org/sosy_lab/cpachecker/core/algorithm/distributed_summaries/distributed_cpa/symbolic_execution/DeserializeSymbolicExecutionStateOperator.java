// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.symbolic_execution;

import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.symbolic_execution.SerializeSymbolicExecutionStateOperator.CONSTRAINTS_KEY;
import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.symbolic_execution.SerializeSymbolicExecutionStateOperator.VALUE_KEY;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.DssSerializeObjectUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentReader;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage.DssMessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.symbolicExecution.SymbolicExecutionState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class DeserializeSymbolicExecutionStateOperator implements DeserializeOperator {
  static ConcurrentMap<String, Map<String, Type>> accessedVariables = new ConcurrentHashMap<>();
  BlockNode blockNode;

  public DeserializeSymbolicExecutionStateOperator(BlockNode pBlockNode) {
    blockNode = pBlockNode;
  }

  @Override
  public AbstractState deserialize(DssMessage pMessage) throws InterruptedException {
    ContentReader content = pMessage.getAbstractStateContent(SymbolicExecutionState.class);
    String serializedValue = content.get(VALUE_KEY);

    Preconditions.checkNotNull(serializedValue, "Value analysis state must be provided");
    ValueAnalysisState valueState =
        DssSerializeObjectUtil.deserialize(serializedValue, ValueAnalysisState.class);
    if (pMessage.getType().equals(DssMessageType.POST_CONDITION)) {
      havocVariables(valueState, getAccessedVariables(blockNode));
    }

    String serializedConstraints = content.get(CONSTRAINTS_KEY);
    Preconditions.checkNotNull(serializedConstraints, "Constraints list must be provided");
    ImmutableSet.Builder<Constraint> constraints = ImmutableSet.builder();
    try {
      for (Object o : DssSerializeObjectUtil.deserialize(serializedConstraints, HashSet.class)) {
        assert o instanceof Constraint;
        constraints.add((Constraint) o);
      }
    } catch (ClassCastException e) {
      throw new AssertionError("Could not deserialize constraints", e);
    }

    return new SymbolicExecutionState(valueState, new ConstraintsState(constraints.build()));
  }

  public static void havocVariables(
      ValueAnalysisState pState, Map<String, Type> pAccessedVariables) {

    for (Map.Entry<String, Type> entry : pAccessedVariables.entrySet()) {
      MemoryLocation mL = MemoryLocation.fromQualifiedName(entry.getKey());
      if (pState.contains(mL)) {
        continue;
      }
      pState.assignConstant(
          mL,
          new ConstantSymbolicExpression(
              SymbolicValueFactory.getInstance().newIdentifier(mL), entry.getValue()),
          entry.getValue());
    }
  }

  public Map<String, Type> getAccessedVariables(BlockNode pBlockNode) {
    if (accessedVariables.containsKey(pBlockNode.getId())) {
      return accessedVariables.get(pBlockNode.getId());
    }

    Map<String, Type> accessed = new HashMap<>();
    List<CExpression> expressions = new ArrayList<>();

    for (CFAEdge edge : pBlockNode.getEdges()) {
      // TODO other types of edges?
      if (edge instanceof CAssumeEdge cAssumeEdge) {
        expressions.add(cAssumeEdge.getExpression());
      }

      if (edge instanceof CFunctionCallEdge callEdge) {
        expressions.addAll(callEdge.getArguments());
        expressions.addAll(callEdge.getFunctionCallExpression().getParameterExpressions());
      }

      if (edge instanceof CDeclarationEdge declarationEdge
          && declarationEdge.getDeclaration() instanceof CVariableDeclaration variableDeclaration
          && variableDeclaration.getInitializer() instanceof CInitializerExpression initExpr) {
        expressions.add(initExpr.getExpression());
      }

      if (edge instanceof AStatementEdge aStatementEdge
          && aStatementEdge.getStatement() instanceof CAssignment cAssignment) {
        if (cAssignment instanceof CFunctionCallAssignmentStatement cFunAssignment) {
          expressions.addAll(cFunAssignment.getFunctionCallExpression().getParameterExpressions());
        }
        if (cAssignment instanceof CExpressionAssignmentStatement cExprAssignment) {
          expressions.add(cExprAssignment.getRightHandSide());
        }
      }

      if (edge instanceof CFunctionReturnEdge returnEdge
          && returnEdge.getSummaryEdge().getExpression()
              instanceof CFunctionCallAssignmentStatement cFunAssignment) {
        expressions.add(cFunAssignment.getLeftHandSide());

        final FunctionEntryNode functionEntryNode = returnEdge.getSummaryEdge().getFunctionEntry();
        final Optional<? extends AVariableDeclaration> optionalReturnVarDeclaration =
            functionEntryNode.getReturnVariable();

        if (optionalReturnVarDeclaration.isPresent()) {
          MemoryLocation functionReturnVar =
              MemoryLocation.forDeclaration(optionalReturnVarDeclaration.get());
          final Type functionReturnType =
              functionEntryNode.getFunctionDefinition().getType().getReturnType();
          accessed.put(functionReturnVar.getExtendedQualifiedName(), functionReturnType);
        }
      }

      if (edge instanceof CReturnStatementEdge returnEdge) {
        if (returnEdge.getExpression().isPresent()) {
          expressions.add(returnEdge.getExpression().orElseThrow());
        }
      }
    }

    for (CExpression expr : expressions) {
      CFAUtils.getIdExpressionsOfExpression(expr).stream()
          .collect(
              ImmutableMap.toImmutableMap(
                  id -> id.getDeclaration().getQualifiedName(),
                  id -> id.getDeclaration().getType(),
                  (first, second) -> first))
          .forEach(accessed::putIfAbsent);
    }
    accessedVariables.put(pBlockNode.getId(), accessed);
    return accessed;
  }
}
