/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.core.algorithm.pdr.old;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Provides simplified and adapted versions of methods for predicate abstraction and refinement.
 * Each program location has its own set of abstraction predicates. All input and output formulas
 * are uninstantiated.
 */
public class PredicatePrecisionManager {

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;
  private final PredicateAbstractionManager pamgr;
  private final Map<CFANode, Collection<AbstractionPredicate>> abstractionPredicates;

  /**
   * Creates a new PredicatePrecisionManager. The set of abstraction predicates is empty for each
   * location.
   *
   * @param pAbstractionDelegate the component that computes the abstractions
   */
  public PredicatePrecisionManager(
      FormulaManagerView pFmgr, PredicateAbstractionManager pAbstractionDelegate) {
    fmgr = pFmgr;
    bfmgr = pFmgr.getBooleanFormulaManager();
    pamgr = pAbstractionDelegate;
    abstractionPredicates = Maps.newHashMap();
  }

  private void refinePredicates(CFANode pLocation, BooleanFormula pInterpolant) {
    // TODO also add atoms/conjuncts ?
    if (!abstractionPredicates.containsKey(pLocation)) {
      abstractionPredicates.put(pLocation, Lists.newLinkedList());
    }
    assert fmgr.isPurelyConjunctive(pInterpolant);

    List<BooleanFormula> symbolicVariables =
        abstractionPredicates
            .get(pLocation)
            .stream()
            .map(AbstractionPredicate::getSymbolicVariable)
            .collect(Collectors.toList());
    for (AbstractionPredicate ap : pamgr.getPredicatesForAtomsOf(pInterpolant)) { // TODO atoms ok?
      if (!symbolicVariables.contains(bfmgr.not(ap.getSymbolicVariable()))) {
        abstractionPredicates.get(pLocation).add(ap);
      }
    }
  }

  /**
   * Refines the predicates known at pLocation with predicates based on pInterpolant and computes an
   * abstraction of the given formula with the new predicate set afterwards.
   *
   * @param pLocation the program location where the formula is to be abstracted
   * @param pBaseFormula the uninstantiated formula to be abstracted
   * @param pInterpolant the formula used as basis for generating new abstraction predicates
   * @return an abstracted version of pBaseFormula
   */
  public BooleanFormula refineAndComputeAbstraction(
      CFANode pLocation, BooleanFormula pBaseFormula, BooleanFormula pInterpolant)
      throws InterruptedException, SolverException {
    refinePredicates(pLocation, pInterpolant);
    return computeAbstraction(pLocation, pBaseFormula);
  }

  /**
   * Computes an abstraction of a formula based on the predicates known at the given location.
   *
   * @param pLocation the program location where pBaseState is to be abstracted
   * @param pBaseFormula the state to be abstracted
   * @return an abstracted version of pBaseFormula
   */
  public BooleanFormula computeAbstraction(CFANode pLocation, BooleanFormula pBaseFormula)
      throws InterruptedException, SolverException {
    if (!abstractionPredicates.containsKey(pLocation)) {
      abstractionPredicates.put(pLocation, Lists.newLinkedList());
      addDefaultPreds(pLocation, pBaseFormula);
    }
    return pamgr.computeAbstraction(pBaseFormula, abstractionPredicates.get(pLocation));
  }

  // Add predicates of the following type : for all variables v1, v2 in formula -> (v1 < v2)
  private void addDefaultPreds(CFANode pLocation, BooleanFormula pBaseFormula) {
    List<String> varNames = Lists.newArrayList(fmgr.extractVariableNames(pBaseFormula));
    if (varNames.size() == 1) {
      AbstractionPredicate newPredicate = pamgr.getPredicateFor(pBaseFormula);
      abstractionPredicates.get(pLocation).add(newPredicate);
      return;
    }

    BitvectorFormulaManagerView bvfmgr = fmgr.getBitvectorFormulaManager();
    for (int i = 0; i < varNames.size() - 1; ++i) {
      for (int j = i + 1; j < varNames.size(); ++j) {
        BitvectorFormula var1 = bvfmgr.makeVariable(32, varNames.get(i)); // TODO ??
        BitvectorFormula var2 = bvfmgr.makeVariable(32, varNames.get(j));

        BooleanFormula var1LessThanVar2 = bvfmgr.lessThan(var1, var2, true); // TODO signed ?
        AbstractionPredicate newPredicate = pamgr.getPredicateFor(var1LessThanVar2);
        abstractionPredicates.get(pLocation).add(newPredicate);
      }
    }
  }
}
