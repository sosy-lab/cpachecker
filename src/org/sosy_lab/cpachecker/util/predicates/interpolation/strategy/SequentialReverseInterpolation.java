/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import com.google.common.collect.Lists;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public class SequentialReverseInterpolation<T> extends ITPStrategy<T> {

  /**
   * This strategy returns a sequence of interpolants by computing
   * each interpolant for i={0..n-1} for the partitions B=[0 .. i] and A=[i+1 .. n] .
   *
   * This strategy computes the "reversed interpolants", which are valid when negated.
   * ITP(A,B) might be different from NOT(ITP(B,A)), but has the same properties.
   */
  public SequentialReverseInterpolation(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      FormulaManagerView pFmgr,
      BooleanFormulaManager pBfmgr) {
    super(pLogger, pShutdownNotifier, pFmgr, pBfmgr);
  }

  @Override
  public List<BooleanFormula> getInterpolants(
      final InterpolationManager.Interpolator<T> interpolator,
      final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStateAndGroupId)
      throws InterruptedException, SolverException {
    final List<T> formulas = Lists.transform(formulasWithStateAndGroupId, Triple::getThird);
    final List<BooleanFormula> interpolants =
        Lists.newArrayListWithExpectedSize(formulas.size() - 1);
    for (int start_of_A = 1; start_of_A < formulas.size(); start_of_A++) {
      // first iteration is left out because B would be empty
      final int end_of_A = formulas.size() - 1;
      interpolants.add(
          bfmgr.not(
              getInterpolantFromSublist(interpolator.itpProver, formulas, start_of_A, end_of_A)));
    }
    return interpolants;
  }
}
