package org.sosy_lab.cpachecker.cpa.formulaslicing;

import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
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
   * @param selectionInfo selection variable -> corresponding lemma
   *                      (instantiated with unprimed SSA).
   * @param transition Transition with respect to which the weakening must be inductive.
   *
   * @return Set of selectors which correspond to atoms which *should*
   *         be abstracted.
   */
  public Set<BooleanFormula> performWeakening(

      // todo: allow to only operate on those lemmas which occur both in the
      // {@code fromState} and in the {@code toState}.
      Map<BooleanFormula, BooleanFormula> selectionInfo,
      PathFormula transition
  ) {
    Set<BooleanFormula> out = new HashSet<>();
    for (Entry<BooleanFormula, BooleanFormula> e : selectionInfo.entrySet()) {
      BooleanFormula selector = e.getKey();
      BooleanFormula atom = e.getValue();

      // Variables which have the SSA index different to the one after the
      // transition.
      Set<String> deadVars = fmgr.getDeadFunctionNames(atom, transition.getSsa());

      if (!deadVars.isEmpty()) {

        // Abstract away the selectors where the associated atoms have changed.
        out.add(selector);
      }
    }
    return out;
  }
}
