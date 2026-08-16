// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.svlibtoformula;

import static org.sosy_lab.cpachecker.util.predicates.pathformula.svlibtoformula.SvLibToSmtConverterUtils.cleanVariableNameForJavaSMT;

import com.google.common.base.Verify;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibCfaEdgeStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTermAssignmentCfaStatement;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class SvLibStatementToFormulaConverter {

  public static @NonNull BooleanFormula convertStatement(
      SvLibCfaEdgeStatement pSvLibCfaEdgeStatement,
      SSAMapBuilder ssa,
      FormulaManagerView fmgr,
      // TODO: This is very ugly, but I don't see an easy way around it at the moment
      SvLibToFormulaConverter pConverter) {
    return switch (pSvLibCfaEdgeStatement) {
      case SvLibTermAssignmentCfaStatement pSvLibAssignment ->
          handleTermAssignment(pSvLibAssignment, ssa, fmgr, pConverter);
      case SvLibFunctionCallAssignmentStatement pSvLibProcedureCallStatement ->
          handleCallAssignment(pSvLibProcedureCallStatement, ssa, fmgr, pConverter);
    };
  }

  private static @NonNull BooleanFormula handleTermAssignment(
      SvLibTermAssignmentCfaStatement pSvLibAssignment,
      SSAMapBuilder ssa,
      FormulaManagerView fmgr,
      SvLibToFormulaConverter pConverter) {
    BooleanFormula result = fmgr.getBooleanFormulaManager().makeTrue();
    Formula rightHandSideTerm =
        SvLibTermToFormulaConverter.convertTerm(
            pSvLibAssignment.getRightHandSide(), ssa, fmgr, pConverter);

    SvLibSimpleDeclaration declaration = pSvLibAssignment.getLeftHandSide().getDeclaration();
    Formula assignedVariable =
        pConverter.makeFreshVariable(
            cleanVariableNameForJavaSMT(declaration.getQualifiedName()),
            declaration.getType(),
            ssa,
            fmgr);

    return fmgr.getBooleanFormulaManager()
        .and(result, fmgr.assignment(assignedVariable, rightHandSideTerm));
  }

  private static @NonNull BooleanFormula handleCallAssignment(
      SvLibFunctionCallAssignmentStatement pSvLibFunctionCallAssignmentStatement,
      SSAMapBuilder ssa,
      FormulaManagerView fmgr,
      SvLibToFormulaConverter pConverter) {
    SvLibFunctionDeclaration functionDeclaration =
        pSvLibFunctionCallAssignmentStatement.getRightHandSide().getDeclaration();
    if (functionDeclaration.equals(
        SvLibFunctionDeclaration.nondetFunctionWithReturnType(
            functionDeclaration.getType().getReturnType()))) {
      Verify.verify(
          pSvLibFunctionCallAssignmentStatement.getLeftHandSide().getIdTerms().size() == 1,
          "Havoc function calls can only assign to a single variable.");

      SvLibSimpleDeclaration variableToHavoc =
          pSvLibFunctionCallAssignmentStatement
              .getLeftHandSide()
              .getIdTerms()
              .getFirst()
              .getDeclaration();

      // Handle nondet function call
      pConverter.makeFreshVariable(
          cleanVariableNameForJavaSMT(variableToHavoc.getQualifiedName()),
          variableToHavoc.getType(),
          ssa,
          fmgr);
      return fmgr.getBooleanFormulaManager().makeTrue();
    }

    throw new UnsupportedOperationException("Function calls are not yet supported.");
  }
}
