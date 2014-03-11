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
package org.sosy_lab.cpachecker.core.algorithm.testgen.old.predicates.formula;

import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UnsafeFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.collect.Lists;


public class FormulaEditImpl implements FormulaEditFunction {

  @Override
  public BooleanFormula negateAtomOfFormula(BooleanFormula pF, int pPos, FormulaManagerView fmv) {
    //      PredicateCPA predCPA = CPAs.retrieveCPA(cpa, PredicateCPA.class);
    //      Formula f;
    //      fm = predCPA.getFormulaManager();


    /*
     * negiere eine teilformel einer gegebenen Formel
     */
    //      zerlege formel in teilformeln

    // boolean splitArithEqualities, boolean conjunctionsOnly

    List<BooleanFormula> bFormulas = new FormulaSplitter().splitCNF(pF, fmv);

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

  public class FormulaSplitter {

    List<BooleanFormula> formulas;

    public FormulaSplitter() {
      formulas = Lists.newLinkedList();
    }

    public List<BooleanFormula> splitCNF(BooleanFormula f, FormulaManagerView fmv) {
      internalSplitCNF(f, fmv);
      return formulas;
    }

    private boolean internalSplitCNF(BooleanFormula f, FormulaManagerView fmv) {
      UnsafeFormulaManager unsafeManager = fmv.getUnsafeFormulaManager();
      BooleanFormulaManager rawBooleanManager = fmv.getBooleanFormulaManager();
      BooleanFormula t = f;

      if (rawBooleanManager.isAnd(f)) {
        for (int i = 0; i < unsafeManager.getArity(f); ++i) {
          if (!internalSplitCNF( //just a comment for blocking the formatter
              unsafeManager.typeFormula(FormulaType.BooleanType, unsafeManager.getArg(f, i)), fmv))//
          { return false; }
        }
        return true;

      } else {
        formulas.add(t);
        return true;
      }
      //      if (/*unsafeManager.isAtom(t) || unsafeManager.isUF(t) || */(!rawBooleanManager.isAnd(t))) {
      //        formulas.add(t);
      //        return true;
      //
      //      } else if (rawBooleanManager.isAnd(f)) {
      //        for (int i = 0; i < unsafeManager.getArity(f); ++i) {
      //          if (!internalSplitCNF(unsafeManager.typeFormula(FormulaType.BooleanType, unsafeManager.getArg(f, i)), fmv)) { return false; }
      //        }
      //        return true;
      //
      //      } else {
      //        return false;
      //      }
    }
  }
}
