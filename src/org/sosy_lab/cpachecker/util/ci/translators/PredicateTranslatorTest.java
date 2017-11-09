/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.ci.translators;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.SymbolicRegionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.SolverViewBasedTest0;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

@RunWith(Parameterized.class)
public class PredicateTranslatorTest extends SolverViewBasedTest0 {

  @Parameters(name = "{0}")
  public static Object[] getAllSolvers() {
    return Solvers.values();
  }

  @Parameter(0)
  public Solvers solverUnderTest;

  @Override
  protected Solvers solverToUse() {
    return solverUnderTest;
  }

  private PredicateRequirementsTranslator pReqTrans;
  private PredicateAbstractState ptrueState;
  private PredicateAbstractState pf1State;
  private PredicateAbstractState pf2State;
  private SSAMap ssaTest;

  @Before
  public void init() throws Exception {
    FormulaManagerView fmv = mgrv;
    pReqTrans = new PredicateRequirementsTranslator(fmv);

    SSAMapBuilder ssaBuilder = SSAMap.emptySSAMap().builder();
    ssaBuilder.setIndex("var1", CNumericTypes.INT, 1);
    ssaBuilder.setIndex("var3", CNumericTypes.INT, 1);
    ssaBuilder.setIndex("fun::varB", CNumericTypes.INT, 1);
    ssaTest = ssaBuilder.build();

    // Region used in abstractionFormula
    RegionManager regionManager = new SymbolicRegionManager(solver);
    Region region = regionManager.makeTrue();

    // Initialize formula manager
    BooleanFormulaManager bfmgr = fmv.getBooleanFormulaManager();
    BooleanFormula bf = bfmgr.makeTrue();

    // create empty path formula
    PathFormula pathFormula =
        new PathFormula(bf, SSAMap.emptySSAMap(), PointerTargetSet.emptyPointerTargetSet(), 0);

    // create PredicateAbstractState ptrueState
    AbstractionFormula aFormula =
        new AbstractionFormula(fmv, region, bf, bf, pathFormula, ImmutableSet.of());
    ptrueState =
        PredicateAbstractState.mkAbstractionState(
            pathFormula, aFormula, PathCopyingPersistentTreeMap.of());

    // create PredicateAbstractState pf1State
    IntegerFormulaManager ifmgr = fmv.getIntegerFormulaManager();
    BooleanFormula bf11 = ifmgr.greaterThan(ifmgr.makeVariable("var1"), ifmgr.makeNumber(0));
    BooleanFormula bf12 = ifmgr.equal(ifmgr.makeVariable("var3"), ifmgr.makeNumber(0));
    BooleanFormula bf13 = ifmgr.lessThan(ifmgr.makeVariable("fun::var1"), ifmgr.makeNumber(0));
    BooleanFormula bf14 = bfmgr.or(bf11, bf12);
    BooleanFormula bf1 = bfmgr.and(bf14, bf13);
    aFormula =
        new AbstractionFormula(fmv, region, bf1, bfmgr.makeTrue(), pathFormula, ImmutableSet.of());
    pf1State =
        PredicateAbstractState.mkAbstractionState(
            pathFormula, aFormula, PathCopyingPersistentTreeMap.of());

    // create PredicateAbstractState pf2State
    BooleanFormula bf21 =
        ifmgr.greaterThan(ifmgr.makeVariable("var2"), ifmgr.makeVariable("fun::varB"));
    BooleanFormula bf22 = ifmgr.lessThan(ifmgr.makeVariable("fun::varC"), ifmgr.makeNumber(0));
    BooleanFormula bf2 = bfmgr.and(bf21, bf22);
    aFormula =
        new AbstractionFormula(fmv, region, bf2, bfmgr.makeTrue(), pathFormula, ImmutableSet.of());
    pf2State =
        PredicateAbstractState.mkAbstractionState(
            pathFormula, aFormula, PathCopyingPersistentTreeMap.of());
  }

  @Test
  public void testConvertToFormula1() throws Exception {
    Pair<List<String>, String> convertedFormula =
        pReqTrans.convertToFormula(ptrueState, ssaTest, null);
    assertThat(convertedFormula.getFirst()).isEmpty();
    assertFormulaIsExpected(convertedFormula, ".defci0", "true");
  }

