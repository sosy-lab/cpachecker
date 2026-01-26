// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.SerializePredicateStateOperator.PTS_KEY;
import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.SerializePredicateStateOperator.SSA_KEY;
import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value.SerializeValueAnalysisStateOperator.FORMULA_KEY;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.CFA;
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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.globalinfo.SerializationInfoStorage;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class DeserializeValueAnalysisStateOperator implements DeserializeOperator {
  static Map<String, Map<String, Type>> accessedVariables = new HashMap<>();
  private final BlockNode blockNode;
  private final FormulaManagerView fmgr;
  private final PathFormulaManagerImpl pfgmr;
  private final ValueAnalysisCPA valueAnalysisCPA;
  private final CFA cfa;

  public DeserializeValueAnalysisStateOperator(
      BlockNode pBlockNode, ValueAnalysisCPA pValueAnalysisCPA, CFA pCFA) {
    valueAnalysisCPA = pValueAnalysisCPA;
    cfa = pCFA;
    blockNode = pBlockNode;
    fmgr = valueAnalysisCPA.getBlockStrengtheningOperator().getSolver().getFormulaManager();
    pfgmr = valueAnalysisCPA.getBlockStrengtheningOperator().getPfmgr();
  }

  @Override
  public AbstractState deserialize(DssMessage pMessage) throws InterruptedException {
    ContentReader valueContent = pMessage.getAbstractStateContent(ValueAnalysisState.class);
    String serializedValue = valueContent.get(STATE_KEY);
    Preconditions.checkNotNull(serializedValue, "Value state must be provided");

    String ssa = valueContent.pushLevel(STATE_KEY).get(SSA_KEY);
    String formula = valueContent.get(FORMULA_KEY);
    String pts = valueContent.get(PTS_KEY);
    valueContent.popLevel();

    SerializationInfoStorage.storeSerializationInformation(valueAnalysisCPA, cfa);
    ValueAnalysisState state;
    try {
      state = DssSerializeObjectUtil.deserialize(serializedValue, ValueAnalysisState.class);
      if (!ssa.isEmpty() && !formula.isEmpty()) {
        SSAMap ssaMap = DssSerializeObjectUtil.deserialize(ssa, SSAMap.class);
        PointerTargetSet targetSet =
            DssSerializeObjectUtil.deserialize(pts, PointerTargetSet.class);
        BooleanFormula parsed = fmgr.parse(formula);
        state.setViolationCondition(
            pfgmr.makeEmptyPathFormulaWithContext(ssaMap, targetSet).withFormula(parsed));
      }
      havocVariables(state, getAccessedVariables(blockNode));
      return state;

    } catch (ClassCastException e) {
      throw new RuntimeException("Could not deserialize constraints");
    } finally {
      SerializationInfoStorage.clear();
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

  public Map<String, Type> getAccessedVariables(BlockNode pBlockNode) {
    if (accessedVariables.containsKey(pBlockNode.getId()))
      return accessedVariables.get(pBlockNode.getId());

    HashMap<String, Type> accessed = new HashMap<>();
    ImmutableSet<CFAEdge> edges = pBlockNode.getEdges();
    List<CExpression> expressions = new ArrayList<>();

    for (CFAEdge edge : edges) {
      // TODO other types of edges?
      if (edge instanceof CAssumeEdge cAssumeEdge) expressions.add(cAssumeEdge.getExpression());

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

      if (edge instanceof CReturnStatementEdge returnEdge)
        if (returnEdge.getExpression().isPresent())
          expressions.add(returnEdge.getExpression().get());
    }

    for (CExpression expr : expressions) {
      CFAUtils.getIdExpressionsOfExpression(expr).stream()
          .collect(
              Collectors.toMap(
                  id -> id.getDeclaration().getQualifiedName(),
                  id -> id.getDeclaration().getType(),
                  (first, second) -> first))
          .forEach(accessed::putIfAbsent);
    }
    accessedVariables.put(pBlockNode.getId(), accessed);
    return accessed;
  }
}
