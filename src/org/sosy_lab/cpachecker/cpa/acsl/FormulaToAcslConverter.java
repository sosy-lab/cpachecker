// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/** Class for converting a formula to an Acsl expression. */
public class FormulaToAcslConverter {
  private final FormulaManagerView fmgr;
  private final MachineModel machineModel;

  public FormulaToAcslConverter(FormulaManagerView pFmgr, MachineModel pModel) {
    this.fmgr = pFmgr;
    this.machineModel = pModel;
  }

  /** Convert the input formula to an Acsl expression. */
  public AcslPredicate formulaToAcslExpression(BooleanFormula input) {

    FormulaToAcslVisitor visitor = new FormulaToAcslVisitor(fmgr, machineModel);
    fmgr.transformRecursively(input, visitor);
    return (AcslPredicate) visitor.getAcslExpressionForFormula(input);
  }

  public ImmutableList<AcslPredicate> flattenAnds(AcslPredicate p) {
    if (p instanceof AcslBinaryPredicate and
        && and.getOperator() == AcslBinaryPredicate.AcslBinaryPredicateOperator.AND) {
      ImmutableList.Builder<AcslPredicate> builder = ImmutableList.builder();
      builder.addAll(flattenAnds(and.getOperand1()));
      builder.addAll(flattenAnds(and.getOperand2()));
      return builder.build();
    } else {
      return ImmutableList.of(p);
    }
  }
}
