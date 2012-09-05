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
package org.sosy_lab.cpachecker.cpa.predicate;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;


@Options(prefix="cpa.predicate.refinement")
public class PredicateRefinementManager extends InterpolationManager<Collection<AbstractionPredicate>> {

  private final AbstractionManager amgr;

  @Option(description="use only the atoms from the interpolants as predicates, "
    + "and not the whole interpolant")
  private boolean atomicPredicates = true;

  @Option(description="split each arithmetic equality into two inequalities when extracting predicates from interpolants")
  private boolean splitItpAtoms = false;

  public PredicateRefinementManager(
      ExtendedFormulaManager pFmgr,
      PathFormulaManager pPmgr,
      Solver pSolver,
      AbstractionManager pAmgr,
      FormulaManagerFactory pFmgrFactory,
      Configuration config,
      LogManager pLogger) throws InvalidConfigurationException {
    super(pFmgr, pPmgr, pSolver, pFmgrFactory, config, pLogger);
    config.inject(this, PredicateRefinementManager.class);

    amgr = pAmgr;
  }

  /**
   * Get the predicates out of an interpolant.
   * @param interpolant The interpolant formula.
   * @param index The index in the list of formulas (just for debugging)
   * @return A set of predicates.
   */
  @Override
  protected Collection<AbstractionPredicate> convertInterpolant(Formula interpolant, int index) {

    Collection<AbstractionPredicate> preds;

    if (interpolant.isFalse()) {
      preds = ImmutableSet.of(amgr.makeFalsePredicate());
    } else {
      preds = getAtomsAsPredicates(interpolant);
    }
    assert !preds.isEmpty();

    logger.log(Level.FINEST, "For step", index, "got:", "predicates", preds);

    File dumpFile = formatFormulaOutputFile("atoms", index);
    Collection<Formula> atoms = Collections2.transform(preds,
        new Function<AbstractionPredicate, Formula>(){
              @Override
              public Formula apply(AbstractionPredicate pArg0) {
                return pArg0.getSymbolicAtom();
              }
        });
    fmgr.printFormulasToFile(atoms, dumpFile);

    return preds;
  }

  /**
   * Create predicates for all atoms in a formula.
   */
  private List<AbstractionPredicate> getAtomsAsPredicates(Formula f) {
    Collection<Formula> atoms;
    if (atomicPredicates) {
      atoms = fmgr.extractAtoms(f, splitItpAtoms, false);
    } else {
      atoms = Collections.singleton(fmgr.uninstantiate(f));
    }

    List<AbstractionPredicate> preds = new ArrayList<AbstractionPredicate>(atoms.size());

    for (Formula atom : atoms) {
      preds.add(amgr.makePredicate(atom));
    }
    return preds;
  }

  @Override
  protected Collection<AbstractionPredicate> getTrueInterpolant() {
    return Collections.emptySet();
  }
}
