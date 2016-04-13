package org.sosy_lab.cpachecker.cpa.formulaslicing;

import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Perform weakening based on the syntactic information.
 */
public class SyntacticWeakeningManager {
  private final FormulaManagerView fmgr;

  public SyntacticWeakeningManager(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
  }

  /**
   * Syntactic formula weakening: slices away all atoms which have variables
   * which were changed (== SSA index changed) by the transition relation.
   * In that case, \phi is exactly the same as \phi',
   * and the formula should be unsatisfiable.
   *
   *
   * @param selectionInfo selection variable -> corresponding lemma
   *                      (instantiated with unprimed SSA).
   * @param transition Transition with respect to which the weakening must be inductive.
   *
   * @param pFromStateLemmas Uninstantiated lemmas describing the from- state.
   * @return Set of selectors which correspond to atoms which *should*
   *         be abstracted.
   */
  public Set<BooleanFormula> performWeakening(
      SSAMap pFromSSA,
      Map<BooleanFormula, BooleanFormula> selectionInfo,
      PathFormula transition,
      Set<BooleanFormula> pFromStateLemmas) {
    Set<BooleanFormula> out = new HashSet<>();
    for (Entry<BooleanFormula, BooleanFormula> e : selectionInfo.entrySet()) {
      BooleanFormula selector = e.getKey();
      BooleanFormula lemma = e.getValue();

      BooleanFormula uninstantiated = fmgr.uninstantiate(lemma);
      BooleanFormula instantiatedFrom = fmgr.instantiate(uninstantiated, pFromSSA);
      BooleanFormula instantiatedTo =
          fmgr.instantiate(uninstantiated, transition.getSsa());

      if (!pFromStateLemmas.contains(uninstantiated) ||
            !instantiatedFrom.equals(instantiatedTo)) {
        out.add(selector);
      }
    }
    return out;
  }
}
