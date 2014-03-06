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
package org.sosy_lab.cpachecker.core.algorithm.testgen.predicates.formula;

import java.util.LinkedList;
import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;


public class FormulaEditImpl implements FormulaEditFunction {

  @Override
  public BooleanFormula negateAtomOfFormula(BooleanFormula pF, int pPos, FormulaManagerView fmv) {
    //      PredicateCPA predCPA = CPAs.retrieveCPA(cpa, PredicateCPA.class);
    //      Formula f;
    //      fm = predCPA.getFormulaManager();

    // TODO get a useful FMV

    /*
     * negiere eine teilformel einer gegebenen Formel
     */
    //      zerlege formel in teilformeln

    // boolean splitArithEqualities, boolean conjunctionsOnly
    Pair<String, List<String>> splitFormulas = PredicatePersistenceUtils.splitFormula(fmv, pF);
    List<BooleanFormula> bFormulas = new LinkedList<>();
    for (String stringFormula : splitFormulas.getSecond()) {
      System.out.println(stringFormula);
      bFormulas.add(fmv.parse(stringFormula));
    }
//    Collection<BooleanFormula> formulaAtoms = fmv.extractAtoms(pF, false, false);
    //    wähle eine teilformel nach BlubStrategie aus
    BooleanFormula fTeil = (bFormulas).get(pPos);
    //    negiere die gewählte teilformel
    BooleanFormula negatedFTeil = fmv.makeNot(fTeil);
    System.out.println("fTeil: " + fTeil.toString() + " -> " + negatedFTeil);
    //    schmeiß die ausgewählte Formel aus der Menge der Formeln raus
    BooleanFormula sum = null;

    for (BooleanFormula t : bFormulas) {
      if (t == fTeil) {
        if (sum == null) {
          sum = negatedFTeil;
        } else {
          sum = fmv.makeAnd(sum, negatedFTeil);
        }

      } else {
        if (sum == null) {
          sum = t;
        } else {
          sum = fmv.makeAnd(sum, t);
        }
      }
    }

    return sum;

  }
}
