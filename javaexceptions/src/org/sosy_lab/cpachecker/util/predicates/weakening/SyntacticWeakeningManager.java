// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.weakening;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/** Perform weakening based on the syntactic information. */
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

      if (!pFromStateLemmas.contains(lemma) || !instantiatedFrom.equals(instantiatedTo)) {
        out.add(selector);
      }
    }
    return out;
  }
}
