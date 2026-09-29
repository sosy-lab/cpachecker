// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class DssPredicateBoundaryRefinementTest {
  private PredicateCPA cpa;
  private FormulaManagerView fmgr;
  private IntegerFormulaManagerView integers;
  private DssPredicateBoundaryRefinement refinement;
  private CFANode location;
  private CFANode otherLocation;
  private AbstractState locationState;
  private Precision emptyPrecision;

  @Before
  public void setUp() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromString("void main() {}");
    location = cfa.getMainFunction();
    otherLocation = cfa.getMainFunction().getExitNode().orElseThrow();
    var config =
        TestUtils.configurationForTest()
            .setOption("solver.solver", "SMTINTERPOL")
            .setOption("cpa.predicate.encodeBitvectorAs", "INTEGER")
            .setOption("cpa.predicate.encodeFloatAs", "RATIONAL")
            .setOption("cpa.predicate.abstraction.computation", "CARTESIAN")
            .setOption(
                "cpa.predicate.blk.alwaysAtGivenNodes", Integer.toString(location.getNodeNumber()))
            .build();
    cpa =
        (PredicateCPA)
            PredicateCPA.factory()
                .setConfiguration(config)
                .setLogger(LogManager.createTestLogManager())
                .setShutdownNotifier(ShutdownNotifier.createDummy())
                .set(cfa, CFA.class)
                .set(Specification.alwaysSatisfied(), Specification.class)
                .set(AggregatedReachedSets.empty(), AggregatedReachedSets.class)
                .createInstance();
    fmgr = cpa.getSolver().getFormulaManager();
    integers = fmgr.getIntegerFormulaManager();
    refinement = new DssPredicateBoundaryRefinement(cpa, location, config);
    locationState =
        LocationCPA.create(cfa, config)
            .getInitialState(location, StateSpacePartition.getDefaultPartition());
    emptyPrecision =
        new CompositePrecision(
            ImmutableList.of(SingletonPrecision.getInstance(), PredicatePrecision.empty()));
  }

  @After
  public void tearDown() throws Exception {
    cpa.close();
  }

  private BooleanFormula value(String variable, int index, int value) {
    return integers.equal(
        integers.makeVariable(variable + "@" + index), integers.makeNumber(value));
  }

  private PredicateAbstractState predicate(BooleanFormula formula) throws Exception {
    PathFormula path =
        cpa.getPathFormulaManager()
            .makeEmptyPathFormulaWithContext(
                SSAMap.emptySSAMap()
                    .builder()
                    .setIndex("x", CNumericTypes.INT, 2)
                    .setIndex("y", CNumericTypes.INT, 2)
                    .setIndex("main::x", CNumericTypes.INT, 2)
                    .build(),
                PointerTargetSet.emptyPointerTargetSet())
            .withFormula(formula);
    return PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
        path,
        (PredicateAbstractState)
            cpa.getInitialState(location, StateSpacePartition.getDefaultPartition()));
  }

  private ARGState exit(BooleanFormula formula) throws Exception {
    return new ARGState(
        new CompositeState(ImmutableList.of(locationState, predicate(formula))), null);
  }

  @Test
  public void refutedConditionLearnsOneCompoundPredicateOnlyAtTheExit() throws Exception {
    var b = fmgr.getBooleanFormulaManager();
    BooleanFormula reachable = b.and(value("x", 2, 0), value("y", 2, 0));
    BooleanFormula violation = b.not(reachable);
    ARGState exit = exit(reachable);
    var condition = predicate(violation);
    Precision refined =
        refinement.refine(
            ImmutableList.of(exit), ImmutableList.of(condition, condition), emptyPrecision);
    var predicates = Precisions.extractPrecisionByType(refined, PredicatePrecision.class);
    assertThat(predicates.getPredicates(location, 1)).hasSize(1);
    assertThat(predicates.getPredicates(otherLocation, 1)).isEmpty();
    var summaries =
        refinement.abstractSummaries(
            ImmutableList.of(new StateAndPrecision(exit, emptyPrecision)), refined);
    assertThat(summaries).hasSize(1);
    var summary =
        AbstractStates.extractStateByType(
            summaries.getFirst().state(), PredicateAbstractState.class);
    BooleanFormula abstraction = summary.getAbstractionFormula().asInstantiatedFormula();
    assertThat(cpa.getSolver().isUnsat(b.and(abstraction, violation))).isTrue();
    assertThat(cpa.getSolver().isUnsat(b.and(reachable, b.not(abstraction)))).isTrue();
    assertThat(
            AbstractStates.extractStateByType(exit, PredicateAbstractState.class)
                .isAbstractionState())
        .isFalse();
  }

  @Test
  public void scopeSharingMakesGlobalPredicatesAvailableAtOtherBoundaries() throws Exception {
    refinement =
        new DssPredicateBoundaryRefinement(
            cpa,
            location,
            TestUtils.configurationForTest()
                .setOption("cpa.predicate.precision.sharing", "SCOPE")
                .build());
    var b = fmgr.getBooleanFormulaManager();
    var reachable = value("x", 2, 0);
    var refined =
        refinement.refine(
            ImmutableList.of(exit(reachable)),
            ImmutableList.of(predicate(b.not(reachable))),
            emptyPrecision);
    var predicates = Precisions.extractPrecisionByType(refined, PredicatePrecision.class);
    assertThat(predicates.getGlobalPredicates()).hasSize(1);
    assertThat(predicates.getLocalPredicates()).isEmpty();
    assertThat(predicates.getPredicates(otherLocation, 1))
        .containsExactlyElementsIn(predicates.getPredicates(location, 1));
  }

  @Test
  public void scopeSharingKeepsLocalPredicatesAtTheirSharedLocation() throws Exception {
    refinement =
        new DssPredicateBoundaryRefinement(
            cpa,
            location,
            TestUtils.configurationForTest()
                .setOption("cpa.predicate.precision.sharing", "SCOPE")
                .build());
    var b = fmgr.getBooleanFormulaManager();
    var reachable = value("main::x", 2, 0);
    var refined =
        refinement.refine(
            ImmutableList.of(exit(reachable)),
            ImmutableList.of(predicate(b.not(reachable))),
            emptyPrecision);
    var predicates = Precisions.extractPrecisionByType(refined, PredicatePrecision.class);
    assertThat(predicates.getGlobalPredicates()).isEmpty();
    assertThat(predicates.getPredicates(location, 1)).hasSize(1);
    assertThat(predicates.getPredicates(otherLocation, 1)).isEmpty();
  }

  @Test
  public void locationInstanceSharingUsesTheOutgoingAbstractionInstance() throws Exception {
    refinement =
        new DssPredicateBoundaryRefinement(
            cpa,
            location,
            TestUtils.configurationForTest()
                .setOption("cpa.predicate.precision.sharing", "LOCATION_INSTANCE")
                .build());
    var b = fmgr.getBooleanFormulaManager();
    var reachable = value("x", 2, 0);
    var refined =
        refinement.refine(
            ImmutableList.of(exit(reachable)),
            ImmutableList.of(predicate(b.not(reachable))),
            emptyPrecision);
    var predicates = Precisions.extractPrecisionByType(refined, PredicatePrecision.class);
    assertThat(predicates.getPredicates(location, 1)).hasSize(1);
    assertThat(predicates.getPredicates(location, 2)).isEmpty();
    assertThat(predicates.getPredicates(otherLocation, 1)).isEmpty();
  }

  @Test
  public void feasibleConditionDoesNotInventASeparatingPredicate() throws Exception {
    ARGState exit = exit(value("x", 2, 1));
    Precision refined =
        refinement.refine(
            ImmutableList.of(exit), ImmutableList.of(predicate(value("x", 2, 1))), emptyPrecision);
    assertThat(
            Precisions.extractPrecisionByType(refined, PredicatePrecision.class)
                .getPredicates(location, 1))
        .isEmpty();
    assertThat(
            refinement.abstractSummaries(
                ImmutableList.of(new StateAndPrecision(exit, emptyPrecision)), refined))
        .hasSize(1);
  }

  @Test
  public void infeasibleExactExitDoesNotBecomeAnOutgoingSummary() throws Exception {
    var b = fmgr.getBooleanFormulaManager();
    ARGState exit = exit(b.and(value("x", 2, 0), value("x", 2, 1)));
    Precision refined =
        refinement.refine(
            ImmutableList.of(exit), ImmutableList.of(predicate(b.makeTrue())), emptyPrecision);
    assertThat(
            refinement.abstractSummaries(
                ImmutableList.of(new StateAndPrecision(exit, emptyPrecision)), refined))
        .isEmpty();
  }
}
