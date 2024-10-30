// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.truth.Truth.assertWithMessage;

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
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.basicimpl.AbstractNumeralFormulaManager.NonLinearArithmetic;

@RunWith(Parameterized.class)
public class ReplaceIntegerDivisionTest {
  private final Solver solver;
  private final IntegerFormulaManager imgr;

  @Parameters(name = "{0}")
  public static Theory[] data() {
    return new Theory[] {Theory.BITVECTOR, Theory.INTEGER};
  }

  public ReplaceIntegerDivisionTest(Theory pTheory) {
    try {
      LogManager logger = LogManager.createTestLogManager();
      ShutdownNotifier notifier = ShutdownNotifier.createDummy();
      Configuration config =
          Configuration.builder()
              .setOption("solver.solver", Solvers.Z3.toString())
              .setOption("solver.nonLinearArithmetic", NonLinearArithmetic.USE.toString())
              .setOption("cpa.predicate.encodeIntegerAs", pTheory.toString())
              .setOption("cpa.predicate.encodeBitvectorAs", Theory.UNSUPPORTED.toString())
              .setOption("cpa.predicate.encodeFloatAs", Theory.UNSUPPORTED.toString())
              .build();

      solver = Solver.create(config, logger, notifier);
      imgr = solver.getFormulaManager().getIntegerFormulaManager();
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

  private int euclideanDivision(int x, int y) {
    int div = x / y;
    if (x < 0 && x != y * div) {
      return div - Integer.signum(y);
    } else {
      return div;
    }
  }

  @Test
  public void integerDivisionTest() {
    for (int x : testValues) {
      for (int y : testValues) {
        var i0 = imgr.makeNumber(x);
        var i1 = imgr.makeNumber(y);
        if (y != 0) {
          assertWithMessage("divide(%s, %s)", x, y)
              .that(eval(imgr.divide(i0, i1)))
              .isEqualTo(euclideanDivision(x, y));
        }
      }
    }
  }

  private int euclideanRemainder(int x, int y) {
    int mod = x % y;
    if (mod < 0) {
      return mod + Math.abs(y);
    } else {
      return mod;
    }
  }

  @Test
  public void integerModuloTest() {
    for (int x : testValues) {
      for (int y : testValues) {
        var i0 = imgr.makeNumber(x);
        var i1 = imgr.makeNumber(y);
        if (y != 0) {
          assertWithMessage("modulo(%s, %s)", x, y)
              .that(eval(imgr.modulo(i0, i1)))
              .isEqualTo(euclideanRemainder(x, y));
        }
      }
    }
  }

  public int eval(NumeralFormula.IntegerFormula pFormula) {
    NumeralFormula.IntegerFormula var = imgr.makeVariable("v");
    try (ProverEnvironment prover =
        solver.newProverEnvironment(SolverContext.ProverOptions.GENERATE_MODELS)) {
      prover.push(imgr.equal(var, pFormula));
      Preconditions.checkArgument(!prover.isUnsat());
      try (Model model = prover.getModel()) {
        return model.evaluate(var).intValue();
      }
    } catch (InterruptedException e) {
      return 0;
    } catch (SolverException e) {
      throw new RuntimeException(e);
    }
  }
}
