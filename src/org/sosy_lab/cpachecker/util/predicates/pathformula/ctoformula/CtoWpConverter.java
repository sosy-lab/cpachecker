// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializers;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder.DummyPointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class CtoWpConverter extends CtoFormulaConverter {

  private final Constraints constraints;
  private final PointerTargetSetBuilder pts;
  private final ErrorConditions errorConditions;

  public CtoWpConverter(
      FormulaEncodingOptions pOptions,
      FormulaManagerView pFmgr,
      MachineModel pMachineModel,
      Optional<VariableClassification> pVariableClassification,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CtoFormulaTypeHandler pTypeHandler,
      AnalysisDirection pDirection) {

    super(
        pOptions,
        pFmgr,
        pMachineModel,
        pVariableClassification,
        pLogger,
        pShutdownNotifier,
        pTypeHandler,
        pDirection);

    constraints = new Constraints(bfmgr);
    pts = DummyPointerTargetSetBuilder.INSTANCE;
    errorConditions = ErrorConditions.dummyInstance(bfmgr);
  }

  public BooleanFormula makePreconditionForEdge(final CFAEdge pEdge, final BooleanFormula pPostcond)
      throws UnrecognizedCFAEdgeException, UnrecognizedCodeException, InterruptedException {

    String functionName = pEdge.getPredecessor().getFunctionName();

    switch (pEdge.getEdgeType()) {
      case StatementEdge:
        {
          return makePreconditionForStatement((CStatementEdge) pEdge, pPostcond, functionName);
        }
      case ReturnStatementEdge:
        {
          final var edge = (CReturnStatementEdge) pEdge;
          return makePreconditionForReturn(edge.asAssignment(), edge, pPostcond, functionName);
        }
      case DeclarationEdge:
        {
          final CDeclarationEdge edge = (CDeclarationEdge) pEdge;
          if (edge.getDeclaration() instanceof CVariableDeclaration) {
            return makePreconditionForVarDeclaration(
                edge, (CVariableDeclaration) edge.getDeclaration(), pPostcond, functionName);
          } else {
            return pPostcond;
          }
        }
      case AssumeEdge:
        {
          return makePreconditionForAssumption((CAssumeEdge) pEdge, pPostcond, functionName);
        }
      case FunctionCallEdge:
        {
          return makePreconditionForFunctionCall(
              (CFunctionCallEdge) pEdge, pPostcond, functionName);
        }
      case FunctionReturnEdge:
        {
          return makePreconditionForFunctionExit(
              ((CFunctionReturnEdge) pEdge).getSummaryEdge(), pPostcond);
        }
      case BlankEdge:
        {
          return pPostcond;
        }
      default:
        throw new UnrecognizedCFAEdgeException(pEdge);
    }
  }

  private static SSAMapBuilder emptySSAMap() {
    return SSAMap.emptySSAMap().builder();
  }

  private BooleanFormula makePreconditionForStatement(
      final CStatementEdge pEdge, final BooleanFormula pPostcond, final String pFunction)
      throws UnrecognizedCodeException {

    CStatement stmt = pEdge.getStatement();
    if (stmt instanceof CAssignment) {
      return makePreconditionForAssignment((CAssignment) stmt, pEdge, pPostcond, pFunction);

    } else {
      if (stmt instanceof CFunctionCallStatement) {
        return makePreconditionForFunctionCallStatement(
            pEdge, (CFunctionCallStatement) stmt, pPostcond, pFunction);
      } else if (!(stmt instanceof CExpressionStatement)) {
        throw new UnrecognizedCodeException("Unknown statement", pEdge, stmt);
      }

      return pPostcond;
    }
  }

  private BooleanFormula makePreconditionForReturn(
      final Optional<CAssignment> pAssgn,
      final CReturnStatementEdge pEdge,
      final BooleanFormula pPostcond,
      final String pFunction)
      throws UnrecognizedCodeException {

    if (!pAssgn.isPresent()) {
      // void return, i.e. no substitution needed
      return pPostcond;
    } else {
      return makePreconditionForAssignement(
          pAssgn.orElseThrow().getLeftHandSide(),
          pAssgn.orElseThrow().getRightHandSide(),
          pEdge,
          pPostcond,
          pFunction);
    }
  }

  private BooleanFormula makePreconditionForAssumption(
      final CAssumeEdge pEdge, final BooleanFormula pPostcond, final String pFunction)
      throws UnrecognizedCodeException, InterruptedException {

    BooleanFormula f =
        makePredicate(
            pEdge.getExpression(),
            pEdge.getTruthAssumption(),
            pEdge,
            pFunction,
            emptySSAMap(),
            pts,
            constraints,
            errorConditions);

    return bfmgr.and(fmgr.uninstantiate(f), pPostcond);
  }

  private BooleanFormula makePreconditionForAssignment(
      final CAssignment pAssgn,
      final CFAEdge pEdge,
      final BooleanFormula pPostcond,
      final String pFunction)
      throws UnrecognizedCodeException {

    CLeftHandSide lhs = pAssgn.getLeftHandSide();
    CRightHandSide rhs = pAssgn.getRightHandSide();

    CType lhsType = lhs.getExpressionType().getCanonicalType();

    if (lhsType instanceof CArrayType) {
      // CtoFormulaConverter converts such assignment to True,
      // hence the post-condition
      return pPostcond;
    }

    if (rhs instanceof CExpression) {
      rhs = makeCastFromArrayToPointerIfNecessary((CExpression) rhs, lhsType);
    }

    return makePreconditionForAssignement(lhs, rhs, pEdge, pPostcond, pFunction);
  }

  private BooleanFormula makePreconditionForAssignement(
      final CLeftHandSide lhs,
      final CRightHandSide rhs,
      final CFAEdge pEdge,
      final BooleanFormula pPostcond,
      final String pFunction)
      throws UnrecognizedCodeException {

    //    Formula l, r;
    //    if (direction == AnalysisDirection.BACKWARD) {
    //      l = buildLvalueTerm(lhs, pEdge, pFunction, emptySSAMap(), pts, constraints,
    // errorConditions);
    //      r = buildTerm(rhs, pEdge, pFunction, emptySSAMap(), pts, constraints, errorConditions);
    //    } else {
    //      r = buildTerm(rhs, pEdge, pFunction, emptySSAMap(), pts, constraints, errorConditions);
    //      l = buildLvalueTerm(lhs, pEdge, pFunction, emptySSAMap(), pts, constraints,
    // errorConditions);
    //    }

    var l =
        buildLvalueTerm(lhs, pEdge, pFunction, emptySSAMap(), pts, constraints, errorConditions);
    var r = buildTerm(rhs, pEdge, pFunction, emptySSAMap(), pts, constraints, errorConditions);

    final Map<Formula, Formula> substitution = new HashMap<>();
    substitution.put(fmgr.uninstantiate(l), fmgr.uninstantiate(r));

    return fmgr.substitute(pPostcond, substitution);
  }

  private BooleanFormula makePreconditionForVarDeclaration(
      final CDeclarationEdge pEdge,
      final CVariableDeclaration pDecl,
      final BooleanFormula pPostcond,
      final String pFunction)
      throws UnrecognizedCodeException {

    checkForLargeArray(pEdge, pDecl.getType().getCanonicalType());

    // Ignore initializer lists as it does not make any sense for
    // preconditions without pointer aliasing

    BooleanFormula result = pPostcond;
    for (CAssignment assignment : CInitializers.convertToAssignments(pDecl, pEdge)) {
      result = makePreconditionForAssignment(assignment, pEdge, result, pFunction);
    }

    return result;
  }

  private BooleanFormula makePreconditionForFunctionCall(
      final CFunctionCallEdge pEdge, final BooleanFormula pPostcond, final String pFunction)
      throws UnrecognizedCodeException {

    final CFunctionCallExpression callExpr = pEdge.getFunctionCallExpression();

    final var params = callExpr.getDeclaration().getParameters();
    final var paramsExprs = callExpr.getParameterExpressions();

    if (params.size() != paramsExprs.size()) {
      throw new UnrecognizedCodeException(
          "Number of parameters on function call does " + "not match function definition", pEdge);
    }

    var result = pPostcond;
    final var vars = fmgr.extractVariables(pPostcond);

    for (int i = 0; i < params.size(); i++) {
      final var param = params.get(i).getQualifiedName();
      final var paramVar = vars.get(param);

      if (paramVar != null) {
        final var expr = paramsExprs.get(i);
        final var exprFormula =
            buildTerm(expr, pEdge, pFunction, emptySSAMap(), pts, constraints, errorConditions);

        final var substitution = new HashMap<Formula, Formula>();
        substitution.put(paramVar, fmgr.uninstantiate(exprFormula));

        result = fmgr.substitute(result, substitution);
      }
    }

    return result;
  }

  private BooleanFormula makePreconditionForFunctionExit(
      final CFunctionSummaryEdge pEdge, final BooleanFormula pPostcond)
      throws UnrecognizedCodeException {

    var retExp = pEdge.getExpression();
    if (retExp instanceof CFunctionCallStatement) {
      // void return, i.e. no substitution needed
      return pPostcond;
    } else if (retExp instanceof CFunctionCallAssignmentStatement) {
      // substitute lhs in the post-condition by the func's return variable
      final var callStmt = (CFunctionCallAssignmentStatement) retExp;
      final var callExpr = callStmt.getRightHandSide();

      final var callerFunction = pEdge.getSuccessor().getFunctionName();
      final var retVarDecl = pEdge.getFunctionEntry().getReturnVariable();
      if (!retVarDecl.isPresent()) {
        throw new UnrecognizedCodeException("Void function used in assignment", pEdge, retExp);
      }

      final var rhs = new CIdExpression(callExpr.getFileLocation(), retVarDecl.orElseThrow());
      return makePreconditionForAssignement(
          callStmt.getLeftHandSide(), rhs, pEdge, pPostcond, callerFunction);
    } else {
      throw new UnrecognizedCodeException("Unknown function exit expression", pEdge, retExp);
    }
  }

  private BooleanFormula makePreconditionForFunctionCallStatement(
      final CStatementEdge pEdge,
      final CFunctionCall pStmt,
      final BooleanFormula pPostcond,
      final String pFunction)
      throws UnrecognizedCodeException {

    if (pStmt instanceof CFunctionCallStatement) {
      return pPostcond;
    } else if (pStmt instanceof CFunctionCallAssignmentStatement) {
      final var callStmt = (CFunctionCallAssignmentStatement) pStmt;
      return makePreconditionForAssignement(
          callStmt.getLeftHandSide(), callStmt.getRightHandSide(), pEdge, pPostcond, pFunction);
    } else {
      throw new UnrecognizedCodeException("Unknown call statement", pEdge, pStmt);
    }
  }
}
