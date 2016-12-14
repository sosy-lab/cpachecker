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

import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
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

  private RCNFManager RCNFManager;
  private BooleanFormulaManager bfmgr;
  private FormulaManagerView mgrView;

  @Before
  public void setUp() throws InvalidConfigurationException {
    Configuration d = Configuration.builder().setOption(
        "rcnf.boundVarsHandling", "drop"
    ).build();
    mgrView = new FormulaManagerView(mgr, d, LogManager.createTestLogManager());
    RCNFManager = new RCNFManager(d);
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
    assertThatFormula(converted).isEqualTo(
        bfmgr.and(
            bfmgr.makeVariable("p"),
            bfmgr.or(
                bfmgr.makeVariable("a"),
                bfmgr.makeVariable("b")
            )
        )
    );
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
    BooleanFormula converted = bfmgr.and(RCNFManager.toLemmas(input, mgrView));
    assertThatFormula(converted).isEquivalentTo(input);
    BooleanFormula expected =
        bfmgr.and(
            bfmgr.makeVariable("a"),
            bfmgr.makeVariable("b"),
            bfmgr.makeVariable("c"),
            bfmgr.makeVariable("d")
        );
    Truth.assertThat(bfmgr.toConjunctionArgs(converted, true)).isEqualTo(bfmgr.toConjunctionArgs(
        expected, true
    ));
  }

  @Test
  public void testExplicitExpansion() throws Exception {
    BooleanFormula input = bfmgr.or(
        bfmgr.and(ImmutableList.of(v("a"), v("b"), v("c"))),
        bfmgr.and(ImmutableList.of(v("d"), v("e"), v("f")))
    );
    BooleanFormula converted = bfmgr.and(RCNFManager.toLemmas(input, mgrView));
    assertThatFormula(converted).isEquivalentTo(input);
    BooleanFormula expected =
        bfmgr.and(
            bfmgr.or(v("a"), v("d")),
            bfmgr.or(v("a"), v("e")),
            bfmgr.or(v("a"), v("f")),
            bfmgr.or(v("b"), v("d")),
            bfmgr.or(v("b"), v("e")),
            bfmgr.or(v("b"), v("f")),
            bfmgr.or(v("c"), v("d")),
            bfmgr.or(v("c"), v("e")),
            bfmgr.or(v("c"), v("f"))
        );
    Truth.assertThat(bfmgr.toConjunctionArgs(converted, true)).isEqualTo(
        bfmgr.toConjunctionArgs(expected, true));
  }

  private BooleanFormula v(String name) {
    return bfmgr.makeVariable(name);
  }
}
