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
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

@RunWith(Parameterized.class)
public class RemainderBitvectorTest {
  private Solver solver;
  private FormulaManagerView fmgr;
  private BitvectorFormulaManagerView bvfmgr;

  @SuppressWarnings("unused")
  private final Solvers solverName;

  @Parameters(name = "{0}")
  public static Solvers[] data() {
    return new Solvers[] {
      // Solvers.MATHSAT5,
      Solvers.Z3, Solvers.PRINCESS, Solvers.BOOLECTOR, Solvers.CVC4, Solvers.CVC5, Solvers.BITWUZLA,
    };
  }

  public RemainderBitvectorTest(Solvers pSolvers) {
    try {
      Configuration config =
          Configuration.builder()
              .setOption("solver.solver", pSolvers.toString())
              .setOption("cpa.predicate.encodeFloatAs", Theory.UNSUPPORTED.toString())
              .setOption("cpa.predicate.encodeIntegerAs", Theory.UNSUPPORTED.toString())
              .build();
      ShutdownNotifier notifier = ShutdownNotifier.createDummy();
      LogManager logger = LogManager.createTestLogManager();

      solver = Solver.create(config, logger, notifier);
      solverName = pSolvers;
      fmgr = solver.getFormulaManager();
      bvfmgr = fmgr.getBitvectorFormulaManager();
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
  public void BitvectorDivisionTest() {
    for (Pair<Integer, Integer> value : testValues) {
      int x = value.getFirstNotNull();
      int y = value.getSecondNotNull();
      assertThat(
              eval(bvfmgr.divide(bvfmgr.makeBitvector(32, x), bvfmgr.makeBitvector(32, y), true)))
          .isEqualTo(x / y);
    }
  }

  @Test
  public void BitvectorRemainderTest() {
    for (Pair<Integer, Integer> value : testValues) {
      int x = value.getFirstNotNull();
      int y = value.getSecondNotNull();
      assertThat(
              eval(
                  bvfmgr.remainder(bvfmgr.makeBitvector(32, x), bvfmgr.makeBitvector(32, y), true)))
          .isEqualTo(x % y);
    }
  }

  public int eval(BitvectorFormula pFormula) {
    BitvectorFormula var = bvfmgr.makeVariable(32, "v");
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(bvfmgr.equal(var, pFormula));
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
