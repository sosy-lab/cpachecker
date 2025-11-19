// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.svlibtoformula;

import static org.sosy_lab.cpachecker.util.predicates.pathformula.svlibtoformula.SvLibToSmtConverterUtils.cleanVariableNameForJavaSMT;

import java.util.Map.Entry;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibHavocStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibProcedureCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibCfaEdgeStatement;
import org.sosy_lab.cpachecker.util.predicates.pathformula.LanguageToSmtConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class SvLibStatementToFormulaConverter {

  public static @NonNull BooleanFormula convertStatement(
      SvLibCfaEdgeStatement pSvLibCfaEdgeStatement, SSAMapBuilder ssa, FormulaManagerView fmgr) {
    return switch (pSvLibCfaEdgeStatement) {
      case SvLibAssignmentStatement pSvLibAssignmentStatement ->
          handleAssignment(pSvLibAssignmentStatement, ssa, fmgr);
      case SvLibHavocStatement pSvLibHavocStatement -> handleHavoc(pSvLibHavocStatement, ssa, fmgr);
      case SvLibProcedureCallStatement pSvLibProcedureCallStatement -> null;
      // TODO: Once we have modules in CPAchecker, we can make sealed classes across packages. Then
      //    this can be removed
      default -> throw new IllegalStateException("Unexpected value: " + pSvLibCfaEdgeStatement);
    };
  }

  private static @NonNull BooleanFormula handleAssignment(
      SvLibAssignmentStatement pSvLibAssignmentStatement,
      SSAMapBuilder ssa,
      FormulaManagerView fmgr) {
    BooleanFormula result = fmgr.getBooleanFormulaManager().makeTrue();
    for (Entry<SvLibSimpleDeclaration, SvLibTerm> assignment :
        pSvLibAssignmentStatement.getAssignments().entrySet()) {
      Formula rightHandSideTerm =
          SvLibTermToFormulaConverter.convertTerm(assignment.getValue(), ssa, fmgr);
      Formula assignedVariable =
          LanguageToSmtConverter.makeFreshVariable(
              cleanVariableNameForJavaSMT(assignment.getKey().getQualifiedName()),
              assignment.getKey().getType(),
              ssa,
              fmgr);

      result =
          fmgr.getBooleanFormulaManager()
              .and(result, fmgr.assignment(assignedVariable, rightHandSideTerm));
    }

    return result;
  }

  private static @NonNull BooleanFormula handleHavoc(
      SvLibHavocStatement pSvLibHavocStatement, SSAMapBuilder ssa, FormulaManagerView fmgr) {
    for (SvLibSimpleDeclaration variableToHavoc : pSvLibHavocStatement.getVariables()) {
      // In JavaSMT, we cannot directly express "havoc", so we assign a fresh variable without
      // any constraints.
      // This is effectively equivalent to havoc in the context of SSA.
      // Therefore, we do not need to add any additional constraints to the result formula.
      LanguageToSmtConverter.makeFreshVariable(
          cleanVariableNameForJavaSMT(variableToHavoc.getQualifiedName()),
          variableToHavoc.getType(),
          ssa,
          fmgr);
    }

    return fmgr.getBooleanFormulaManager().makeTrue();
  }
}
