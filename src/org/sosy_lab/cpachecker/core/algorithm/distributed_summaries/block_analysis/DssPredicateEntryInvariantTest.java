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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class DssPredicateEntryInvariantTest {
  private CFA cfa;
  private PredicateCPA cpa;
  private FormulaManagerView fmgr;
  private LocationCPA locations;

  @Before
  public void setUp() throws Exception {
    cfa = TestCfaUtils.makeCfaFromString("void main() { int x; x = 1; }");
    var config =
        TestUtils.configurationForTest()
            .setOption("solver.solver", "SMTINTERPOL")
            .setOption("cpa.predicate.encodeBitvectorAs", "INTEGER")
            .setOption("cpa.predicate.encodeFloatAs", "RATIONAL")
            .setOption("cpa.predicate.handlePointerAliasing", "false")
            .setOption("cpa.predicate.ignoreIrrelevantVariables", "false")
            .setOption(
                "cpa.predicate.blk.alwaysAtGivenNodes",
                Integer.toString(cfa.getMainFunction().getNodeNumber()))
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
    locations = LocationCPA.create(cfa, config);
  }

  @After
  public void tearDown() throws Exception {
    cpa.close();
  }

  private BooleanFormula value(String name, int value) {
    var integers = fmgr.getIntegerFormulaManager();
    return integers.equal(integers.makeVariable(name), integers.makeNumber(value));
  }

  private DssPredicateEntryInvariant invariant(BooleanFormula formula) {
    return new DssPredicateEntryInvariant(
        fmgr.dumpFormula(formula).toString(), ImmutableMap.of("main::x", CNumericTypes.INT));
  }

  private ARGState entry(SSAMap ssa, BooleanFormula constraint) throws Exception {
    var path =
        cpa.getPathFormulaManager()
            .makeEmptyPathFormulaWithContext(ssa, PointerTargetSet.emptyPointerTargetSet())
            .withFormula(constraint);
    var initial =
        (PredicateAbstractState)
            cpa.getInitialState(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
    var predicate =
        PredicateAbstractState.mkAbstractionState(
            path,
            cpa.getPredicateManager().asAbstraction(fmgr.uninstantiate(constraint), path),
            initial.getAbstractionLocationsOnPath());
    return new ARGState(
        new CompositeState(
            ImmutableList.of(
                locations.getInitialState(
                    cfa.getMainFunction(), StateSpacePartition.getDefaultPartition()),
                predicate)),
        null);
  }

  private PredicateAbstractState predicate(ARGState state) {
    return AbstractStates.extractStateByType(state, PredicateAbstractState.class);
  }

  @Test
  public void freshEntryUsesFirstSsaIndexWithoutMutatingTheInput() throws Exception {
    var old = entry(SSAMap.emptySSAMap(), fmgr.getBooleanFormulaManager().makeTrue());
    var strengthened = invariant(value("main::x", 0)).strengthen(old, cpa);
    var state = predicate(strengthened);
    assertThat(state.getPathFormula().getSsa().getIndex("main::x")).isEqualTo(1);
    assertThat(state.getPathFormula().getFormula()).isEqualTo(value("main::x@1", 0));
    assertThat(state.getAbstractionFormula().asInstantiatedFormula())
        .isEqualTo(value("main::x@1", 0));
    assertThat(predicate(old).getPathFormula().getSsa().containsVariable("main::x")).isFalse();
    assertThat(((CompositeState) strengthened.getWrappedState()).getWrappedStates().getFirst())
        .isSameInstanceAs(((CompositeState) old.getWrappedState()).getWrappedStates().getFirst());
  }

  @Test
  public void incomingIndicesAndConstraintsArePreserved() throws Exception {
    var ssa = SSAMap.emptySSAMap().builder().setIndex("main::x", CNumericTypes.INT, 7).build();
    var old = entry(ssa, value("main::x@7", 1));
    var strengthened = invariant(value("main::x", 0)).strengthen(old, cpa);
    assertThat(predicate(strengthened).getPathFormula().getSsa().getIndex("main::x")).isEqualTo(7);
    assertThat(cpa.getSolver().isUnsat(predicate(strengthened).getPathFormula().getFormula()))
        .isTrue();
    assertThat(cpa.getSolver().isUnsat(predicate(old).getPathFormula().getFormula())).isFalse();
  }

  @Test
  public void assignmentDoesNotTreatTheEntryFactAsAnExitFact() throws Exception {
    var old = entry(SSAMap.emptySSAMap(), fmgr.getBooleanFormulaManager().makeTrue());
    var strengthened = invariant(value("main::x", 0)).strengthen(old, cpa);
    var assignment =
        cfa.nodes().stream()
            .flatMap(n -> n.getLeavingEdges().stream())
            .filter(CStatementEdge.class::isInstance)
            .findFirst()
            .orElseThrow();
    var after =
        cpa.getPathFormulaManager().makeAnd(predicate(strengthened).getPathFormula(), assignment);
    int output = after.getSsa().getIndex("main::x");
    assertThat(output).isEqualTo(2);
    assertThat(cpa.getSolver().isUnsat(after.getFormula())).isFalse();
    assertThat(
            cpa.getSolver()
                .isUnsat(
                    fmgr.getBooleanFormulaManager()
                        .and(after.getFormula(), value("main::x@" + output, 0))))
        .isTrue();
    assertThat(
            cpa.getSolver()
                .isUnsat(
                    fmgr.getBooleanFormulaManager()
                        .and(after.getFormula(), value("main::x@" + output, 1))))
        .isFalse();
  }

  @Test
  public void anInputWithIntermediateSsaValuesIsLeftUnchanged() throws Exception {
    var ssa = SSAMap.emptySSAMap().builder().setIndex("main::x", CNumericTypes.INT, 2).build();
    var initial = predicate(entry(ssa, fmgr.getBooleanFormulaManager().makeTrue()));
    var path =
        initial
            .getPathFormula()
            .withFormula(
                fmgr.getBooleanFormulaManager().and(value("main::x@1", 0), value("main::x@2", 1)));
    var state =
        PredicateAbstractState.mkAbstractionState(
            path, initial.getAbstractionFormula(), initial.getAbstractionLocationsOnPath());
    var old = new ARGState(new CompositeState(ImmutableList.of(state)), null);
    assertThat(invariant(value("main::x", 1)).strengthen(old, cpa)).isSameInstanceAs(old);
    assertThat(cpa.getSolver().isUnsat(path.getFormula())).isFalse();
  }

  @Test
  public void provedUnreachabilityMakesTheEntryInfeasible() throws Exception {
    var old = entry(SSAMap.emptySSAMap(), fmgr.getBooleanFormulaManager().makeTrue());
    var strengthened = invariant(fmgr.getBooleanFormulaManager().makeFalse()).strengthen(old, cpa);
    assertThat(predicate(strengthened).getAbstractionFormula().isFalse()).isTrue();
  }

  @Test
  public void perRunBlockCopiesRetainContextWithoutChangingTheCachedBlock() {
    var node = cfa.getMainFunction();
    var block =
        new BlockNode(
            "block",
            node,
            node,
            ImmutableSet.of(node),
            ImmutableSet.of(),
            ImmutableSet.of(),
            ImmutableSet.of());
    var fact = invariant(value("main::x", 0));
    var stack = new CallstackState(null, node.getFunctionName(), node);
    var copy = block.withEntryInvariant(fact).withKnownEntryCallstack(stack);
    assertThat(block.getEntryInvariant()).isEmpty();
    assertThat(block.getKnownEntryCallstack()).isEmpty();
    assertThat(copy.getEntryInvariant()).hasValue(fact);
    assertThat(copy.getKnownEntryCallstack()).hasValue(stack);
    assertThat(
            block.withKnownEntryCallstack(stack).withEntryInvariant(fact).getKnownEntryCallstack())
        .hasValue(stack);
  }
}
