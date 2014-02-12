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

import org.sosy_lab.cpachecker.util.invariants.templates.TemplateConstraint;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFormulaList;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateNumber;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateSumList;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateTerm;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateUIF;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateVariable;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UnsafeFormulaManager;


public class TemplateUnsafeFormulaManager implements UnsafeFormulaManager {

  private TemplateFormulaManager manager;

  public TemplateUnsafeFormulaManager(TemplateFormulaManager manager) {
    this.manager = manager;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Formula> T typeFormula(FormulaType<T> pType, Formula pF) {
    return (T)pF;
  }

  @Override
  public boolean isAtom(Formula pF) {
    BooleanFormulaManager bfmgr = manager.getBooleanFormulaManager();
    return bfmgr.isBoolean(pF) &&
        !bfmgr.isAnd((BooleanFormula) pF) &&
        !bfmgr.isOr((BooleanFormula) pF) &&
        !bfmgr.isNot((BooleanFormula) pF);
  }

  @Override
  public int getArity(Formula pF) {
    if (pF instanceof TemplateConstraint) {
      return 2;
    } else if (pF instanceof TemplateTerm) {
      TemplateTerm tt = (TemplateTerm)pF;

      int args = 0;
      if (tt.hasUIF()) {
        args++;
      }
      if (tt.hasCoefficient()) {
        args++;
      }
      if (tt.hasParameter()) {
        args++;
      }
      if (tt.hasVariable()) {
        args++;
      }
      if (args == 0 || args > 1) {
        return args;
      }
      if (tt.hasUIF()) {
        return tt.getUIF().getArity();
      }

      return args;
    }

    return 0;
  }


  @Override
  public Formula getArg(Formula pF, int pN) {
    if (pF instanceof TemplateConstraint) {

      TemplateConstraint tc = (TemplateConstraint)pF;
      switch (pN) {
      case 0:
        return tc.getLeft();
      case 1:
        return tc.getRight();
      default:
        throw new IndexOutOfBoundsException("Invalid index");
      }

    } else if (pF instanceof TemplateTerm) {
      TemplateTerm tt = (TemplateTerm)pF;

      int args = 0;
      if (tt.hasUIF()) {
        args++;
      }
      if (tt.hasCoefficient()) {
        args++;
      }
      if (tt.hasParameter()) {
        args++;
      }
      if (tt.hasVariable()) {
        args++;
      }
      Formula[] formulas = new Formula[args];
      int i = 0;

      if (tt.hasUIF()) {
        formulas[i] = tt.getUIF();
        i++;
      }
      if (tt.hasCoefficient()) {
        formulas[i] = tt.getUIF();
        i++;
      }
      if (tt.hasParameter()) {
        formulas[i] = tt.getUIF();
        i++;
      }
      if (tt.hasVariable()) {
        formulas[i] = tt.getUIF();
        i++;
      }

      if (args == 0 || args > 1) {
        if (args > 0) {
          return formulas[pN];
        } else {
          throw new IndexOutOfBoundsException("Invalid index");
        }
      }

      if (tt.hasUIF()) {
        TemplateSumList uifargs = tt.getUIF().getArgs();
        return uifargs.getFormulas()[pN];
      }

      return formulas[pN];
    }

    throw new IndexOutOfBoundsException("Invalid index");
  }

  @Override
  public boolean isVariable(Formula pF) {
    if (pF instanceof TemplateTerm) {
      TemplateTerm tt = (TemplateTerm)pF;
      return !tt.hasUIF()  && !tt.hasCoefficient() && !tt.hasParameter() && tt.hasVariable();
    }

    return pF instanceof TemplateVariable;
  }

  @Override
  public boolean isNumber(Formula pF) {
    if (pF instanceof TemplateTerm) {
      TemplateTerm tt = (TemplateTerm)pF;
      return tt.isANumber();
    }

    return pF instanceof TemplateNumber;
  }

  @Override
  public boolean isUF(Formula pF) {
    if (pF instanceof TemplateTerm) {
      TemplateTerm tt = (TemplateTerm)pF;
      return tt.hasUIF()  && !tt.hasCoefficient() && !tt.hasParameter() && !tt.hasVariable();
    }
    return false;
  }

  @Override
  public String getName(Formula pF) {
    if (pF instanceof TemplateTerm) {
      TemplateTerm tt = (TemplateTerm)pF;
      if (!tt.hasUIF()  && !tt.hasCoefficient() && !tt.hasParameter() && tt.hasVariable()) {
        return tt.getVariable().getName();
      }
      if (tt.hasUIF() && !tt.hasCoefficient() && !tt.hasParameter() && !tt.hasVariable()) {
        return tt.getUIF().getName();
      }
    }

    if (pF instanceof TemplateVariable) {
      TemplateVariable tv = (TemplateVariable)pF;
      return tv.getName();
    }
    if (pF instanceof TemplateUIF) {
      TemplateUIF tuif = (TemplateUIF)pF;
      return tuif.getName();
    }
    throw new IllegalArgumentException("Can't get the name from the given formula!");
  }

  @Override
  public Formula replaceArgsAndName(Formula pF, String pNewName, Formula[] pArgs) {
    return null;
  }

  @Override
  public Formula replaceArgs(Formula pF, Formula[] pArgs) {
    if (pF instanceof TemplateTerm) {
      TemplateTerm tt = (TemplateTerm)pF;
      if (tt.hasUIF() && !tt.hasCoefficient() && !tt.hasParameter() && !tt.hasVariable()) {
        TemplateUIF oldUif = tt.getUIF();
        TemplateUIF newUif = new TemplateUIF(oldUif.getName(), oldUif.getFormulaType(), new TemplateSumList(new TemplateFormulaList(pArgs)));
        return new TemplateTerm(newUif);
      }
    }

    if (pF instanceof TemplateUIF) {
      TemplateUIF oldUif = (TemplateUIF)pF;
      TemplateUIF newUif = new TemplateUIF(oldUif.getName(), oldUif.getFormulaType(), new TemplateSumList(new TemplateFormulaList(pArgs)));
      return new TemplateTerm(newUif);
    }

    throw new IllegalArgumentException("Can't replace the args of the given formula!");
  }

  @Override
  public Formula replaceName(Formula pF, String pNewName) {
    if (pF instanceof TemplateTerm) {
      TemplateTerm tt = (TemplateTerm)pF;
      if (!tt.hasUIF()  && !tt.hasCoefficient() && !tt.hasParameter() && tt.hasVariable()) {
        TemplateTerm newtt = new TemplateTerm(tt.getFormulaType());
        newtt.setVariable(new TemplateVariable(tt.getVariable().getFormulaType(), pNewName));
        return newtt;
      }
      if (tt.hasUIF() && !tt.hasCoefficient() && !tt.hasParameter() && !tt.hasVariable()) {
        TemplateUIF oldUif = tt.getUIF();
        TemplateUIF newUif = new TemplateUIF(pNewName, oldUif.getFormulaType(), oldUif.getArgs());
        return new TemplateTerm(newUif);
      }
    }

    if (pF instanceof TemplateVariable) {
      TemplateVariable tv = (TemplateVariable)pF;
      return new TemplateVariable(tv.getFormulaType(), pNewName);
    }
    if (pF instanceof TemplateUIF) {
      TemplateUIF oldUif = (TemplateUIF)pF;
      TemplateUIF newUif = new TemplateUIF(pNewName, oldUif.getFormulaType(), oldUif.getArgs());
      return new TemplateTerm(newUif);
    }
    throw new IllegalArgumentException("Can't set the name from the given formula!");
  }
}
