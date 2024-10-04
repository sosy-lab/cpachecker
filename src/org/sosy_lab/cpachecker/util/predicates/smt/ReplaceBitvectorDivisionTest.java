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
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.basicimpl.AbstractNumeralFormulaManager.NonLinearArithmetic;

@RunWith(Parameterized.class)
public class ReplaceBitvectorDivisionTest {
  private final Solver solver;
  private final BitvectorFormulaManager bvmgr;

  private Theory theory;

  @Parameters(name = "{0}")
  public static Theory[] data() {
    return new Theory[] {Theory.BITVECTOR, Theory.INTEGER, Theory.RATIONAL};
  }

  public ReplaceBitvectorDivisionTest(Theory pTheory) {
    try {
      LogManager logger = LogManager.createTestLogManager();
      ShutdownNotifier notifier = ShutdownNotifier.createDummy();
      Configuration config =
          Configuration.builder()
              .setOption("solver.solver", Solvers.Z3.toString())
              .setOption("solver.nonLinearArithmetic", NonLinearArithmetic.USE.toString())
              .setOption("cpa.predicate.encodeIntegerAs", Theory.UNSUPPORTED.toString())
              .setOption("cpa.predicate.encodeBitvectorAs", pTheory.toString())
              .setOption("cpa.predicate.encodeFloatAs", Theory.UNSUPPORTED.toString())
              .build();

      theory = pTheory;
      solver = Solver.create(config, logger, notifier);
      bvmgr = solver.getFormulaManager().getBitvectorFormulaManager();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  private ImmutableList<Integer> testValues;

  @Before
  public void init() {
    ImmutableList.Builder<Integer> builder = ImmutableList.builder();
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

  private int truncatedDivision(int x, int y) {
    return x / y;
  }

  @Test
  public void bitvectorDivisionTest() {
    for (int x : testValues) {
      for (int y : testValues) {
        var bv0 = bvmgr.makeBitvector(32, x);
        var bv1 = bvmgr.makeBitvector(32, y);
        assertWithMessage("divide(%s, %s)", x, y)
            .that(eval(bvmgr.divide(bv0, bv1, true)))
            .isEqualTo(truncatedDivision(x, y));
      }
    }
  }

  private int truncatedRemainder(int x, int y) {
    return x % y;
  }

  @Test
  public void bitvectorRemainderTest() {
    // When rationals are used as theory `remainder` is replaced by UFs
    assume().that(theory).isNotEqualTo(Theory.RATIONAL);

    for (int x : testValues) {
      for (int y : testValues) {
        var bv0 = bvmgr.makeBitvector(32, x);
        var bv1 = bvmgr.makeBitvector(32, y);
        assertWithMessage("remainder(%s, %s)", x, y)
            .that(eval(bvmgr.remainder(bv0, bv1, true)))
            .isEqualTo(truncatedRemainder(x, y));
      }
    }
  }

  private int floorRemainder(int x, int y) {
    return Math.floorMod(x, y);
  }

  @Test
  public void bitvectorModuloTest() {
    // When rationals are used a theory `modulo` is replaced by UFs
    assume().that(theory).isNotEqualTo(Theory.RATIONAL);

    for (int x : testValues) {
      for (int y : testValues) {
        var bv0 = bvmgr.makeBitvector(32, x);
        var bv1 = bvmgr.makeBitvector(32, y);
        assertWithMessage("smodulo(%s, %s)", x, y)
            .that(eval(bvmgr.smodulo(bv0, bv1)))
            .isEqualTo(floorRemainder(x, y));
      }
    }
  }

  public int eval(BitvectorFormula pFormula) {
    BitvectorFormula var = bvmgr.makeVariable(32, "v");
    try (ProverEnvironment prover =
        solver.newProverEnvironment(SolverContext.ProverOptions.GENERATE_MODELS)) {
      prover.push(bvmgr.equal(var, pFormula));
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
