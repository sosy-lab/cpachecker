// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.precondition;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula.TraceFormulaOptions;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

public class VariableAssignmentPreConditionComposer implements PreConditionComposer {

  private final FormulaContext context;
  private final TraceFormulaOptions options;
  private final boolean includeInitialAssignment;

  public VariableAssignmentPreConditionComposer(
      FormulaContext pContext, TraceFormulaOptions pOptions, boolean pWithInitialAssignment) {
    context = pContext;
    options = pOptions;
    includeInitialAssignment = pWithInitialAssignment;
  }

  @Override
  public PreCondition extractPreCondition(List<CFAEdge> pCounterexample)
      throws SolverException, InterruptedException, CPATransferException {
    PreCondition nondets = createNondetPrecondition(pCounterexample);
    if (!includeInitialAssignment) {
      return nondets;
    }
    List<CFAEdge> remainingCounterexample = new ArrayList<>();
    List<CFAEdge> preconditionEdges = new ArrayList<>();
    Set<String> coveredVariables = new HashSet<>();
    for (CFAEdge cfaEdge : pCounterexample) {
      // check if current edge declares or initializes a variable
      if (cfaEdge.getEdgeType() != CFAEdgeType.DeclarationEdge
          && cfaEdge.getEdgeType() != CFAEdgeType.StatementEdge) {
        remainingCounterexample.add(cfaEdge);
        continue;
      }
      // check if variable was declared in allowed function scopes
      if (!options
          .getFunctionsForPrecondition()
          .contains(cfaEdge.getPredecessor().getFunction().getQualifiedName())) {
        remainingCounterexample.add(cfaEdge);
        continue;
      }
      // add declaration edge to precondition if it initializes a variable with literals
      if (cfaEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
        CDeclarationEdge declarationEdge = (CDeclarationEdge) cfaEdge;
        String qualifiedName = declarationEdge.getDeclaration().getQualifiedName();
        if (coveredVariables.contains(qualifiedName) || !handleDeclarationEdge(declarationEdge)) {
          remainingCounterexample.add(cfaEdge);
        } else {
          preconditionEdges.add(cfaEdge);
        }
        coveredVariables.add(qualifiedName);
        continue;
      }
      // add statement edges of not yet covered variables that have a literal on the right-hand
      // side
      if (cfaEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
        CStatementEdge statementEdge = (CStatementEdge) cfaEdge;
        if (statementEdge.getStatement() instanceof CFunctionCallAssignmentStatement) {
          CFunctionCallAssignmentStatement statement =
              (CFunctionCallAssignmentStatement) statementEdge.getStatement();
          coveredVariables.add(statement.getLeftHandSide().toQualifiedASTString());
          remainingCounterexample.add(cfaEdge);
          continue;
        }
        if (statementEdge.getStatement() instanceof CExpressionAssignmentStatement) {
          CExpressionAssignmentStatement statement =
              (CExpressionAssignmentStatement) statementEdge.getStatement();
          String qualifiedName = statement.getLeftHandSide().toQualifiedASTString();
          if (coveredVariables.contains(qualifiedName)
              || !(statement.getRightHandSide() instanceof CLiteralExpression)) {
            remainingCounterexample.add(cfaEdge);
          } else {
            preconditionEdges.add(cfaEdge);
          }
          coveredVariables.add(qualifiedName);
          continue;
        }
      }
      remainingCounterexample.add(cfaEdge);
    }
    FormulaManagerView fmgr = context.getSolver().getFormulaManager();
    return new PreCondition(
        preconditionEdges,
        remainingCounterexample,
        fmgr.uninstantiate(
            fmgr.getBooleanFormulaManager()
                .and(
                    nondets.getPrecondition(),
                    context.getManager().makeFormulaForPath(preconditionEdges).getFormula())));
  }

  private PreCondition createNondetPrecondition(List<CFAEdge> pCounterexample)
      throws SolverException, InterruptedException, CPATransferException {
    BooleanFormulaManager bmgr = context.getSolver().getFormulaManager().getBooleanFormulaManager();
    BooleanFormula precond = bmgr.makeTrue();
    try (ProverEnvironment prover = context.getProver()) {
      prover.push(context.getManager().makeFormulaForPath(pCounterexample).getFormula());
      Preconditions.checkArgument(!prover.isUnsat(), "a model has to be existent");
      for (ValueAssignment modelAssignment : prover.getModelAssignments()) {
        context.getLogger().log(Level.FINEST, "tfprecondition=" + modelAssignment);
        BooleanFormula formula = modelAssignment.getAssignmentAsFormula();
        if (!Pattern.matches(".+::.+@[0-9]+", modelAssignment.getKey().toString())) {
          precond = bmgr.and(precond, formula);
        }
      }
      return new PreCondition(
          ImmutableList.of(),
          pCounterexample,
          context.getSolver().getFormulaManager().uninstantiate(precond));
    }
  }

  private boolean handleDeclarationEdge(CDeclarationEdge declarationEdge) {
    // only variable declarations can be part of preconditions
    if (!(declarationEdge.getDeclaration() instanceof CVariableDeclaration)) {
      return false;
    }

    CVariableDeclaration variableDeclaration =
        (CVariableDeclaration) declarationEdge.getDeclaration();
    // variable must not be excluded when added to precondition
    if (options.getExcludeFromPrecondition().contains(variableDeclaration.getQualifiedName())) {
      return false;
    }

    CInitializer initializer = variableDeclaration.getInitializer();
    // variable must be initialized to be part of the precondition;
    if (initializer == null) {
      return false;
    }

    // arrays have to have literals only
    if (initializer instanceof CInitializerList) {
      CInitializerList listInitializer = (CInitializerList) initializer;
      List<CInitializer> waitlist = new ArrayList<>(listInitializer.getInitializers());
      while (!waitlist.isEmpty()) {
        CInitializer next = waitlist.remove(0);
        if (next instanceof CInitializerList) {
          waitlist.addAll(((CInitializerList) next).getInitializers());
          continue;
        }
        if (next instanceof CInitializerExpression) {
          CInitializerExpression expression = (CInitializerExpression) next;
          if (!(expression.getExpression() instanceof CLiteralExpression)) {
            return false;
          }
        }
      }
      return true;
    }

    // must only be initialized with literals
    if (initializer instanceof CInitializerExpression) {
      CInitializerExpression expression = (CInitializerExpression) initializer;
      return expression.getExpression() instanceof CLiteralExpression;
    }
    return false;
  }
}
