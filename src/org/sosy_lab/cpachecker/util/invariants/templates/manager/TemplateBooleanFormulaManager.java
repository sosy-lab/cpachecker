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
package org.sosy_lab.cpachecker.util.invariants.templates.manager;

import java.util.List;

import org.sosy_lab.cpachecker.util.invariants.templates.NonTemplate;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateBoolean;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateConjunction;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateDisjunction;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFalse;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateNegation;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateTrue;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;


public class TemplateBooleanFormulaManager implements BooleanFormulaManager {

  private TemplateFormulaManager manager;

  public TemplateBooleanFormulaManager(TemplateFormulaManager manager) {
    this.manager = manager;
  }

  @Override
  public boolean isBoolean(Formula pF) {
    // OLD:
    // //For TemplateFormulas, to be boolean is to be a subclass of
    // //TemplateConjunction.
    //return TemplateConjunction.isInstance(pF);
    return (pF instanceof TemplateBoolean);
  }

  /**
   * @return a Formula representing the given logical value
   */
  @Override
  public BooleanFormula makeBoolean(boolean value) {
    return value ? new TemplateTrue() : new TemplateFalse();
  }

  /**
   * Creates a formula representing a negation of the argument.
   * @param f a Formula
   * @return (!f1)
   */
  @Override
  public BooleanFormula not(BooleanFormula f) {
    BooleanFormula F = null;
    try {
      TemplateBoolean b = (TemplateBoolean) f;
      F = TemplateNegation.negate(b);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate(getFormulaType());
    }
    return F;
  }

  /**
   * Creates a formula representing an AND of the two arguments.
   * @param f1 a Formula
   * @param f2 a Formula
   * @return (f1 & f2)
   */
  @Override
  public BooleanFormula and(BooleanFormula f1, BooleanFormula f2) {
    BooleanFormula F = null;
    try {
      TemplateBoolean b1 = (TemplateBoolean) f1;
      TemplateBoolean b2 = (TemplateBoolean) f2;
      F = TemplateConjunction.conjoin(b1, b2);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate(getFormulaType());
    }
    // Below is the old method, in which we make True of anything
    // that won't case to a Boolean. It shouldn't be necessary, but
    // might let us continue to work in some odd case.
    /*
    TemplateBoolean b1, b2;
    try {
      b1 = (TemplateBoolean) f1;
    } catch (ClassCastException e) {
      b1 = new TemplateTrue();
    }
    try {
      b2 = (TemplateBoolean) f2;
    } catch (ClassCastException e) {
      b2 = new TemplateTrue();
    }
    F = TemplateConjunction.conjoin(b1, b2);
    */
    return F;
  }

  @Override
  public BooleanFormula and(List<BooleanFormula> pBits) {
    BooleanFormula result = makeBoolean(true);
    for (BooleanFormula f : pBits) {
      result = and(result, f);
    }
    return result;
  }

  /**
   * Creates a formula representing an OR of the two arguments.
   * @param f1 a Formula
   * @param f2 a Formula
   * @return (f1 | f2)
   */
  @Override
  public BooleanFormula or(BooleanFormula f1, BooleanFormula f2) {
    BooleanFormula F = null;
    try {
      TemplateBoolean b1 = (TemplateBoolean) f1;
      TemplateBoolean b2 = (TemplateBoolean) f2;
      F = TemplateDisjunction.disjoin(b1, b2);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate(getFormulaType());
    }
    return F;
  }

  /**
   * Creates a formula representing an equivalence of the two arguments.
   * @param f1 a Formula
   * @param f2 a Formula
   * @return (f1 <-> f2)
   */
  @Override
  public BooleanFormula equivalence(BooleanFormula f1, BooleanFormula f2) {
    BooleanFormula F = null;
    try {
      TemplateBoolean b1 = (TemplateBoolean) f1;
      TemplateBoolean b2 = (TemplateBoolean) f2;
      TemplateBoolean nb1 = TemplateNegation.negate(b1);
      TemplateBoolean nb2 = TemplateNegation.negate(b2);
      TemplateBoolean both = TemplateConjunction.conjoin(b1, b2);
      TemplateBoolean neither = TemplateConjunction.conjoin(nb1, nb2);
      F = TemplateDisjunction.disjoin(both, neither);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate(getFormulaType());
    }
    return F;
  }

  /**
   * Creates a formula representing "IF cond THEN f1 ELSE f2"
   * @param cond a Formula
   * @param f1 a Formula
   * @param f2 a Formula
   * @return (IF atom THEN f1 ELSE f2)
   */

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Formula> T ifThenElse(BooleanFormula cond,
                T f1, T f2) {
    // We do not allow ifthenelse structures in templates.
    return (T) new NonTemplate(manager.getFormulaType(f1));
  }


  @Override
  public BooleanFormula xor(BooleanFormula pBits1, BooleanFormula pBits2) {
    return not(equivalence(pBits1, pBits1));
  }

  @Override
  public boolean isNot(BooleanFormula pBits) {
    return pBits instanceof TemplateNegation;
  }

  @Override
  public boolean isAnd(BooleanFormula pBits) {
    return pBits instanceof TemplateConjunction;
  }

  @Override
  public boolean isOr(BooleanFormula pBits) {
    return pBits instanceof TemplateDisjunction;
  }

  @Override
  public boolean isXor(BooleanFormula pBits) {
    return false;
  }

  @Override
  public FormulaType<BooleanFormula> getFormulaType() {
    return FormulaType.BooleanType;
  }

  @Override
  public BooleanFormula makeVariable(String pVar) {
    return new NonTemplate(getFormulaType());
    //return manager.makeVariable(getFormulaType(), pVar, null);
  }

  @Override
  public boolean isTrue(BooleanFormula pFormula) {
    return pFormula instanceof TemplateTrue;
  }

  @Override
  public boolean isFalse(BooleanFormula pFormula) {
    return pFormula instanceof TemplateFalse;
  }

  @Override
  public boolean isEquivalence(BooleanFormula pFormula) {
    return false;
  }

  @Override
  public boolean isImplication(BooleanFormula pFormula) {
    return false;
  }

  @Override
  public <T extends Formula> boolean isIfThenElse(T pF) {
    return false;
  }

}
