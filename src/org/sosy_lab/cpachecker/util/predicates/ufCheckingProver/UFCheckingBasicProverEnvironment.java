// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.ufCheckingProver;

import com.google.common.collect.ImmutableList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BasicProverEnvironment;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Get the model, substitute implementation for UFs which were used to replace
 * non-linear numerical operations (overflow/etc), and if the model does not
 * hold anymore, generate a new one.
 */
public class UFCheckingBasicProverEnvironment<T> implements BasicProverEnvironment<T> {

  private final BasicProverEnvironment<T> delegate;
  private final LogManager logger;
  private final BooleanFormulaManager bfmgr;
  private final FunctionApplicationManager faMgr;

  private final UFCheckingProverOptions options;

  // We count the number of pushed constraints,
  // because we keep constraints, until the last pushed formula is popped.
  private final Deque<Integer> pushedConstraints = new ArrayDeque<>();


  @Options(prefix="cpa.predicate.solver.ufCheckingProver")
  public static class UFCheckingProverOptions {

    /**
     * For some UFs we can compute the correct result for the given parameters,
     * but then the solver chooses new parameters and we have to compute a new result. Again, and again.
     * Example: we try to solve "a=2 & UF_multiply(a,b)=5" and try b=[1,2,3,...].
     * Thus we abort after some iterations and ignore the invalid result of the UF.
     * This procedure should be sound.
     */
    @Option(description = "How often should we try to get a better evaluation?")
    private int maxIterationNum = 5;

    @Option(description = "C99 only defines the overflow of unsigned integer type.")
    private boolean isSignedOverflowSafe = true;

    public UFCheckingProverOptions(Configuration config) throws InvalidConfigurationException {
      config.inject(this);
    }

    boolean isSignedOverflowSafe() {
      return isSignedOverflowSafe;
    }
  }

  public UFCheckingBasicProverEnvironment(LogManager pLogger, BasicProverEnvironment<T> bpe,
      FormulaManagerView pFmgr, UFCheckingProverOptions options) {
    this.delegate = bpe;
    this.logger = pLogger;
    this.bfmgr = pFmgr.getBooleanFormulaManager();
    this.faMgr = new FunctionApplicationManager(pFmgr, pLogger, options);
    this.options = options;
  }

  @Override
  public T push(BooleanFormula f) throws InterruptedException {

    // add new level
    pushedConstraints.addLast(0);

    return delegate.push(f);
  }

  @Override
  public void pop() {

    // first pop constraints
    for (int i = 0; i < pushedConstraints.getLast(); i++) {
      delegate.pop();
    }

    // reset counter to last entry
    pushedConstraints.removeLast();

    // then pop the basic formula
    delegate.pop();
  }

  @Override
  public T addConstraint(BooleanFormula constraint) throws InterruptedException {
    return delegate.addConstraint(constraint);
  }

  @Override
  public void push() {

    // add new level
    pushedConstraints.addLast(0);

    delegate.push();
  }

  @Override
  public boolean isUnsat() throws SolverException, InterruptedException {
    boolean unsat = delegate.isUnsat();
    int additionalConstraints = 0;
    while (!unsat) {

      final List<BooleanFormula> constraints = new ArrayList<>();

      // next line only succeeds if the solver supports the generation of a model.
      // TODO enable by default for MathSat?

      try (final Model model = getModel()) {
        for (ValueAssignment entry : model) {

          if (!entry.isFunction()) {

            // We are only interested in UFs.
            continue;
          }

          final Object value = entry.getValue();
          final BooleanFormula newAssignment = faMgr.evaluate(entry, value);

          if (!bfmgr.isTrue(newAssignment)) {
            constraints.add(newAssignment);
          }
        }
      }
      if (constraints.isEmpty()) {
        logger.log(Level.FINE, "no UFs to improve");
        break;
      }

      if (additionalConstraints > options.maxIterationNum) {
        logger.log(Level.INFO, "aborting further sat-checks with UF-checking");
        break;
      }

      // push the new constraints and re-check for satisfiability

      additionalConstraints++;
      push(bfmgr.and(constraints));
      unsat = delegate.isUnsat();
    }

    // update level-counter
    pushedConstraints.addLast(pushedConstraints.removeLast() + additionalConstraints);

    return unsat;
  }

  @Override
  public Model getModel() throws SolverException {
    return delegate.getModel();
  }

  @Override
  public ImmutableList<ValueAssignment> getModelAssignments() throws SolverException {
    return delegate.getModelAssignments();
  }

  @Override
  public void close() {
    delegate.close();
  }

  @Override
  public boolean isUnsatWithAssumptions(Collection<BooleanFormula> assumptions)
      throws SolverException, InterruptedException {
    return delegate.isUnsatWithAssumptions(assumptions);
  }

  @Override
  public List<BooleanFormula> getUnsatCore() {
    return delegate.getUnsatCore();
  }

  @Override
  public <R> R allSat(AllSatCallback<R> callback, List<BooleanFormula> important)
      throws InterruptedException, SolverException {
    return delegate.allSat(callback, important);
  }

  @Override
  public Optional<List<BooleanFormula>> unsatCoreOverAssumptions(
      Collection<BooleanFormula> assumptions) throws SolverException, InterruptedException {
    return delegate.unsatCoreOverAssumptions(assumptions);
  }
}