  @Test
  public void testConvertToFormula2() throws Exception {
    Pair<List<String>, String> convertedFormula =
        pReqTrans.convertToFormula(pf1State, ssaTest, null);
    assertThat(convertedFormula.getFirst())
        .containsExactly(
            "(declare-fun |fun::var1| () Int)",
            "(declare-fun var3@1 () Int)",
            "(declare-fun var1@1 () Int)");
    assertFormulaIsExpected(
        convertedFormula, ".defci0", "(and (or (> var1@1 0) (= var3@1 0)) (< |fun::var1| 0))");
  }

  @Test
  public void testConvertRequirements1() throws Exception {
    Pair<Pair<List<String>, String>, Pair<List<String>, String>> convertedRequirements =
        pReqTrans.convertRequirements(pf1State, ImmutableList.of(), ssaTest, null, null);
    assertThat(convertedRequirements.getFirst().getFirst())
        .containsExactly(
            "(declare-fun var1 () Int)",
            "(declare-fun var3 () Int)",
            "(declare-fun |fun::var1| () Int)");
    assertFormulaIsExpected(
        convertedRequirements.getFirst(),
        "pre",
        "(and (or (> var1 0) (= var3 0)) (< |fun::var1| 0))");
    assertThat(convertedRequirements.getSecond().getFirst()).isEmpty();
    assertFormulaIsExpected(convertedRequirements.getSecond(), "post", "false");
  }

  @Test
  public void testConvertRequirements2() throws Exception {
    List<PredicateAbstractState> pAbstrStates = ImmutableList.of(ptrueState);
    Pair<Pair<List<String>, String>, Pair<List<String>, String>> convertedRequirements =
        pReqTrans.convertRequirements(pf2State, pAbstrStates, ssaTest, null, null);
    assertThat(convertedRequirements.getFirst().getFirst())
        .containsExactly(
            "(declare-fun var2 () Int)",
            "(declare-fun |fun::varB| () Int)",
            "(declare-fun |fun::varC| () Int)");
    assertFormulaIsExpected(
        convertedRequirements.getFirst(), "pre", "(and (> var2 |fun::varB|) (< |fun::varC| 0))");
    assertThat(convertedRequirements.getSecond().getFirst()).isEmpty();
    assertFormulaIsExpected(convertedRequirements.getSecond(), "post", "true");
  }

  @Test
  public void testConvertRequirements3() throws Exception {
    List<PredicateAbstractState> pAbstrStates = ImmutableList.of(pf1State, pf2State);
    Pair<Pair<List<String>, String>, Pair<List<String>, String>> convertedRequirements =
        pReqTrans.convertRequirements(ptrueState, pAbstrStates, ssaTest, null, null);
    assertThat(convertedRequirements.getFirst().getFirst()).isEmpty();
    assertFormulaIsExpected(convertedRequirements.getFirst(), "pre", "true");

    assertThat(convertedRequirements.getSecond().getFirst())
        .containsExactly(
            "(declare-fun var1@1 () Int)",
            "(declare-fun var3@1 () Int)",
            "(declare-fun |fun::var1| () Int)",
            "(declare-fun var2 () Int)",
            "(declare-fun |fun::varB@1| () Int)",
            "(declare-fun |fun::varC| () Int)");
    assertFormulaIsExpected(
        convertedRequirements.getSecond(),
        "post",
        "(or (and (or (> var1@1 0) (= var3@1 0)) (< |fun::var1| 0))(and (> var2 |fun::varB@1|) (< |fun::varC| 0)))");
  }

  private void assertFormulaIsExpected(
      Pair<List<String>, String> convertedFormula, String termName, String expectedFormula)
      throws SolverException, InterruptedException {
    String defs = Joiner.on("\n").join(convertedFormula.getFirst());
    BooleanFormula f =
        mgr.parse(defs + "\n" + convertedFormula.getSecond() + "\n(assert " + termName + ")");
    BooleanFormula expected = mgr.parse(defs + "\n(assert " + expectedFormula + ")");
    assertThatFormula(f).isEquivalentTo(expected);
  }
}
