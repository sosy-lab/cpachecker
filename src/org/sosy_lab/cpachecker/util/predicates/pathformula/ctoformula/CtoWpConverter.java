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
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializers;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
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
      throws UnrecognizedCFAEdgeException, UnrecognizedCCodeException, InterruptedException {

    String functionName = pEdge.getPredecessor().getFunctionName();

    switch (pEdge.getEdgeType()) {
      case StatementEdge:
        {
          return makePreconditionForStatement((CStatementEdge) pEdge, pPostcond, functionName);
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
      case ReturnStatementEdge:
        {
          return pPostcond;
        }
      case BlankEdge:
        {
          return pPostcond;
        }
      default:
        throw new UnrecognizedCFAEdgeException(pEdge);
    }
  }

  private static final SSAMapBuilder emptySSAMap() {
    return SSAMap.emptySSAMap().builder();
  }

  private final BooleanFormula makePreconditionForAssumption(
      final CAssumeEdge pEdge, final BooleanFormula pPostcond, final String pFunction)
      throws UnrecognizedCCodeException, InterruptedException {

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

  private final BooleanFormula makePreconditionForStatement(
      final CStatementEdge pEdge, final BooleanFormula pPostcond, final String pFunction)
      throws UnrecognizedCCodeException {

    CStatement stmt = pEdge.getStatement();
    if (stmt instanceof CAssignment) {
      return makePreconditionForAssignment((CAssignment) stmt, pEdge, pPostcond, pFunction);

    } else {
      if (stmt instanceof CFunctionCallStatement) {
        // TODO: add support
        throw new UnrecognizedCCodeException(
            "Preconditions for a function call are not supported", pEdge);

      } else if (!(stmt instanceof CExpressionStatement)) {
        throw new UnrecognizedCCodeException("Unknown statement", pEdge, stmt);
      }

      return pPostcond;
    }
  }

  private final BooleanFormula makePreconditionForAssignment(
      final CAssignment pAssgn,
      final CFAEdge pEdge,
      final BooleanFormula pPostcond,
      final String pFunction)
      throws UnrecognizedCCodeException {

    CLeftHandSide lhs = pAssgn.getLeftHandSide();
    CRightHandSide rhs = pAssgn.getRightHandSide();

    CType lhsType = lhs.getExpressionType().getCanonicalType();

    if (lhsType instanceof CArrayType) {
      // CtoFormulaConverter converts such assignment to True,
      // hence the postcondition
      return pPostcond;
    }

    if (rhs instanceof CExpression) {
      rhs = this.makeCastFromArrayToPointerIfNecessary((CExpression) rhs, lhsType);
    }

    Formula l = null, r = null;
    if (direction == AnalysisDirection.BACKWARD) {
      l = buildLvalueTerm(lhs, pEdge, pFunction, emptySSAMap(), pts, constraints, errorConditions);
      r = buildTerm(rhs, pEdge, pFunction, emptySSAMap(), pts, constraints, errorConditions);
    } else {
      r = buildTerm(rhs, pEdge, pFunction, emptySSAMap(), pts, constraints, errorConditions);
      l = buildLvalueTerm(lhs, pEdge, pFunction, emptySSAMap(), pts, constraints, errorConditions);
    }

    final Map<Formula, Formula> substitution = new HashMap<>();
    substitution.put(fmgr.uninstantiate(l), fmgr.uninstantiate(r));

    BooleanFormula result = fmgr.substitute(pPostcond, substitution);

    return result;
  }

  private final BooleanFormula makePreconditionForVarDeclaration(
      final CDeclarationEdge pEdge,
      final CVariableDeclaration pDecl,
      final BooleanFormula pPostcond,
      final String pFunction)
      throws UnrecognizedCCodeException {

    checkForLargeArray(pEdge, pDecl.getType().getCanonicalType());

    // Ignore initializer lists as it does not make any sense for
    // preconditions without pointer aliasing

    BooleanFormula result = pPostcond;
    for (CAssignment assignment : CInitializers.convertToAssignments(pDecl, pEdge)) {
      result = makePreconditionForAssignment(assignment, pEdge, result, pFunction);
    }

    return result;
  }
}
