/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.test.SolverBasedTest0;

/**
 * Test the semi-CNF conversion.
 */
@RunWith(Parameterized.class)
public class RCNFManagerTest extends SolverBasedTest0{
  @Parameters(name = "{0}")
  public static Object[] getAllSolvers() {
    return Solvers.values();
  }

  @Parameter(0)
  public Solvers solver;

  @Override
  protected Solvers solverToUse() {
    return solver;
  }

  @Override
  protected ConfigurationBuilder createTestConfigBuilder() {
    ConfigurationBuilder config = super.createTestConfigBuilder();
    if (solverToUse() == Solvers.PRINCESS || solverToUse() == Solvers.SMTINTERPOL) {
      config.setOption("cpa.predicate.encodeFloatAs", "Integer");
      config.setOption("cpa.predicate.encodeBitvectorAs", "Integer");
    }
    config.setOption("rcnf.boundVarsHandling", "drop");
    return config;
  }

  private RCNFManager RCNFManager;
  private BooleanFormulaManager bfmgr;
  private FormulaManagerView mgrView;

  @Before
  public void setUp() throws InvalidConfigurationException {
    mgrView = new FormulaManagerView(mgr, config, LogManager.createTestLogManager());
    RCNFManager = new RCNFManager(config);
    bfmgr = mgrView.getBooleanFormulaManager();
  }

  @Test
  public void testFactorization() throws Exception{
    BooleanFormula a = bfmgr.and(
        bfmgr.makeVariable("p"),
        bfmgr.makeVariable("a")
    );
    BooleanFormula b = bfmgr.and(
        bfmgr.makeVariable("p"),
        bfmgr.makeVariable("b")
    );
    BooleanFormula c = bfmgr.or(a, b);

    BooleanFormula converted = bfmgr.and(RCNFManager.toLemmas(c, mgrView));
    assertThatFormula(converted).isEquivalentTo(c);
    assertThat(bfmgr.toConjunctionArgs(converted, false))
        .containsExactly(
            bfmgr.makeVariable("p"), bfmgr.or(bfmgr.makeVariable("a"), bfmgr.makeVariable("b")));
  }

  @Test
  public void testNestedConjunctions() throws Exception {
    BooleanFormula input = bfmgr.and(
        bfmgr.makeVariable("a"),
        bfmgr.and(
            bfmgr.makeVariable("b"),
            bfmgr.and(
                bfmgr.makeVariable("c"),
                bfmgr.makeVariable("d")
            )
        )
    );
    Set<BooleanFormula> lemmas = RCNFManager.toLemmas(input, mgrView);
    assertThatFormula(bmgr.and(lemmas)).isEquivalentTo(input);
    Truth.assertThat(lemmas).containsExactly(v("a"), v("b"), v("c"), v("d"));
  }

  @Test
  public void testExplicitExpansion() throws Exception {
    BooleanFormula input = bfmgr.or(
        bfmgr.and(v("a"), v("b"), v("c")),
        bfmgr.and(v("d"), v("e"), v("f"))
    );
    Set<BooleanFormula> lemmas = RCNFManager.toLemmas(input, mgrView);
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
