/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import java.util.List;

import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

import com.google.common.primitives.Ints;

public class TreeInterpolationWithSolver<T> extends AbstractTreeInterpolation<T> {

  /**
   * This strategy uses a SMT solver that directly computes tree interpolants.
   * The layout of the tree is explained in {@link AbstractTreeInterpolation}.
   * Currently only SMTInterpol and Z3 support this.
   */
  public TreeInterpolationWithSolver(LogManager pLogger, ShutdownNotifier pShutdownNotifier,
                                     FormulaManagerView pFmgr, BooleanFormulaManager pBfmgr) {
    super(pLogger, pShutdownNotifier, pFmgr, pBfmgr);
  }

  @Override
  public List<BooleanFormula> getInterpolants(
          final InterpolationManager.Interpolator<T> interpolator,
          final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds)
          throws InterruptedException, SolverException {
    final Pair<List<Triple<BooleanFormula, AbstractState, T>>, List<Integer>> p = buildTreeStructure(formulasWithStatesAndGroupdIds);
    final List<BooleanFormula> itps = interpolator.itpProver.getTreeInterpolants(
            wrapAllInSets(projectToThird(p.getFirst())), Ints.toArray(p.getSecond()));
    return flattenTreeItps(formulasWithStatesAndGroupdIds, itps);
  }

}
