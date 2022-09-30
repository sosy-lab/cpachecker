// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.java_smt.api.BasicProverEnvironment;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.SolverException;

public class BasicProverEnvironmentView<E> implements BasicProverEnvironment<E> {

  private final BasicProverEnvironment<E> delegate;
  private final FormulaWrappingHandler wrappingHandler;

  public BasicProverEnvironmentView(
      BasicProverEnvironment<E> pDelegate, FormulaWrappingHandler pWrappingHandler) {
    delegate = pDelegate;
    wrappingHandler = pWrappingHandler;
  }

  @Override
  public E push(BooleanFormula f) throws InterruptedException {
    return delegate.push(f);
  }

  @Override
  public void pop() {
    delegate.pop();
  }

  @Override
  public E addConstraint(BooleanFormula constraint) throws InterruptedException {
    return delegate.addConstraint(constraint);
  }

  @Override
  public void push() {
    delegate.push();
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public boolean isUnsat() throws SolverException, InterruptedException {
    return delegate.isUnsat();
  }

  @Override
  public Model getModel() throws SolverException {
    return new ModelView(delegate.getModel(), wrappingHandler);
  }

  @Override
  public ImmutableList<ValueAssignment> getModelAssignments() throws SolverException {
    return fixModelAssignments(delegate.getModelAssignments());
  }

  /** Z3 adds irrelevant terms to the model if quantifiers and UFs are used, remove them. */
  static ImmutableList<ValueAssignment> fixModelAssignments(ImmutableList<ValueAssignment> m) {
    if (m.stream().noneMatch(valueAssignment -> valueAssignment.getName().contains("!"))) {
      return m; // fast path for common case
    }

    return from(m).filter(ModelView::isRelevantModelTerm).toList();
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
  public Optional<List<BooleanFormula>> unsatCoreOverAssumptions(
      Collection<BooleanFormula> pAssumptions) throws SolverException, InterruptedException {
    return delegate.unsatCoreOverAssumptions(pAssumptions);
  }

  @Override
  public <R> R allSat(
      org.sosy_lab.java_smt.api.BasicProverEnvironment.AllSatCallback<R> pCallback,
      List<BooleanFormula> pImportant)
      throws InterruptedException, SolverException {
    return delegate.allSat(pCallback, pImportant);
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
