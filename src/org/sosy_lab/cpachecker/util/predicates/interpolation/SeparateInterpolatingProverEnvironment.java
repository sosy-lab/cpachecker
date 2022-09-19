// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.interpolation;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * This is a class that allows to use a different SMT solver for interpolation than for the rest.
 * Whenever it is used, it copies the formulas to the interpolation SMT solver and back accordingly.
 */
public class SeparateInterpolatingProverEnvironment<T>
    implements InterpolatingProverEnvironment<T> {

  private final FormulaManager mainFmgr;
  private final FormulaManager itpFmgr;
  private final InterpolatingProverEnvironment<T> itpEnv;

  public SeparateInterpolatingProverEnvironment(
      FormulaManager pMainFmgr,
      FormulaManager pItpFmgr,
      InterpolatingProverEnvironment<T> pItpEnv) {
    mainFmgr = checkNotNull(pMainFmgr);
    itpFmgr = checkNotNull(pItpFmgr);
    itpEnv = checkNotNull(pItpEnv);
  }

  @Override
  public T push(BooleanFormula mainF) throws InterruptedException {
    BooleanFormula itpF = itpFmgr.parse(mainFmgr.dumpFormula(mainF).toString());
    return itpEnv.push(itpF);
  }

  @Override
  public void pop() {
    itpEnv.pop();
  }

  @Override
  public T addConstraint(BooleanFormula constraint) throws InterruptedException {
    return itpEnv.addConstraint(convertToItp(constraint));
  }

  @Override
  public void push() {
    itpEnv.push();
  }

  @Override
  public boolean isUnsat() throws InterruptedException, SolverException {
    return itpEnv.isUnsat();
  }

  @Override
  public boolean isUnsatWithAssumptions(Collection<BooleanFormula> assumptions)
      throws SolverException, InterruptedException {
    return itpEnv.isUnsatWithAssumptions(Collections2.transform(assumptions, this::convertToItp));
  }

  @Override
  public void close() {
    itpEnv.close();
  }

  @Override
  public BooleanFormula getInterpolant(Collection<T> pFormulasOfA)
      throws SolverException, InterruptedException {
    BooleanFormula itpF = itpEnv.getInterpolant(pFormulasOfA);
    return convertToMain(itpF);
  }

  @Override
  public List<BooleanFormula> getSeqInterpolants(List<? extends Collection<T>> partitionedFormulas)
      throws SolverException, InterruptedException {
    final List<BooleanFormula> itps = itpEnv.getSeqInterpolants(partitionedFormulas);
    return Lists.transform(itps, this::convertToMain);
  }

  @Override
  public List<BooleanFormula> getTreeInterpolants(
      List<? extends Collection<T>> partitionedFormulas, int[] startOfSubTree)
      throws SolverException, InterruptedException {
    final List<BooleanFormula> itps =
        itpEnv.getTreeInterpolants(partitionedFormulas, startOfSubTree);
    return Lists.transform(itps, this::convertToMain);
  }

  private BooleanFormula convertToItp(BooleanFormula f) {
    return itpFmgr.parse(mainFmgr.dumpFormula(f).toString());
  }

  private BooleanFormula convertToMain(BooleanFormula f) {
    return mainFmgr.parse(itpFmgr.dumpFormula(f).toString());
  }

  @Override
  public Model getModel() throws SolverException {
    return itpEnv.getModel();
  }

  @Override
  public ImmutableList<ValueAssignment> getModelAssignments() throws SolverException {
    return itpEnv.getModelAssignments();
  }

  @Override
  public List<BooleanFormula> getUnsatCore() {
    return Lists.transform(itpEnv.getUnsatCore(), this::convertToMain);
  }

  @Override
  public Optional<List<BooleanFormula>> unsatCoreOverAssumptions(
      Collection<BooleanFormula> pAssumptions) throws SolverException, InterruptedException {
    Optional<List<BooleanFormula>> opt =
        itpEnv.unsatCoreOverAssumptions(Collections2.transform(pAssumptions, this::convertToItp));
    if (opt.isPresent()) {
      return Optional.of(Lists.transform(opt.orElseThrow(), this::convertToMain));
    } else {
      return opt;
    }
  }

  @Override
  public <R> R allSat(AllSatCallback<R> pCallback, List<BooleanFormula> pImportant)
      throws InterruptedException, SolverException {
    return itpEnv.allSat(pCallback, Lists.transform(pImportant, this::convertToItp));
  }
}
