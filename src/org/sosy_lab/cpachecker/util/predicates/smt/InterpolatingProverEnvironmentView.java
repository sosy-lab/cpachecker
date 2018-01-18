/*
 * CPAchecker is a tool for configurable software verification.
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
  public BooleanFormula getInterpolant(List<E> formulasOfA)
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
