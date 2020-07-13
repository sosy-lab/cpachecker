/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
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

  public PathFormula makePrecondition(
      final PathFormula pOldFormula, final CFAEdge pEdge)
      throws UnrecognizedCFAEdgeException, UnrecognizedCodeException, InterruptedException {

    String function = (pEdge.getPredecessor() != null)
                      ? pEdge.getPredecessor().getFunctionName() : null;

    var postcond = pOldFormula.getFormula();
    var ssa = pOldFormula.getSsa().builder();

    var newFormula = makePreconditionForEdge(pEdge, function, postcond, ssa);
    var newSsa = ssa.build();

    int newLength = pOldFormula.getLength() + 1;
    return new PathFormula(newFormula, newSsa, pOldFormula.getPointerTargetSet(), newLength);
  }

  private BooleanFormula makePreconditionForEdge(
      final CFAEdge pEdge, final String pFunction, final BooleanFormula pPostcond, final SSAMapBuilder ssa)
      throws UnrecognizedCFAEdgeException, UnrecognizedCodeException, InterruptedException {


    switch (pEdge.getEdgeType()) {
      case StatementEdge:
        {
          return makePreconditionForStatement((CStatementEdge) pEdge, pFunction, pPostcond, ssa);
        }
      case ReturnStatementEdge:
        {
          final var edge = (CReturnStatementEdge)pEdge;
          return makePreconditionForReturn(edge.asAssignment(), edge, pFunction, pPostcond, ssa);
        }
      case DeclarationEdge:
        {
          final CDeclarationEdge edge = (CDeclarationEdge) pEdge;
          if (edge.getDeclaration() instanceof CVariableDeclaration) {
            return makePreconditionForVarDeclaration(
                edge, (CVariableDeclaration) edge.getDeclaration(), pFunction, pPostcond, ssa);
          } else {
            return pPostcond;
          }
        }
      case AssumeEdge:
        {
          return makePreconditionForAssumption((CAssumeEdge) pEdge, pFunction, pPostcond, ssa);
        }
      case FunctionCallEdge:
        {
          return makePreconditionForFunctionCall((CFunctionCallEdge)pEdge, pFunction, pPostcond, ssa);
        }
      case FunctionReturnEdge:
        {
          return makePreconditionForFunctionExit(((CFunctionReturnEdge)pEdge).getSummaryEdge(), pPostcond, ssa);
        }
      case BlankEdge:
        {
          return pPostcond;
        }
      default:
        throw new UnrecognizedCFAEdgeException(pEdge);
    }
  }



  private BooleanFormula makePreconditionForStatement(
      final CStatementEdge pEdge, final String pFunction, final BooleanFormula pPostcond, final SSAMapBuilder ssa)
      throws UnrecognizedCodeException {

    CStatement stmt = pEdge.getStatement();
    if (stmt instanceof CFunctionCall) {
      return makePreconditionForFunctionCallStatement((CFunctionCall)stmt, pEdge, pFunction, pPostcond, ssa);
    } else if (stmt instanceof CAssignment) {
      return makePreconditionForAssignment((CAssignment) stmt, pEdge, pFunction, pPostcond, ssa);
    } else if (!(stmt instanceof CExpressionStatement)) {
      throw new UnrecognizedCodeException("Unknown statement", pEdge, stmt);
    }

    return pPostcond;
  }

  private BooleanFormula makePreconditionForReturn(
      final com.google.common.base.Optional<CAssignment> pAssgn, final CReturnStatementEdge pEdge,
      final String pFunction,
      final BooleanFormula pPostcond,
      final SSAMapBuilder ssa)
      throws UnrecognizedCodeException {

    if (!pAssgn.isPresent()) {
      // void return, i.e. no substitution needed
      return pPostcond;
    } else {
      return makePreconditionForAssignement(
          pAssgn.get().getLeftHandSide(), pAssgn.get().getRightHandSide(),
          pEdge, pFunction, pPostcond, ssa);
    }
  }

  private BooleanFormula makePreconditionForAssumption(
      final CAssumeEdge pEdge, final String pFunction, final BooleanFormula pPostcond, final SSAMapBuilder ssa)
      throws UnrecognizedCodeException, InterruptedException {

    BooleanFormula f =
        makePredicate(
            pEdge.getExpression(),
            pEdge.getTruthAssumption(),
            pEdge,
            pFunction,
            ssa,
            pts,
            constraints,
            errorConditions);

    return bfmgr.and(fmgr.uninstantiate(f), pPostcond);
  }

  private BooleanFormula makePreconditionForAssignment(
      final CAssignment pAssgn, final CFAEdge pEdge,
      final String pFunction, final BooleanFormula pPostcond, final SSAMapBuilder ssa)
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
      rhs = this.makeCastFromArrayToPointerIfNecessary((CExpression) rhs, lhsType);
    }

    return makePreconditionForAssignement(lhs, rhs, pEdge, pFunction, pPostcond, ssa);
  }


  private BooleanFormula makePreconditionForAssignement(
    final CLeftHandSide lhs, final CRightHandSide rhs, final CFAEdge pEdge,
    final String pFunction, final BooleanFormula pPostcond, final SSAMapBuilder ssa)
      throws UnrecognizedCodeException {

//    Formula l, r;
//    if (direction == AnalysisDirection.BACKWARD) {
//      l = buildLvalueTerm(lhs, pEdge, pFunction, emptySSAMap(), pts, constraints, errorConditions);
//      r = buildTerm(rhs, pEdge, pFunction, emptySSAMap(), pts, constraints, errorConditions);
//    } else {
//      r = buildTerm(rhs, pEdge, pFunction, emptySSAMap(), pts, constraints, errorConditions);
//      l = buildLvalueTerm(lhs, pEdge, pFunction, emptySSAMap(), pts, constraints, errorConditions);
//    }

    var l = buildLvalueTerm(lhs, pEdge, pFunction, ssa, pts, constraints, errorConditions);
    var r = buildTerm(rhs, pEdge, pFunction, ssa, pts, constraints, errorConditions);

    var subst = makeSubstitution(l, r, lhs.getExpressionType(), rhs.getExpressionType(), pEdge);

    return fmgr.substitute(pPostcond, subst);
  }


  private BooleanFormula makePreconditionForVarDeclaration(
      final CDeclarationEdge pEdge, final CVariableDeclaration pDecl,
      final String pFunction, final BooleanFormula pPostcond, final SSAMapBuilder ssa)
      throws UnrecognizedCodeException {

    checkForLargeArray(pEdge, pDecl.getType().getCanonicalType());

    // Ignore initializer lists as it does not make any sense for
    // preconditions without pointer aliasing

    BooleanFormula result = pPostcond;
    for (CAssignment assignment : CInitializers.convertToAssignments(pDecl, pEdge)) {
      result = makePreconditionForAssignment(assignment, pEdge, pFunction, result, ssa);
    }

    return result;
  }

  private BooleanFormula makePreconditionForFunctionCall(
      final CFunctionCallEdge pEdge, final String pFunction, final BooleanFormula pPostcond, final SSAMapBuilder ssa)
      throws UnrecognizedCodeException {


    final var callStmt = pEdge.getRawAST().orNull();
    if(callStmt == null){
      throw new UnrecognizedCodeException("Unknown function call statement", pEdge);
    }


    final var callExpr = callStmt.getFunctionCallExpression();
    if(callExpr == null){
      throw new UnrecognizedCodeException("Unknown function call expression", pEdge, callStmt);
    }


    final var params = callExpr.getDeclaration().getParameters();
    final var paramsExprs = callExpr.getParameterExpressions();

    if (params.size() != paramsExprs.size()) {
      throw new UnrecognizedCodeException(
          "Number of parameters on function call does " + "not match function definition", pEdge);
    }


    var result = pPostcond;
    final var vars = fmgr.extractVariables(pPostcond);

    for(int i = 0; i < params.size(); i++) {
      final var param = params.get(i);
      final var paramName = param.getQualifiedName();
      final var paramVar = vars.get(paramName);

      if (paramVar != null) {
        final var expr = paramsExprs.get(i);
        final var exprFormula = buildTerm(expr, pEdge, pFunction, ssa, pts, constraints, errorConditions);

        var subst = makeSubstitution(paramVar, exprFormula, param.getType(), expr.getExpressionType(), pEdge);

        result = fmgr.substitute(result, subst);
      }
    }

    return result;
  }

  private BooleanFormula makePreconditionForFunctionExit(
      final CFunctionSummaryEdge pEdge, final BooleanFormula pPostcond, final SSAMapBuilder ssa)
      throws UnrecognizedCodeException {

    var retExp = pEdge.getExpression();
    if (retExp instanceof CFunctionCallStatement) {
      // void return, i.e. no substitution needed
      return pPostcond;
    } else if (retExp instanceof CFunctionCallAssignmentStatement) {
      // substitute lhs in the post-condition by the func's return variable
      final var callStmt = (CFunctionCallAssignmentStatement)retExp;
      final var callExpr = callStmt.getRightHandSide();

      final var callerFunction = pEdge.getSuccessor().getFunctionName();
      final var retVarDecl =  pEdge.getFunctionEntry().getReturnVariable();
      if (!retVarDecl.isPresent()) {
        throw new UnrecognizedCodeException("Void function used in assignment", pEdge, retExp);
      }

      final var rhs = new CIdExpression(callExpr.getFileLocation(), retVarDecl.get());
      return makePreconditionForAssignement(callStmt.getLeftHandSide(), rhs, pEdge, callerFunction, pPostcond, ssa);
    } else {
      throw new UnrecognizedCodeException("Unknown function exit expression", pEdge, retExp);
    }
  }

  private BooleanFormula makePreconditionForFunctionCallStatement(
      final CFunctionCall pStmt, final CStatementEdge pEdge,
      final String pFunction, final BooleanFormula pPostcond, final SSAMapBuilder ssa)
      throws UnrecognizedCodeException {

    if (pStmt instanceof CFunctionCallStatement) {
      return pPostcond;

    } else if (pStmt instanceof CFunctionCallAssignmentStatement) {
      var stmt = (CFunctionCallAssignmentStatement)pStmt;

      var lhs = buildLvalueTerm(stmt.getLeftHandSide(), pEdge, pFunction, ssa, pts, constraints, errorConditions);
      var rhs = buildTerm(stmt.getRightHandSide(), pEdge, pFunction, ssa, pts, constraints, errorConditions);

      var subst = makeSubstitution(lhs, rhs,
          stmt.getLeftHandSide().getExpressionType(),
          stmt.getRightHandSide().getExpressionType(), pEdge);

      return fmgr.substitute(pPostcond, subst);

    } else {
      throw new UnrecognizedCodeException("Unknown call statement", pEdge, pStmt);
    }
  }

  private Map<Formula, Formula> makeSubstitution(
      final Formula lhs, final Formula rhs, final CType lhsType, final CType rhsType, final CFAEdge edge)
      throws UnrecognizedCodeException {

    var rhsCasted = makeCast(rhsType, lhsType, rhs, constraints, edge);

    Map<Formula, Formula> substitution = new HashMap<>();
    substitution.put(fmgr.uninstantiate(lhs), fmgr.uninstantiate(rhsCasted));

    return substitution;
  }
}
