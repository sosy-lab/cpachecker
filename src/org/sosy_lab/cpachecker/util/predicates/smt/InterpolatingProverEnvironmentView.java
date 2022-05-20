// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import java.util.Collection;
import java.util.List;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

/** Model wrapping for InterpolatingProverEnvironment */
class InterpolatingProverEnvironmentView<E> extends BasicProverEnvironmentView<E>
    implements InterpolatingProverEnvironment<E> {

  private final InterpolatingProverEnvironment<E> delegate;

  InterpolatingProverEnvironmentView(
      InterpolatingProverEnvironment<E> pDelegate, FormulaWrappingHandler pWrappingHandler) {
    super(pDelegate, pWrappingHandler);
    delegate = pDelegate;
  }

  @Override
  public BooleanFormula getInterpolant(Collection<E> formulasOfA)
      throws SolverException, InterruptedException {
    return delegate.getInterpolant(formulasOfA);
  }

  @Override
  public List<BooleanFormula> getSeqInterpolants(List<? extends Collection<E>> partitionedFormulas)
      throws SolverException, InterruptedException {
    return delegate.getSeqInterpolants(partitionedFormulas);
  }

  @Override
  public List<BooleanFormula> getTreeInterpolants(
      List<? extends Collection<E>> partitionedFormulas, int[] startOfSubTree)
      throws SolverException, InterruptedException {
    return delegate.getTreeInterpolants(partitionedFormulas, startOfSubTree);
  }
}
