// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ltl;

import org.sosy_lab.cpachecker.util.ltl.formulas.BooleanConstant;
import org.sosy_lab.cpachecker.util.ltl.formulas.Conjunction;
import org.sosy_lab.cpachecker.util.ltl.formulas.Disjunction;
import org.sosy_lab.cpachecker.util.ltl.formulas.Finally;
import org.sosy_lab.cpachecker.util.ltl.formulas.Globally;
import org.sosy_lab.cpachecker.util.ltl.formulas.Literal;
import org.sosy_lab.cpachecker.util.ltl.formulas.Next;
import org.sosy_lab.cpachecker.util.ltl.formulas.Release;
import org.sosy_lab.cpachecker.util.ltl.formulas.StrongRelease;
import org.sosy_lab.cpachecker.util.ltl.formulas.Until;
import org.sosy_lab.cpachecker.util.ltl.formulas.WeakUntil;

public interface LtlFormulaVisitor {
  public String visit(BooleanConstant pBooleanConstant);

  public String visit(Conjunction pConjunction);

  public String visit(Disjunction pDisjunction);

  public String visit(Finally pFinally);

  public String visit(Globally pGlobally);

  public String visit(Literal pLiteral);

  public String visit(Next pNext);

  public String visit(Release pRelease);

  public String visit(StrongRelease pStrongRelease);

  public String visit(Until pUntil);

  public String visit(WeakUntil pWeakUntil);
}
