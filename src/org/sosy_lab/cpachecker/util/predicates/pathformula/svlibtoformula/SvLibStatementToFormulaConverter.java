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
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibCfaEdgeStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibHavocStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibProcedureCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
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
      case SvLibHavocStatement pSvLibHavocStatement -> null;
      case SvLibProcedureCallStatement pSvLibProcedureCallStatement -> null;
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
          SSAHandler.makeFreshVariable(
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
}
