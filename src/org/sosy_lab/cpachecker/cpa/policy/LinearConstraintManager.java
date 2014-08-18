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

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.rationals.ExtendedRational;
import org.sosy_lab.cpachecker.util.rationals.LinearConstraint;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

import java.util.Map;

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


  LinearConstraintManager(FormulaManagerView pFmgr) {
    bfmgr = pFmgr.getBooleanFormulaManager();
    rfmgr = pFmgr.getRationalFormulaManager();
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
   *
   * @param expr Linear expression to convert.
   * @param pSSAMap Version number for each variable.
   * @return NumeralFormula for the SMT solver.
   *
   */
  NumeralFormula linearExpressionToFormula(
      LinearExpression expr, SSAMap pSSAMap) {

    NumeralFormula sum = rfmgr.makeNumber("0");
    for (Map.Entry<String, ExtendedRational> entry : expr) {

      String origVarName = entry.getKey();

      // Return the variable name according to the SSA map.
      String varName = FormulaManagerView.makeName(origVarName,
          pSSAMap.getIndex(origVarName)
      );

      final NumeralFormula item = rfmgr.multiply(
          rfmgr.makeVariable(varName),
          rfmgr.makeNumber(entry.getValue().toString())
      );
      sum = rfmgr.add(sum, item);
    }

    return sum;
  }
}
