/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.policy;

import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.rationals.ExtendedRational;
import org.sosy_lab.cpachecker.util.rationals.LinearConstraint;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

import com.google.common.base.Preconditions;

/**
 * Wrapper for a linear constraint:
 *
 * Note that since {@link ExtendedRational} can be <code>NaN</code>
 * this class can throw exceptions when converting linear constraints to
 * formulas.
 */
public class LinearConstraintManager {

  private final BooleanFormulaManagerView bfmgr;
  private final NumeralFormulaManager<
      NumeralFormula, NumeralFormula.RationalFormula> rfmgr;
  private final LogManager logger;

  LinearConstraintManager(
      FormulaManagerView pFmgr,
      LogManager logger
      ) {
    bfmgr = pFmgr.getBooleanFormulaManager();
    rfmgr = pFmgr.getRationalFormulaManager();
    this.logger = logger;
  }

  /**
   * @param constraint Constraint to convert.
   * @param pSSAMap Map which contains the versioning index for each variable.
   * @return formula which can be passed to a solver.
   */
  BooleanFormula linearConstraintToFormula(
      LinearConstraint constraint, SSAMap pSSAMap) {

    Preconditions.checkState(
        constraint.getBound().getType() != ExtendedRational.NumberType.NaN,
        "Constraints can not contain the number NaN"
    );

    switch (constraint.getBound().getType()) {
       case NEG_INFTY:
        return bfmgr.makeBoolean(false);
      case INFTY:
        return bfmgr.makeBoolean(true);
      case RATIONAL:
        return rfmgr.lessOrEquals(
            linearExpressionToFormula(constraint.getExpression(), pSSAMap),
            rfmgr.makeNumber(constraint.getBound().toString())
        );
      default:
        throw new RuntimeException(
            "Internal Error, unexpected formula");
    }
  }

  /**
   * @param expr Linear expression to convert.
   * @param pSSAMap Version number for each variable.
   *
   * @return NumeralFormula for the SMT solver.
   * @throws UnsupportedOperationException if the conversion can not be
   * performed.
   */
  NumeralFormula linearExpressionToFormula(
      LinearExpression expr, SSAMap pSSAMap)
      throws UnsupportedOperationException {
    return linearExpressionToFormula(expr, pSSAMap, "");
  }

  /**
   * @param expr Linear expression to convert.
   * @param pSSAMap Version number for each variable.
   * @param customPrefix Custom string prefix to add before each variable.
   *
   * @return NumeralFormula for the SMT solver.
   * @throws UnsupportedOperationException if the conversion can not be
   * performed.
   */
  NumeralFormula linearExpressionToFormula(
      LinearExpression expr, SSAMap pSSAMap, String customPrefix
  ) {

    NumeralFormula sum = null;
    for (Map.Entry<String, ExtendedRational> entry : expr) {
      ExtendedRational coeff = entry.getValue();
      String origVarName = entry.getKey();

      // Return the variable name according to the SSA map.
      String varName = customPrefix + FormulaManagerView.makeName(
          origVarName, pSSAMap.getIndex(origVarName)
      );
      NumeralFormula item;
      if (coeff.getType() != ExtendedRational.NumberType.RATIONAL) {
        throw new UnsupportedOperationException(
            "Can not convert the expression " + expr);
      } else if (coeff.equals(ExtendedRational.ZERO)) {
        continue;
      } else if (coeff.equals(ExtendedRational.ONE)) {
        item = rfmgr.makeVariable(varName);
      } else if (coeff.equals(ExtendedRational.NEG_ONE)) {
        item = rfmgr.negate(rfmgr.makeVariable(varName));
      } else {
        item = rfmgr.multiply(
            rfmgr.makeVariable(varName),
            rfmgr.makeNumber(entry.getValue().toString())
        );
      }

      if (sum == null) {
        sum = item;
      } else {
        sum = rfmgr.add(sum, item);
      }
    }

    if (sum == null) {
      return rfmgr.makeNumber("0");
    } else {
      return sum;
    }
  }


  /**
   * @param prover Prover engine used
   * @param expression Expression to maximize
   * @return Returned value in the extended rational field.
   *
   * @throws SolverException, InterruptedException
   */
  ExtendedRational maximize(
      OptEnvironment prover, LinearExpression expression, SSAMap pSSAMap
  ) throws SolverException, InterruptedException {

    NumeralFormula objective = linearExpressionToFormula(expression, pSSAMap);

    return maximize(prover, objective);
  }

  /**
   *  Lower-level API allowing one to provide the actual formula.
   */
  ExtendedRational maximize(
      OptEnvironment prover, NumeralFormula objective
  ) throws SolverException, InterruptedException {

    // We can only maximize a single variable.
    // Create a new variable, make it equal to the linear expression which we
    // have.
    FreshVariable target = FreshVariable.createFreshVar(rfmgr);
    BooleanFormula constraint =
        rfmgr.equal(
            target.variable, objective
        );
    prover.addConstraint(constraint);
    prover.setObjective(target.variable);

    OptEnvironment.OptResult result = prover.maximize();

    switch (result) {
      case OPT:
        Model model = prover.getModel();
        logger.log(Level.FINEST, "OPT");
        return (ExtendedRational) model.get(
            new Model.Constant(target.name(), Model.TermType.Real)
        );
      case UNSAT:
        logger.log(Level.FINEST, "UNSAT");
        return ExtendedRational.NEG_INFTY;
      case UNBOUNDED:
        logger.log(Level.FINEST, "UNBOUNDED");
        return ExtendedRational.INFTY;
      case UNDEF:
        logger.log(Level.FINEST, "UNDEFINED");
        throw new SolverException("Result undefiend: something is wrong");
      default:
        logger.log(Level.FINEST, "ERROR RUNNING OPTIMIZATION");
        throw new RuntimeException("Internal Error, unaccounted case");
    }
  }
}
