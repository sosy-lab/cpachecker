/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.ltl.formulas;

import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.cpachecker.util.ltl.LtlFormulaVisitor;

/** Finally. */
public final class Finally extends UnaryFormula {

  public Finally(LtlFormula f) {
    super(f);
  }

  public static LtlFormula of(LtlFormula pOperand) {
    if (pOperand instanceof BooleanConstant) {
      return pOperand;
    }

    if (pOperand instanceof Finally) {
      return pOperand;
    }

    if (pOperand instanceof Globally && ((Globally) pOperand).getOperand() instanceof Finally) {
      return pOperand;
    }

    if (pOperand instanceof Until) {
      return of(((Until) pOperand).getRight());
    }

    if (pOperand instanceof Disjunction) {
      return new Disjunction(
          Collections3.transformedImmutableListCopy(
              ((Disjunction) pOperand).getChildren(), Finally::of));
    }

    return new Finally(pOperand);
  }

  @Override
  public String getSymbol() {
    return "F";
  }

  @Override
  public Globally not() {
    return new Globally(getOperand().not());
  }

  @Override
  public String accept(LtlFormulaVisitor v) {
    return v.visit(this);
  }
}
