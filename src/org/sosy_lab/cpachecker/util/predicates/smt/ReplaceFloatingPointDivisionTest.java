// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.truth.TruthJUnit.assume;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.Theory;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormulaManager;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.basicimpl.AbstractNumeralFormulaManager.NonLinearArithmetic;

@RunWith(Parameterized.class)
public class ReplaceFloatingPointDivisionTest {
  private final Solver solver;
  private final FloatingPointFormulaManager fpmgr;

  private Theory theory;

  private static final FloatingPointType DOUBLE_PRECISION =
      FormulaType.getDoublePrecisionFloatingPointType();

  @Parameters(name = "{0}")
  public static Theory[] data() {
    return new Theory[] {Theory.FLOAT, Theory.INTEGER, Theory.RATIONAL};
  }

  public ReplaceFloatingPointDivisionTest(Theory pTheory) {
    try {
      LogManager logger = LogManager.createTestLogManager();
      ShutdownNotifier notifier = ShutdownNotifier.createDummy();
      Configuration config =
          Configuration.builder()
              .setOption("solver.solver", Solvers.Z3.toString())
              .setOption("solver.nonLinearArithmetic", NonLinearArithmetic.USE.toString())
              .setOption("cpa.predicate.encodeIntegerAs", Theory.UNSUPPORTED.toString())
              .setOption("cpa.predicate.encodeBitvectorAs", Theory.UNSUPPORTED.toString())
              .setOption("cpa.predicate.encodeFloatAs", pTheory.toString())
              .build();

      theory = pTheory;
      solver = Solver.create(config, logger, notifier);
      fpmgr = solver.getFormulaManager().getFloatingPointFormulaManager();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  private ImmutableList<Integer> testValues;

  @Before
  public void init() {
    ImmutableList.Builder<Integer> builder = ImmutableList.builder();
    builder.add(0);
    builder.add(1);
    builder.add(Integer.MIN_VALUE);
    Random random = new Random(0);
    int c = 0;
    while (c < 20) {
      int r = random.nextInt();
      if (r != 0) {
        builder.add(r);
        c++;
      }
    }
    testValues = builder.build();
  }

  private double rneDivision(int x, int y) {
    return ((double) x) / y;
  }

  @Test
  public void floatDivisionTest() {
    // When the theory is Theory.INTEGER `division` will round to the next integer
    assume().that(theory).isNotEqualTo(Theory.INTEGER);

    for (int x : testValues) {
      for (int y : testValues) {
        var f0 = fpmgr.makeNumber(x, DOUBLE_PRECISION);
        var f1 = fpmgr.makeNumber(y, DOUBLE_PRECISION);
        if (y != 0) {
          assertWithMessage("divide(%s, %s)", x, y)
              .that(eval(fpmgr.divide(f0, f1)) == rneDivision(x, y))
              .isTrue();
        }
      }
    }
  }

  private double rneRemainder(int x, int y) {
    return Math.IEEEremainder(x, y);
  }

  @Test
  public void floatRemainderTest() {
    // If the theory is not Theory.FLOAT an UF symbol is used for `remainder` and the test fails
    assume().that(theory).isNoneOf(Theory.INTEGER, Theory.RATIONAL);

    for (int x : testValues) {
      for (int y : testValues) {
        var f0 = fpmgr.makeNumber(x, DOUBLE_PRECISION);
        var f1 = fpmgr.makeNumber(y, DOUBLE_PRECISION);
        if (y != 0) {
          assertWithMessage("remainder(%s, %s)", x, y)
              .that(eval(fpmgr.remainder(f0, f1)))
              .isEqualTo(rneRemainder(x, y));
        }
      }
    }
  }

  public double eval(FloatingPointFormula pFormula) {
    FloatingPointFormula var = fpmgr.makeVariable("v", DOUBLE_PRECISION);
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(fpmgr.assignment(var, pFormula));
      Preconditions.checkArgument(!prover.isUnsat());
      try (Model model = prover.getModel()) {
        return model.evaluate(var).doubleValue();
      }
    } catch (InterruptedException e) {
      return 0;
    } catch (SolverException e) {
      throw new RuntimeException(e);
    }
  }
}
