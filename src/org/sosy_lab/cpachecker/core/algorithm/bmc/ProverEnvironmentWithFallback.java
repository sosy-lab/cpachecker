/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.collect.ImmutableList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

class ProverEnvironmentWithFallback
    implements AutoCloseable, InterpolatingProverEnvironment<Object> {

  private final Deque<BooleanFormula> stack = new ArrayDeque<>();

  private final boolean useInterpolation;

  private final Solver solver;

  private @Nullable InterpolatingProverEnvironment<Object> interpolatingProverEnvironment;

  private @Nullable ProverEnvironment proverEnvironment;

  private boolean closed = false;

  private EnumSet<ProverOptions> proverOptions;

  private boolean isUnsat = false;

  public ProverEnvironmentWithFallback(Solver pSolver, ProverOptions... pProverOptions) {
    solver = pSolver;
    proverOptions = EnumSet.noneOf(ProverOptions.class);
    proverOptions.addAll(Arrays.asList(pProverOptions));
    useInterpolation = proverOptions.contains(ProverOptions.GENERATE_UNSAT_CORE);
  }

  private ProverOptions[] getOptions() {
    return proverOptions.toArray(new ProverOptions[proverOptions.size()]);
  }

  @SuppressWarnings("unchecked")
  private void ensureInitialized() {
    if (interpolatingProverEnvironment == null && proverEnvironment == null) {
      if (closed) {
        throw new IllegalStateException("Already closed.");
      }
      if (useInterpolation) {
        interpolatingProverEnvironment =
            (InterpolatingProverEnvironment<Object>)
                solver.newProverEnvironmentWithInterpolation();
      } else {
        proverEnvironment = solver.newProverEnvironment(getOptions());
      }
    }
  }

  @Override
  public void close() {
    if (!closed) {
      if (interpolatingProverEnvironment != null) {
        interpolatingProverEnvironment.close();
      }
      if (proverEnvironment != null) {
        proverEnvironment.close();
      }
      stack.clear();
      closed = true;
    }
  }

  public boolean supportsInterpolation() {
    ensureInitialized();
    return interpolatingProverEnvironment != null;
  }

  public boolean supportsUnsatCoreGeneration() {
    return proverOptions.contains(ProverOptions.GENERATE_UNSAT_CORE);
  }

  @Override
  public Object push(BooleanFormula pFormula) throws InterruptedException {
    ensureInitialized();
    stack.push(pFormula);
    if (supportsInterpolation()) {
      return interpolatingProverEnvironment.push(pFormula);
    }
    proverEnvironment.push(pFormula);
    return this;
  }

  @Override
  public void pop() {
    isUnsat = false;
    ensureInitialized();
    if (supportsInterpolation()) {
      interpolatingProverEnvironment.pop();
    } else {
      proverEnvironment.pop();
    }
    stack.pop();
  }

  @Override
  public boolean isUnsat() throws SolverException, InterruptedException {
    ensureInitialized();
    if (supportsInterpolation()) {
      try {
        return isUnsat = interpolatingProverEnvironment.isUnsat();
      } catch (SolverException solverException) {
        interpolatingProverEnvironment.close();
        interpolatingProverEnvironment = null;
        proverEnvironment = solver.newProverEnvironment(getOptions());
        Iterator<BooleanFormula> it = stack.descendingIterator();
        while (it.hasNext()) {
          proverEnvironment.push(it.next());
        }
        try {
          return isUnsat();
        } catch (SolverException solverException2) {
          solverException.addSuppressed(solverException2);
          throw solverException;
        }
      }
    }
    try {
      return isUnsat = proverEnvironment.isUnsat();
    } catch (SolverException solverException) {
      if (!supportsUnsatCoreGeneration()) {
        throw solverException;
      }
      proverEnvironment.close();
      proverEnvironment = null;
      proverOptions.remove(ProverOptions.GENERATE_UNSAT_CORE);
      proverEnvironment = solver.newProverEnvironment(getOptions());
      Iterator<BooleanFormula> it = stack.descendingIterator();
      while (it.hasNext()) {
        proverEnvironment.push(it.next());
      }
      try {
        return isUnsat();
      } catch (SolverException solverException2) {
        solverException.addSuppressed(solverException2);
        throw solverException;
      }
    }
  }

  @Override
  public boolean isUnsatWithAssumptions(Collection<BooleanFormula> pArg0)
      throws SolverException, InterruptedException {
    ensureInitialized();
    if (supportsInterpolation()) {
      return interpolatingProverEnvironment.isUnsatWithAssumptions(pArg0);
    }
    return proverEnvironment.isUnsatWithAssumptions(pArg0);
  }

  @Override
  public Optional<List<BooleanFormula>> unsatCoreOverAssumptions(Collection<BooleanFormula> pArg0)
      throws SolverException, InterruptedException {
    if (supportsInterpolation()) {
      return interpolatingProverEnvironment.unsatCoreOverAssumptions(pArg0);
    }
    return proverEnvironment.unsatCoreOverAssumptions(pArg0);
  }

  @Override
  public List<BooleanFormula> getUnsatCore() {
    ensureInitialized();
    if (isUnsat && supportsUnsatCoreGeneration()) {
      return new ArrayList<>(stack);
    }
    if (supportsInterpolation()) {
      return interpolatingProverEnvironment.getUnsatCore();
    }
    return proverEnvironment.getUnsatCore();
  }

  @Override
  public ImmutableList<ValueAssignment> getModelAssignments() throws SolverException {
    ensureInitialized();
    if (supportsInterpolation()) {
      return interpolatingProverEnvironment.getModelAssignments();
    }
    return proverEnvironment.getModelAssignments();
  }

  public BooleanFormula getInterpolant(Iterable<Object> pAssertionIds)
      throws SolverException, InterruptedException {
    ensureInitialized();
    if (!supportsInterpolation()) {
      throw new IllegalStateException("Interpolation has been switched off.");
    }
    final List<Object> assertionIds;
    if (pAssertionIds instanceof List) {
      assertionIds = (List<Object>) pAssertionIds;
    } else {
      assertionIds = ImmutableList.copyOf(pAssertionIds);
    }
    try {
      return interpolatingProverEnvironment.getInterpolant(assertionIds);
    } catch (SolverException solverException) {
      interpolatingProverEnvironment.close();
      interpolatingProverEnvironment = null;
      proverEnvironment = solver.newProverEnvironment(getOptions());
      Iterator<BooleanFormula> it = stack.descendingIterator();
      while (it.hasNext()) {
        proverEnvironment.push(it.next());
      }
      throw solverException;
    }
  }

  @Override
  @Nullable
  public Object addConstraint(BooleanFormula pArg0) throws InterruptedException {
    ensureInitialized();
    if (supportsInterpolation()) {
      return interpolatingProverEnvironment.addConstraint(pArg0);
    }
    return proverEnvironment.addConstraint(pArg0);
  }

  @Override
  public Model getModel() throws SolverException {
    ensureInitialized();
    if (supportsInterpolation()) {
      return interpolatingProverEnvironment.getModel();
    }
    return proverEnvironment.getModel();
  }

  @Override
  public void push() {
    ensureInitialized();
    if (supportsInterpolation()) {
      interpolatingProverEnvironment.push();
    }
    proverEnvironment.push();
  }

  @Override
  public List<BooleanFormula> getSeqInterpolants(List<? extends Collection<Object>> pArg0)
      throws SolverException, InterruptedException {
    ensureInitialized();
    if (!supportsInterpolation()) {
      throw new IllegalStateException("Interpolation has been switched off.");
    }
    return interpolatingProverEnvironment.getSeqInterpolants(pArg0);
  }

  @Override
  public List<BooleanFormula> getTreeInterpolants(
      List<? extends Collection<Object>> pArg0, int[] pArg1)
      throws SolverException, InterruptedException {
    ensureInitialized();
    if (!supportsInterpolation()) {
      throw new IllegalStateException("Interpolation has been switched off.");
    }
    return interpolatingProverEnvironment.getTreeInterpolants(pArg0, pArg1);
  }

  @Override
  public BooleanFormula getInterpolant(List<Object> pArg0)
      throws SolverException, InterruptedException {
    return getInterpolant((Iterable<Object>) pArg0);
  }

  @Override
  public <R> R allSat(AllSatCallback<R> pArg0, List<BooleanFormula> pArg1)
      throws InterruptedException, SolverException {
    ensureInitialized();
    if (supportsInterpolation()) {
      return interpolatingProverEnvironment.allSat(pArg0, pArg1);
    }
    return proverEnvironment.allSat(pArg0, pArg1);
  }

  public boolean isEmpty() {
    return stack.isEmpty();
  }
}