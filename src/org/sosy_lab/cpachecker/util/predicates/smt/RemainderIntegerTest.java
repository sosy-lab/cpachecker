// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.Theory;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

@RunWith(Parameterized.class)
public class RemainderIntegerTest {
  private Solver solver;
  private FormulaManagerView fmgr;
  private BooleanFormulaManagerView bfmgr;
  private IntegerFormulaManagerView ifmgr;

  private final Solvers solverName;

  @Parameters(name = "{0}")
  public static Solvers[] data() {
    return new Solvers[] {
      Solvers.OPENSMT,
      Solvers.SMTINTERPOL,
      Solvers.Z3,
      Solvers.PRINCESS,
      Solvers.CVC4,
      Solvers.CVC5,
    };
  }

  public RemainderIntegerTest(Solvers pSolvers) {
    try {
      Configuration config =
          Configuration.builder()
              .setOption("solver.solver", pSolvers.toString())
              .setOption("solver.nonLinearArithmetic", "USE")
              .setOption("cpa.predicate.encodeFloatAs", Theory.UNSUPPORTED.toString())
              .setOption("cpa.predicate.encodeBitvectorAs", Theory.UNSUPPORTED.toString())
              .build();
      ShutdownNotifier notifier = ShutdownNotifier.createDummy();
      LogManager logger = LogManager.createTestLogManager();

      solver = Solver.create(config, logger, notifier);
      solverName = pSolvers;
      fmgr = solver.getFormulaManager();
      bfmgr = fmgr.getBooleanFormulaManager();
      ifmgr = fmgr.getIntegerFormulaManager();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  ImmutableList<Pair<Integer, Integer>> testValues =
      ImmutableList.of(
          Pair.of(5, 3),
          Pair.of(5, -3),
          Pair.of(-5, 3),
          Pair.of(-5, -3),
          Pair.of(3, 2),
          Pair.of(3, -2),
          Pair.of(-3, 2),
          Pair.of(-3, -2),
          Pair.of(1, 2),
          Pair.of(1, -2),
          Pair.of(-1, 2),
          Pair.of(-1, -2));

  @Test
  public void IntegerDivisionTest() {
    for (Pair<Integer, Integer> value : testValues) {
      int x = value.getFirstNotNull();
      int y = value.getSecondNotNull();
      assertThat(eval(ifmgr.divide(ifmgr.makeNumber(x), ifmgr.makeNumber(y)))).isEqualTo(x / y);
    }
  }

  @Test
  public void IntegerRemainderTest() {
    for (Pair<Integer, Integer> value : testValues) {
      int x = value.getFirstNotNull();
      int y = value.getSecondNotNull();
      assertThat(eval(ifmgr.modulo(ifmgr.makeNumber(x), ifmgr.makeNumber(y)))).isEqualTo(x % y);
    }
  }

  public int eval(IntegerFormula pFormula) {
    IntegerFormula var = ifmgr.makeVariable("v");
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(ifmgr.equal(var, pFormula));
      Preconditions.checkArgument(!prover.isUnsat());
      return prover.getModel().evaluate(var).intValueExact();
    } catch (InterruptedException e) {
      return 0;
    } catch (SolverException e) {
      throw new RuntimeException(e);
    }
  }
}
