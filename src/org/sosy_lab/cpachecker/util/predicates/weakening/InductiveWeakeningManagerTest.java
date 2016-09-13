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
package org.sosy_lab.cpachecker.util.predicates.weakening;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

import java.util.Set;

@RunWith(Parameterized.class)
public class InductiveWeakeningManagerTest {
  @Parameters(name = "{0}")
  public static Object[] getAllSolvers() {
    return Solvers.values();
  }

  @Parameter(0)
  public Solvers solver;

  private InductiveWeakeningManager inductiveWeakeningManager;
  private IntegerFormulaManager ifmgr;
  private BooleanFormulaManager bfmgr;

  @Before
  public void setUp() throws Exception {
    Configuration config = TestDataTools.configurationForTest().setOptions(
        ImmutableMap.of(
            "solver.solver", solver.toString(),

            // Just to please Princess.
            "cpa.predicate.encodeFloatAs", "integer"
        )
    ).build();
    ShutdownNotifier notifier = ShutdownNotifier.createDummy();
    LogManager logger = LogManager.createTestLogManager();
    Solver solver = Solver.create(config, logger, notifier);
    FormulaManagerView fmgr = solver.getFormulaManager();
    inductiveWeakeningManager = new InductiveWeakeningManager(
        config, solver, logger, notifier);
    ifmgr = fmgr.getIntegerFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();

  }

  @Test public void testSlicingVerySimple() throws Exception {
    SSAMap startingSsa = SSAMap.emptySSAMap().withDefault(0);
    PathFormula transition = new PathFormula(
        ifmgr.equal(
            ifmgr.makeVariable("x@1"),
            ifmgr.add(
                ifmgr.makeVariable("x@0"),
                ifmgr.makeNumber(1)
            )
        ),
        startingSsa.builder().setIndex(
            "x", CNumericTypes.INT, 1
        ).build(),
        PointerTargetSet.emptyPointerTargetSet(), 0
    );
    Set<BooleanFormula> lemmas = ImmutableSet.of(
        ifmgr.equal(
            ifmgr.makeVariable("x@0"), ifmgr.makeNumber(1)
        ),
        ifmgr.equal(
            ifmgr.makeVariable("y@0"), ifmgr.makeNumber(0)
        )
    );
    Set<BooleanFormula> weakening = inductiveWeakeningManager
        .findInductiveWeakeningForRCNF(startingSsa, transition, lemmas);
    assertThat(weakening).containsExactly(
        ifmgr.equal(
            ifmgr.makeVariable("y@0"), ifmgr.makeNumber(0)
        )
    );
  }

  @Test public void testRemovingRedundancies() throws Exception {
    IntegerFormula x, y;
    x = ifmgr.makeVariable("x");
    y = ifmgr.makeVariable("y");
    IntegerFormula zero = ifmgr.makeNumber(0);

    BooleanFormula input = bfmgr.and(
        ifmgr.greaterThan(x, zero),
        ifmgr.greaterThan(y, zero),
        ifmgr.greaterThan(ifmgr.add(x, y), zero)
    );

    BooleanFormula simplified = inductiveWeakeningManager.removeRedundancies(
        input
    );
    assertThat(simplified).isEqualTo(
        bfmgr.and(
            ifmgr.greaterThan(x, zero),
            ifmgr.greaterThan(y, zero)
        )
    );
  }
}
