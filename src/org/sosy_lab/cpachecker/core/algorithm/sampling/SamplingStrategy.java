// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.sampling;

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BasicProverEnvironment;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.SolverException;

public interface SamplingStrategy {

  /**
   * Return a model satisfying the formulas currently on the prover stack as well as an arbitrary
   * number of constraints involving the given variableFormulas which depend only on the implemented
   * strategy. Return null if no satisfying model can be found using the implemented strategy.
   *
   * <p>This function may modify the prover stack during its execution but is guaranteed to have
   * restored it to its previous state on return.
   */
  @Nullable List<ValueAssignment> getModel(
      FormulaManagerView fmgr, Iterable<Formula> variableFormulas, BasicProverEnvironment<?> prover)
      throws InterruptedException, SolverException;
}
