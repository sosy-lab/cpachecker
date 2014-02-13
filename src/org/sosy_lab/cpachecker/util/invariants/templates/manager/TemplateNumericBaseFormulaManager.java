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

import org.sosy_lab.cpachecker.util.invariants.InfixReln;
import org.sosy_lab.cpachecker.util.invariants.templates.*;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateNumeralValue;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;


public class TemplateNumericBaseFormulaManager {


  public Formula negate(Formula pF) {
    TemplateNumeralValue tf = null;
    try {
      tf = (TemplateNumeralValue) pF;
      //tf = (TemplateNumeralValue) tf.copy();
      tf.negate();
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
    }
    return tf;
  }

  public Formula add(Formula pF1, Formula pF2) {
    NumeralFormula F = null;
    try {
      TemplateSum s1 = (TemplateSum) pF1;
      TemplateSum s2 = (TemplateSum) pF2;
      F = new TemplateSum(s1, s2);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate(((TemplateFormula) pF1).getFormulaType());
    }
    return F;
  }

  public Formula subtract(Formula pF1, Formula pF2) {
    Formula F = null;
    try {
      TemplateSum s1 = (TemplateSum) pF1;
      TemplateSum s2 = (TemplateSum) pF2;
      F = TemplateSum.subtract(s1, s2);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate(((TemplateFormula) pF1).getFormulaType());
    }
    return F;
  }

  public Formula divide(Formula pF1, Formula pF2) {
    Formula F = null;
    try {
      TemplateSum s1 = (TemplateSum) pF1;
      TemplateSum s2 = (TemplateSum) pF2;
      F = TemplateSum.divide(s1, s2);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      F = new NonTemplate(((TemplateFormula) pF1).getFormulaType());
    }
    return F;
  }

  public Formula modulo(Formula pF1, Formula pF2) {
    return new NonTemplate(((TemplateFormula) pF1).getFormulaType());
  }

  public Formula multiply(Formula pF1, Formula pF2) {
    Formula F = null;
    try {
      TemplateSum s1 = (TemplateSum) pF1;
      TemplateSum s2 = (TemplateSum) pF2;
      F = TemplateSum.multiply(s1, s2);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate(((TemplateFormula) pF1).getFormulaType());
    }
    return F;
  }

  // ----------------- Numeric relations -----------------

  public BooleanFormula equal(Formula pF1, Formula pF2) {
    BooleanFormula F = null;
    try {
      TemplateSum s1 = (TemplateSum) pF1;
      TemplateSum s2 = (TemplateSum) pF2;
      F = new TemplateConstraint(s1, InfixReln.EQUAL, s2);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate(((TemplateFormula) pF1).getFormulaType());
    }
    return F;
  }

  public BooleanFormula greaterThan(Formula pF1, Formula pF2) {
    BooleanFormula F = null;
    try {
      TemplateSum s1 = (TemplateSum) pF1;
      TemplateSum s2 = (TemplateSum) pF2;
      F = new TemplateConstraint(s2, InfixReln.LT, s1);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F =new NonTemplate(((TemplateFormula) pF1).getFormulaType());
    }
    return F;
  }

  public BooleanFormula greaterOrEquals(Formula pF1, Formula pF2) {
    BooleanFormula F = null;
    try {
      TemplateSum s1 = (TemplateSum) pF1;
      TemplateSum s2 = (TemplateSum) pF2;
      F = new TemplateConstraint(s2, InfixReln.LEQ, s1);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate(((TemplateFormula) pF1).getFormulaType());
    }
    return F;
  }

  public BooleanFormula lessThan(Formula pF1, Formula pF2) {
    BooleanFormula F = null;
    try {
      TemplateSum s1 = (TemplateSum) pF1;
      TemplateSum s2 = (TemplateSum) pF2;
      F = new TemplateConstraint(s1, InfixReln.LT, s2);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate(((TemplateFormula) pF1).getFormulaType());
    }
    return F;
  }

  public BooleanFormula lessOrEquals(Formula pF1, Formula pF2) {
    BooleanFormula F = null;
    try {
      TemplateSum s1 = (TemplateSum) pF1;
      TemplateSum s2 = (TemplateSum) pF2;
      F = new TemplateConstraint(s1, InfixReln.LEQ, s2);
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate(((TemplateFormula) pF1).getFormulaType());
    }
    return F;
  }

  public boolean isNegate(Formula pNumber) {
    // TODO Auto-generated method stub
    return false;
  }


  public boolean isAdd(Formula pNumber) {
    // TODO Auto-generated method stub
    return false;
  }


  public boolean isSubtract(Formula pNumber) {
    // TODO Auto-generated method stub
    return false;
  }


  public boolean isDivide(Formula pNumber) {
    // TODO Auto-generated method stub
    return false;
  }


  public boolean isModulo(Formula pNumber) {
    // TODO Auto-generated method stub
    return false;
  }


  public boolean isMultiply(Formula pNumber) {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isEqual(BooleanFormula pNumber) {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isGreaterThan(BooleanFormula pNumber) {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isGreaterOrEquals(BooleanFormula pNumber) {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isLessThan(BooleanFormula pNumber) {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isLessOrEquals(BooleanFormula pNumber) {
    // TODO Auto-generated method stub
    return false;
  }
}
