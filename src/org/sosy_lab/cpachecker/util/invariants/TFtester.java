/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

public class TFtester {

  public static void main(String[] args) {

    TemplateFormulaManager tfm = new TemplateFormulaManager();
    Formula a1 = tfm.makeVariable("a", 1);
    Formula a2 = tfm.makeVariable("a", 2);
    Formula b1 = tfm.makeVariable("b", 1);
    Formula b2 = tfm.makeVariable("b", 2);
    Formula c1 = tfm.makeVariable("c", 1);
    Formula c2 = tfm.makeVariable("c", 2);
    Formula d1 = tfm.makeVariable("d", 1);
    Formula d2 = tfm.makeVariable("d", 2);

    Formula A = tfm.makeGeq(a1, a2);
    Formula B = tfm.makeGt(b1, b2);
    Formula C = tfm.makeLeq(c1, c2);
    Formula D = tfm.makeLt(d1, d2);

    Formula P = tfm.makeOr(A, B);
    Formula Q = tfm.makeOr(C, D);
    Formula R = tfm.makeNot(Q);

    TemplateBoolean F = (TemplateBoolean) tfm.makeAnd(P, R);

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
    TemplateBoolean N = (TemplateBoolean) tfm.makeNot(M);
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

  private static void printTF(Formula f) {
    System.out.println(f.toString());
  }

}












