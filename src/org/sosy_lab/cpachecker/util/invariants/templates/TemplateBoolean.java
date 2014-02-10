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
package org.sosy_lab.cpachecker.util.invariants.templates;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;


public abstract class TemplateBoolean extends TemplateFormula implements BooleanFormula {

  @Override
  public TemplateBoolean copy() {
    return null;
  }

  public abstract void prefixVariables(String prefix);

  public abstract void flatten();

  public abstract TemplateBoolean makeCNF();

  public abstract TemplateBoolean makeDNF();

  /**
   * "Strong CNF", pulls negations into atomic formulas.
   */
  public TemplateBoolean makeSCNF() {
    TemplateBoolean tb = absorbNegations();
    tb = tb.makeCNF();
    return tb;
  }

  /**
   * "Strong DNF", pulls negations into atomic formulas.
   */
  public TemplateBoolean makeSDNF() {
    TemplateBoolean tb = absorbNegations();
    tb = tb.makeDNF();
    return tb;
  }

  /**
   * There are different implementations of the Formula and FormulaManager interfaces,
   * meaning essentially that formulas can be constructed in various "languages". If
   * you want to translate a TemplateFormula into the corresponding formula in another
   * "language", you just call this method, passing a FormulaManager for the language
   * that you want.
   */
  @Override
  public BooleanFormula translate(FormulaManagerView fmgr) {
    return null;
  }

  @Override
  public FormulaType<BooleanFormula> getFormulaType() {
    return FormulaType.BooleanType;
  }

  /**
   * Apply a negation, and alter the boolean object accordingly.
   * The exact effect depends on the type of boolean:
   * Negation: cancel the negation.
   * Conjunction: apply DeMorgan's law.
   * Disjunction: apply DeMorgan's law.
   * Constraints:
   *   LEQ: flip arguments, switch to LT
   *   LT:  flip arguments, switch to LEQ
   *   EQ:  make disjunction of both possible LT relns on arguments
   */
  public abstract TemplateBoolean logicNegate();

  /**
   * Push negations in the entire syntax tree all the way down
   * to the literals, and then absorb them into the atomic relations,
   * flipping inequalities appropriately.
   */
  public abstract TemplateBoolean absorbNegations();

}
