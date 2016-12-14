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
package org.sosy_lab.cpachecker.core.algorithm.pdr;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Provides simplified and adapted versions of methods for predicate abstraction and refinement.
 * Each program location has its own set of abstraction predicates. All input and output formulas
 * are uninstantiated.
 */
public class PredicatePrecisionManager {

  private final PredicateAbstractionManager pamgr;
  private final Map<CFANode, Collection<AbstractionPredicate>> abstractionPredicates;

  /**
   * Creates a new PredicatePrecisionManager. The set of abstraction predicates is empty for each
   * location.
   *
   * @param pAbstractionDelegate the component that computes the abstractions
   */
  public PredicatePrecisionManager(PredicateAbstractionManager pAbstractionDelegate) {
    pamgr = pAbstractionDelegate;
    abstractionPredicates = Maps.newHashMap();
  }

  private void refinePredicates(CFANode pLocation, BooleanFormula pInterpolant) {
    // TODO also add atoms/conjuncts ?
    AbstractionPredicate newPredicate = pamgr.getPredicateFor(pInterpolant);
    if (!abstractionPredicates.containsKey(pLocation)) {
      abstractionPredicates.put(pLocation, Lists.newLinkedList());
    }
    abstractionPredicates.get(pLocation).add(newPredicate);
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
    return pamgr.computeAbstraction(
        pBaseFormula, abstractionPredicates.getOrDefault(pLocation, Collections.emptyList()));
  }
}
