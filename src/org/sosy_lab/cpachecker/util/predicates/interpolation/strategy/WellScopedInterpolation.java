/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.interpolation.strategy;

import com.google.common.primitives.ImmutableIntArray;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public class WellScopedInterpolation<T> extends AbstractTreeInterpolation<T> {

  /**
   * This strategy returns a sequence of interpolants by computing each interpolant for i={0..n-1}
   * for the partitions A=[lastFunctionEntryIndex .. i] and B=[0 .. lastFunctionEntryIndex-1 , i+1
   * .. n]. The resulting interpolants are based on a tree-like scheme. The approach is based on
   * "Abstractions from Proofs (Henzinger, Jhala, Majumdar, McMillan)".
   *
   * <p>INFO: This interpolation strategy might not be sufficient for predicate analysis with an
   * inductive sequence/tree of abstractions. In some cases we cannot exclude counterexamples from
   * re-exploration.
   *
   * <p>Example (taken from recursive program "recHanoi01.c"):
   *
   * <p>The tree has the structure [0,1,0,0,0,5,0,0,0] and is given in DOT notation with formulae
   * and interpolants.
   *
   * <p>The wellscoped interpolants at E->F and G->F do not contradict each other (transivitely).
   * This example shows that the idea of the paper "wellscoped interpolants" should not be used.
   *
   * <pre>
   * digraph tree {
   *   A [label="(and (= c@3 0)\n(= nondet@2 main::n@3)\n(not (< main::n@3 1))\n(not (< 31 main::n@3))\n(= c@4 0))"];
   *   B [label="(and (= main::n@3 apply::n_p_@2)\n(= c@4 c_p_apply@2))"];
   *   C [label="(and (= apply::n_p_@2 apply::n@2)\n(= c_p_apply@2 c@5)\n(= apply::n@2 0)\n(= c@5 c_ret_apply@2)"];
   *   D [label="(= c@6 c_ret_apply@2)"];
   *   E [label="true"];
   *   F [label="(and (= main::n@3 h::n_p_@2)\n(= c@6 c_p_h@2))"];
   *   G [label="(and (= h::n_p_@2 h::n@2)\n(= c_p_h@2 c@7)\n(= h::n@2 1)\n(= h::_ret_@2 1)\n(= c@7 c_ret_h@2))"];
   *   H [label="(and (= c_ret_h@2 c@8)\n(= h::_ret_@2 main::result@4))"];
   *   I [label="(not (= c@8 main::result@4))"];
   *   A->B [label="true"];
   *   B->D [label="(not (= main::n@3 1))"];
   *   D->E [label="(not (= main::n@3 1))"];
   *   E->F [label="(not (= main::n@3 1))"];
   *   F->H [label="false"];
   *   H->I [label="false"];
   *   C->B [label="(not (= apply::n_p_@2 1))"];
   *   G->F [label="(not (= h::n_p_@2 0))"];
   * }
   * </pre>
   */
  public WellScopedInterpolation(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      FormulaManagerView pFmgr,
      BooleanFormulaManager pBfmgr) {
    super(pLogger, pShutdownNotifier, pFmgr, pBfmgr);
  }

  @Override
  public List<BooleanFormula> getInterpolants(
          final InterpolationManager.Interpolator<T> interpolator,
          final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds)
          throws InterruptedException, SolverException {
    final Pair<List<Triple<BooleanFormula, AbstractState, T>>, ImmutableIntArray> p =
        buildTreeStructure(formulasWithStatesAndGroupdIds);
    final List<BooleanFormula> itps = new ArrayList<>();
    for (int end_of_A = 0; end_of_A < p.getFirst().size() - 1; end_of_A++) {
      // last iteration is left out because B would be empty
      final int start_of_A = p.getSecond().get(end_of_A);
      itps.add(getInterpolantFromSublist(interpolator.itpProver, projectToThird(p.getFirst()), start_of_A, end_of_A));
    }
    return flattenTreeItps(formulasWithStatesAndGroupdIds, itps);
  }

}
