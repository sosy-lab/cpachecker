// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.precondition;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
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
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.InvalidCounterexampleException;
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
  private final boolean includeDeclaredPreconditionVariables;

  public VariableAssignmentPreConditionComposer(
      FormulaContext pContext,
      TraceFormulaOptions pOptions,
      boolean pWithInitialAssignment,
      boolean pWithDeclaredPreconditionVariables) {
    context = pContext;
    options = pOptions;
    includeInitialAssignment = pWithInitialAssignment;
    includeDeclaredPreconditionVariables = pWithDeclaredPreconditionVariables;
  }

  @Override
  public PreCondition extractPreCondition(List<CFAEdge> pCounterexample)
      throws SolverException,
          InterruptedException,
          CPATransferException,
          InvalidCounterexampleException {
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
      if (includeDeclaredPreconditionVariables) {
        if (cfaEdge.getDescription().contains("__FAULT_LOCALIZATION_precondition")) {
          // ignore.
          continue;
        }
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
        if (statementEdge.getStatement() instanceof CFunctionCallAssignmentStatement statement) {

          coveredVariables.add(statement.getLeftHandSide().toQualifiedASTString());
          remainingCounterexample.add(cfaEdge);
          continue;
        }
        if (statementEdge.getStatement() instanceof CExpressionAssignmentStatement statement) {

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
                    context.getManager().makeFormulaForPath(preconditionEdges).getFormula())),
        nondets.getNondetVariables());
  }

  private PreCondition createNondetPrecondition(List<CFAEdge> pCounterexample)
      throws SolverException,
          InterruptedException,
          CPATransferException,
          InvalidCounterexampleException {
    BooleanFormulaManager bmgr = context.getSolver().getFormulaManager().getBooleanFormulaManager();
    BooleanFormula precond = bmgr.makeTrue();
    ImmutableSet.Builder<String> nondetVariables = ImmutableSet.builder();
    try (ProverEnvironment prover = context.getProver()) {
      prover.push(context.getManager().makeFormulaForPath(pCounterexample).getFormula());
      if (prover.isUnsat()) {
        throw new InvalidCounterexampleException(
            "Precondition cannot be computed since counterexample is not feasible.");
      }
      for (ValueAssignment modelAssignment : prover.getModelAssignments()) {
        context.getLogger().log(Level.FINEST, "tfprecondition=" + modelAssignment);
        BooleanFormula formula = modelAssignment.getAssignmentAsFormula();
        if (modelAssignment.getName().contains("nondet")) {
          precond = bmgr.and(precond, formula);
          nondetVariables.add(modelAssignment.getName());
        } else if (modelAssignment.getName().startsWith("__FAULT_LOCALIZATION_precondition")) {
          precond = bmgr.and(precond, formula);
        }
      }
      return new PreCondition(
          ImmutableList.of(),
          pCounterexample,
          context.getSolver().getFormulaManager().uninstantiate(precond),
          nondetVariables.build());
    }
  }

  private boolean handleDeclarationEdge(CDeclarationEdge declarationEdge) {
    // only variable declarations can be part of preconditions
    if (!(declarationEdge.getDeclaration() instanceof CVariableDeclaration variableDeclaration)) {
      return false;
    }

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
    if (initializer instanceof CInitializerList listInitializer) {
      List<CInitializer> waitlist = new ArrayList<>(listInitializer.getInitializers());
      while (!waitlist.isEmpty()) {
        CInitializer next = waitlist.remove(0);
        if (next instanceof CInitializerList cInitializerList) {
          waitlist.addAll(cInitializerList.getInitializers());
          continue;
        }
        if ((next instanceof CInitializerExpression expression)
            && !(expression.getExpression() instanceof CLiteralExpression)) {
          return false;
        }
      }
      return true;
    }

    // must only be initialized with literals
    return initializer instanceof CInitializerExpression expression
        && expression.getExpression() instanceof CLiteralExpression;
  }
}
