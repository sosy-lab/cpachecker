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
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.EnumerationFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FloatingPointNumber;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.RationalFormula;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.StringFormula;

/**
 * This is a class that allows to use a different SMT solver for interpolation than for the rest.
 * Whenever it is used, it copies the formulas to the interpolation SMT solver and back accordingly.
 */
public class SeparateInterpolatingProverEnvironment<T>
    implements InterpolatingProverEnvironment<T> {

  private final FormulaManagerView mainFmgr;
  private final FormulaManagerView itpFmgr;
  private final InterpolatingProverEnvironment<T> itpEnv;

  public SeparateInterpolatingProverEnvironment(
      FormulaManagerView pMainFmgr,
      FormulaManagerView pItpFmgr,
      InterpolatingProverEnvironment<T> pItpEnv) {
    mainFmgr = checkNotNull(pMainFmgr);
    itpFmgr = checkNotNull(pItpFmgr);
    itpEnv = checkNotNull(pItpEnv);
  }

  @Override
  public T push(BooleanFormula mainF) throws InterruptedException {
    return itpEnv.push(convertToItp(mainF));
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
  public void push() throws InterruptedException {
    itpEnv.push();
  }

  @Override
  public int size() {
    return itpEnv.size();
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
    return itpFmgr.translateFrom(f, mainFmgr);
  }

  private Formula convertToItp(Formula f) {
    return itpFmgr.parseArbitraryFormula(mainFmgr.dumpArbitraryFormula(f));
  }

  private BooleanFormula convertToMain(BooleanFormula f) {
    return mainFmgr.translateFrom(f, itpFmgr);
  }

  private Formula convertToMain(Formula f) {
    return mainFmgr.parseArbitraryFormula(itpFmgr.dumpArbitraryFormula(f));
  }

  @Override
  public Model getModel() throws SolverException {
    Model itpModel = itpEnv.getModel();
    return new Model() {

      @Override
      public ImmutableList<ValueAssignment> asList() {
        return ImmutableList.copyOf(
            Lists.transform(
                itpModel.asList(),
                valueAssignment ->
                    new ValueAssignment(
                        convertToMain(valueAssignment.getKey()),
                        convertToMain(valueAssignment.getValueAsFormula()),
                        convertToMain(valueAssignment.getAssignmentAsFormula()),
                        valueAssignment.getName(),
                        valueAssignment.getValue(),
                        valueAssignment.getArgumentsInterpretation())));
      }

      @Override
      @SuppressWarnings("unchecked")
      public <S extends Formula> @Nullable S eval(S formula) {
        return (S) convertToMain(itpModel.eval(convertToItp(formula)));
      }

      @Override
      public @Nullable Object evaluate(Formula formula) {
        return itpModel.evaluate(convertToItp(formula));
      }

      @Override
      public @Nullable BigInteger evaluate(IntegerFormula formula) {
        return itpModel.evaluate((IntegerFormula) convertToItp(formula));
      }

      @Override
      public @Nullable Rational evaluate(RationalFormula formula) {
        return itpModel.evaluate((RationalFormula) convertToItp(formula));
      }

      @Override
      public @Nullable Boolean evaluate(BooleanFormula formula) {
        return itpModel.evaluate(convertToItp(formula));
      }

      @Override
      public @Nullable BigInteger evaluate(BitvectorFormula formula) {
        return itpModel.evaluate((BitvectorFormula) convertToItp(formula));
      }

      @Override
      public @Nullable String evaluate(StringFormula formula) {
        return itpModel.evaluate((StringFormula) convertToItp(formula));
      }

      @Override
      public @Nullable String evaluate(EnumerationFormula formula) {
        return itpModel.evaluate((EnumerationFormula) convertToItp(formula));
      }

      @Override
      public @Nullable FloatingPointNumber evaluate(FloatingPointFormula formula) {
        return itpModel.evaluate((FloatingPointFormula) convertToItp(formula));
      }

      @Override
      public void close() {
        itpModel.close();
      }
    };
  }

  @Override
  public ImmutableList<ValueAssignment> getModelAssignments() throws SolverException {
    return getModel().asList();
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
    return opt.map(value -> Lists.transform(value, this::convertToMain));
  }

  @Override
  public <R> R allSat(AllSatCallback<R> pCallback, List<BooleanFormula> pImportant)
      throws InterruptedException, SolverException {
    AllSatCallback<R> itpCallback =
        new AllSatCallback<>() {
          @Override
          public void apply(List<BooleanFormula> model) {
            pCallback.apply(
                Lists.transform(model, SeparateInterpolatingProverEnvironment.this::convertToMain));
          }

          @Override
          public R getResult() throws InterruptedException {
            return pCallback.getResult();
          }
        };
    return itpEnv.allSat(itpCallback, Lists.transform(pImportant, this::convertToItp));
  }
}
