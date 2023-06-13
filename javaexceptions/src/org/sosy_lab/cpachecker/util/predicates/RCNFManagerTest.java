// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.truth.Truth;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.predicates.smt.SolverViewBasedTest0;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

/** Test the semi-CNF conversion. */
@RunWith(Parameterized.class)
public class RCNFManagerTest extends SolverViewBasedTest0 {
  @Parameters(name = "{0}")
  public static Object[] getAllSolvers() {
    return Solvers.values();
  }

  @Parameter(0)
  public Solvers solverToUse;

  @Override
  protected Solvers solverToUse() {
    return solverToUse;
  }

  @Override
  protected ConfigurationBuilder createTestConfigBuilder() {
    return super.createTestConfigBuilder().setOption("rcnf.boundVarsHandling", "drop");
  }

  private RCNFManager rcnfManager;
  private BooleanFormulaManager bfmgr;

  @Before
  public void setUp() throws InvalidConfigurationException {
    rcnfManager = new RCNFManager(config);
    bfmgr = bmgrv;
  }

  @Test
  public void testFactorization() throws Exception {
    BooleanFormula a = bfmgr.and(bfmgr.makeVariable("p"), bfmgr.makeVariable("a"));
    BooleanFormula b = bfmgr.and(bfmgr.makeVariable("p"), bfmgr.makeVariable("b"));
    BooleanFormula c = bfmgr.or(a, b);

    BooleanFormula converted = bfmgr.and(rcnfManager.toLemmas(c, mgrv));
    assertThatFormula(converted).isEquivalentTo(c);
    assertThat(bfmgr.toConjunctionArgs(converted, false))
        .containsExactly(
            bfmgr.makeVariable("p"), bfmgr.or(bfmgr.makeVariable("a"), bfmgr.makeVariable("b")));
  }

  @Test
  public void testNestedConjunctions() throws Exception {
    BooleanFormula input =
        bfmgr.and(
            bfmgr.makeVariable("a"),
            bfmgr.and(
                bfmgr.makeVariable("b"),
                bfmgr.and(bfmgr.makeVariable("c"), bfmgr.makeVariable("d"))));
    Set<BooleanFormula> lemmas = rcnfManager.toLemmas(input, mgrv);
    assertThatFormula(bmgr.and(lemmas)).isEquivalentTo(input);
    Truth.assertThat(lemmas).containsExactly(v("a"), v("b"), v("c"), v("d"));
  }

  @Test
  public void testExplicitExpansion() throws Exception {
    BooleanFormula input =
        bfmgr.or(bfmgr.and(v("a"), v("b"), v("c")), bfmgr.and(v("d"), v("e"), v("f")));
    Set<BooleanFormula> lemmas = rcnfManager.toLemmas(input, mgrv);
    assertThatFormula(bfmgr.and(lemmas)).isEquivalentTo(input);
    Truth.assertThat(lemmas)
        .containsExactly(
            bfmgr.or(v("a"), v("d")),
            bfmgr.or(v("a"), v("e")),
            bfmgr.or(v("a"), v("f")),
            bfmgr.or(v("b"), v("d")),
            bfmgr.or(v("b"), v("e")),
            bfmgr.or(v("b"), v("f")),
            bfmgr.or(v("c"), v("d")),
            bfmgr.or(v("c"), v("e")),
            bfmgr.or(v("c"), v("f")));
  }

  private BooleanFormula v(String name) {
    return bfmgr.makeVariable(name);
  }
}
