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
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

import com.google.common.collect.ImmutableSet;


@Options(prefix="cpa.predicate.refinement")
public class PredicateRefinementManager {

  private final LogManager logger;

  private final AbstractionManager amgr;
  private final FormulaManager fmgr;

  @Option(description="use only the atoms from the interpolants as predicates, "
    + "and not the whole interpolant")
  private boolean atomicPredicates = true;

  @Option(description="split each arithmetic equality into two inequalities when extracting predicates from interpolants")
  private boolean splitItpAtoms = false;

  public PredicateRefinementManager(
      AbstractionManager pAmgr,
      FormulaManager pFmgr,
      Configuration config,
      LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this, PredicateRefinementManager.class);

    logger = pLogger;
    amgr = pAmgr;
    fmgr = pFmgr;
  }

  /**
   * Get the predicates out of an interpolant.
   * @param interpolant The interpolant formula.
   * @return A set of predicates.
   */
  public Collection<AbstractionPredicate> convertInterpolant(Formula interpolant) {

    Collection<AbstractionPredicate> preds;

    if (interpolant.isFalse()) {
      preds = ImmutableSet.of(amgr.makeFalsePredicate());
    } else {
      preds = getAtomsAsPredicates(interpolant);
    }
    assert !preds.isEmpty();

    logger.log(Level.FINEST, "Got predicates", preds);

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
}
