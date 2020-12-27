// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.ufCheckingProver;

import java.util.Collection;
import java.util.List;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

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
  public BooleanFormula getInterpolant(Collection<T> formulasOfA)
      throws SolverException, InterruptedException {
    return delegate.getInterpolant(formulasOfA);
  }

  @Override
  public List<BooleanFormula> getSeqInterpolants(List<? extends Collection<T>> partitionedFormulas)
      throws SolverException, InterruptedException {
    return delegate.getSeqInterpolants(partitionedFormulas);
  }

  @Override
  public List<BooleanFormula> getTreeInterpolants(
      List<? extends Collection<T>> partitionedFormulas, int[] startOfSubTree)
      throws SolverException, InterruptedException {
    return delegate.getTreeInterpolants(partitionedFormulas, startOfSubTree);
  }
}
