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
package org.sosy_lab.cpachecker.util.predicates.ufCheckingProver;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class UFCheckingInterpolatingProverEnvironment<T> extends UFCheckingBasicProverEnvironment<T>
    implements InterpolatingProverEnvironment<T> {

  private final InterpolatingProverEnvironment<T> delegate;

  public UFCheckingInterpolatingProverEnvironment(
      LogManager pLogger,
      InterpolatingProverEnvironment<T> ipe,
      FormulaManagerView pFmgr,
      UFCheckingProverOptions options) {
    super(pLogger, ipe, pFmgr, options);
    this.delegate = ipe;
  }

  @Override
  public boolean isUnsatWithAssumptions(Collection<BooleanFormula> pAssumptions)
      throws SolverException, InterruptedException {
    // TODO forward to isUnsat() ??
    return delegate.isUnsatWithAssumptions(pAssumptions);
  }

  @Override
  public BooleanFormula getInterpolant(List<T> formulasOfA)
      throws SolverException, InterruptedException {
    return delegate.getInterpolant(formulasOfA);
  }

  @Override
  public List<BooleanFormula> getSeqInterpolants(List<Set<T>> partitionedFormulas)
      throws SolverException, InterruptedException {
    return delegate.getSeqInterpolants(partitionedFormulas);
  }

  @Override
  public List<BooleanFormula> getTreeInterpolants(List<Set<T>> partitionedFormulas, int[] startOfSubTree)
      throws SolverException, InterruptedException {
    return delegate.getTreeInterpolants(partitionedFormulas, startOfSubTree);
  }
}
