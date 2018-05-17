/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.weakening;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Perform weakening based on the syntactic information.
 */
public class SyntacticWeakeningManager {
  private final FormulaManagerView fmgr;

  public SyntacticWeakeningManager(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
  }

  /**
   * Syntactic formula weakening: slices away all atoms which have variables which were changed (==
   * SSA index changed) by the transition relation. In that case, \phi is exactly the same as \phi',
   * and the formula should be unsatisfiable.
   *
   * @param selectionInfo selection variable -> corresponding (uninstantiated) lemma
   * @param pFromStateLemmas Uninstantiated lemmas describing the from- state.
   * @return Set of selectors which correspond to atoms which *should* be abstracted.
   */
  public Set<BooleanFormula> performWeakening(
      SSAMap pFromSSA,
      Map<BooleanFormula, BooleanFormula> selectionInfo,
      SSAMap pToSSA,
      Set<BooleanFormula> pFromStateLemmas) {
    Set<BooleanFormula> out = new HashSet<>();
    for (Entry<BooleanFormula, BooleanFormula> e : selectionInfo.entrySet()) {
      BooleanFormula selector = e.getKey();
      BooleanFormula lemma = e.getValue();

      BooleanFormula instantiatedFrom = fmgr.instantiate(lemma, pFromSSA);
      BooleanFormula instantiatedTo = fmgr.instantiate(lemma, pToSSA);

      if (!pFromStateLemmas.contains(lemma) ||
            !instantiatedFrom.equals(instantiatedTo)) {
        out.add(selector);
      }
    }
    return out;
  }
}
