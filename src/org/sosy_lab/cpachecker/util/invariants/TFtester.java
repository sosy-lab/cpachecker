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
package org.sosy_lab.cpachecker.util.invariants;

import org.sosy_lab.cpachecker.util.invariants.templates.TemplateBoolean;
import org.sosy_lab.cpachecker.util.invariants.templates.manager.TemplateFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;

public class TFtester {

  public static void main(String[] args) {

    TemplateFormulaManager tfm = new TemplateFormulaManager();

    FormulaManagerView view = new FormulaManagerView(tfm);
    NumeralFormulaManagerView<NumeralFormula, RationalFormula> nfmgr = view.getRationalFormulaManager();
    BooleanFormulaManagerView bfmgr = view.getBooleanFormulaManager();
    FormulaType<? extends NumeralFormula> type = FormulaType.RationalType;
    NumeralFormula a1 = view.makeVariable(type, "a", 1);
    NumeralFormula a2 = view.makeVariable(type, "a", 2);
    NumeralFormula b1 = view.makeVariable(type, "b", 1);
    NumeralFormula b2 = view.makeVariable(type, "b", 2);
    NumeralFormula c1 = view.makeVariable(type, "c", 1);
    NumeralFormula c2 = view.makeVariable(type, "c", 2);
    NumeralFormula d1 = view.makeVariable(type, "d", 1);
    NumeralFormula d2 = view.makeVariable(type, "d", 2);

    BooleanFormula A = nfmgr.greaterOrEquals(a1, a2);
    BooleanFormula B = nfmgr.greaterThan(b1, b2);
    BooleanFormula C = nfmgr.lessOrEquals(c1, c2);
    BooleanFormula D = nfmgr.lessThan(d1, d2);

    BooleanFormula P = bfmgr.or(A, B);
    BooleanFormula Q = bfmgr.or(C, D);
    BooleanFormula R = bfmgr.not(Q);

    TemplateBoolean F = (TemplateBoolean) bfmgr.and(P, R);

    printS("R:");
    printTF(R);

    printS("F:");
    printTF(F);
    F.flatten();
    printS("F:");
    printTF(F);

    TemplateBoolean G = F.makeCNF();
    TemplateBoolean H = F.makeDNF();
    printS("CNF:");
    printTF(G);
    printS("DNF:");
    printTF(H);

    TemplateBoolean Gs = F.makeSCNF();
    TemplateBoolean Hs = F.makeSDNF();
    printS("Strong CNF:");
    printTF(Gs);
    printS("Strong DNF:");
    printTF(Hs);

    TemplateBoolean M = F;
    TemplateBoolean N = (TemplateBoolean) bfmgr.not(M);
    printS("N:");
    printTF(N);
    TemplateBoolean K = M.logicNegate();
    printS("N's argument negated:");
    printTF(K);
    TemplateBoolean L = N.absorbNegations();
    printS("N negation absorbed:");
    printTF(L);

  }

  private static void printS(String s) {
    System.out.println(s);
  }

  private static void printTF(BooleanFormula f) {
    System.out.println(f.toString());
  }

}












