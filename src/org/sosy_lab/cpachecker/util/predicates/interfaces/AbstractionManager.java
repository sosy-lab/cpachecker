package org.sosy_lab.cpachecker.util.predicates.interfaces;

import java.util.Collection;

import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

public interface AbstractionManager {

  /**
   * creates a Predicate from the Boolean symbolic variable (var) and
   * the atom that defines it
   */
  AbstractionPredicate makePredicate(Formula atom);

  AbstractionPredicate makeFalsePredicate();

  /**
   * Get predicate corresponding to a variable.
   * @param var A symbolic formula representing the variable. The same formula has to been passed to makePredicate earlier.
   * @return a Predicate
   */
  AbstractionPredicate getPredicate(Formula var);

  /**
   * Given an abstract formula (which is a BDD over the predicates), build
   * its concrete representation (which is a MathSAT formula corresponding
   * to the BDD, in which each predicate is replaced with its definition)
   */
  Formula toConcrete(Region af);

  AbstractionFormula makeTrueAbstractionFormula(Formula previousBlockFormula);

  RegionManager getRegionManager();

  Collection<AbstractionPredicate> extractPredicates(Region pAf);

}