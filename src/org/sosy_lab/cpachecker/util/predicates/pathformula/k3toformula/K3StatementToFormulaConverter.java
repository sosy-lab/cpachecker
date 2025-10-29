// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.k3toformula;

import java.util.Map.Entry;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3CfaEdgeStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3HavocStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Term;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class K3StatementToFormulaConverter {

  public static @NonNull BooleanFormula convertStatement(
      K3CfaEdgeStatement pK3Statement, SSAMapBuilder ssa, FormulaManagerView fmgr) {
    return switch (pK3Statement) {
      case K3AssignmentStatement pK3AssignmentStatement ->
          handleAssignment(pK3AssignmentStatement, ssa, fmgr);
      case K3HavocStatement pK3HavocStatement -> null;
      case K3ProcedureCallStatement pK3ProcedureCallStatement -> null;
    };
  }

  private static @NonNull BooleanFormula handleAssignment(
      K3AssignmentStatement pK3AssignmentStatement, SSAMapBuilder ssa, FormulaManagerView fmgr) {
    BooleanFormula result = fmgr.getBooleanFormulaManager().makeTrue();
    for (Entry<K3SimpleDeclaration, K3Term> assignment :
        pK3AssignmentStatement.getAssignments().entrySet()) {
      Formula rightHandSideTerm =
          K3TermToFormulaConverter.convertTerm(assignment.getValue(), ssa, fmgr);
      Formula assignedVariable =
          SSAHandler.makeFreshVariable(
              assignment.getKey().getQualifiedName(), assignment.getKey().getType(), ssa, fmgr);

      result =
          fmgr.getBooleanFormulaManager()
              .and(result, fmgr.assignment(assignedVariable, rightHandSideTerm));
    }

    return result;
  }
}
