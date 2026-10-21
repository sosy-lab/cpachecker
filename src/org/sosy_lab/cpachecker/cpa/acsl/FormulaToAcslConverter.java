// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.Tactic;

/** Class for converting a formula to an Acsl expression. */
@SuppressWarnings("unused")
public class FormulaToAcslConverter {
  private final FormulaManagerView fmgr;
  private final MachineModel machineModel;
  private HashMap<String, Integer> exceptionCounter;

  public FormulaToAcslConverter(FormulaManagerView pFmgr, MachineModel pModel) {
    this.fmgr = pFmgr;
    this.machineModel = pModel;
    this.exceptionCounter = new HashMap<>();
  }

  /** Convert the input formula to an Acsl expression. */
  public AcslPredicate formulaToAcslExpression(BooleanFormula input, SSAMap pSSAMap)
      throws InterruptedException, SolverException {
    // Simplification options:
    try {
      // fails every time with Z3 Invalid argument
      BooleanFormula nnfied = fmgr.applyTactic(input, Tactic.NNF);
    } catch (Exception e) {
      incrementExceptionCounter("NNF");
    }

    try {
      // fails half the time with Z3 Type error
      BooleanFormula eliminated = fmgr.eliminateDeadVariables(input, pSSAMap);
    } catch (Exception e) {
      incrementExceptionCounter("Elimination");
    }

    try {
      // this is the only one that does not give errors constantly
      BooleanFormula simplified = fmgr.simplify(input);
    } catch (Exception e) {
      incrementExceptionCounter("Simplification");
    }

    try {
      // fails every time with a Z3 sort mismatch
      BooleanFormula uninstantiated = fmgr.uninstantiate(input);
    } catch (Exception e) {
      incrementExceptionCounter("Uninstantiate");
    }

    FormulaToAcslVisitor visitor = new FormulaToAcslVisitor(fmgr, machineModel);
    fmgr.transformRecursively(input, visitor);
    return (AcslPredicate) visitor.getAcslExpressionForFormula(input);
  }

  public void incrementExceptionCounter(String key) {
    exceptionCounter.merge(key, 1, Integer::sum);
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
