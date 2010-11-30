package org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces;

import org.sosy_lab.cpachecker.util.symbpredabstraction.AbstractionFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.AbstractionPredicate;

public interface AbstractionManager {

  /**
   * creates a Predicate from the Boolean symbolic variable (var) and
   * the atom that defines it
   */
  AbstractionPredicate makePredicate(SymbolicFormula atom);

  AbstractionPredicate makeFalsePredicate();

  /**
   * Get predicate corresponding to a variable.
   * @param var A symbolic formula representing the variable. The same formula has to been passed to makePredicate earlier.
   * @return a Predicate
   */
  AbstractionPredicate getPredicate(SymbolicFormula var);

  /**
   * Given an abstract formula (which is a BDD over the predicates), build
   * its concrete representation (which is a MathSAT formula corresponding
   * to the BDD, in which each predicate is replaced with its definition)
   */
  SymbolicFormula toConcrete(Region af);

  AbstractionFormula makeTrueAbstractionFormula(SymbolicFormula previousBlockFormula);

  RegionManager getRegionManager();

}