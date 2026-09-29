// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssTestUtils;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage.DssMessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysisTestBase;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class DistributedPredicateCPATest {

  /**
   * Number of leaving edges from the main function's entry node needed to reach past "int i = 0;
   * int a = 0;" in doc/examples/example.c: the CFA prepends a global-vars-init blank edge, the
   * function declaration itself, and a function-start dummy edge before the first real statement.
   */
  private static final int EDGES_PAST_DECLARATIONS = 5;

  private static PredicateCPA createPredicateCpa(CFA cfa) throws Exception {
    return createPredicateCpa(cfa, ImmutableMap.of());
  }

  private static PredicateCPA createPredicateCpa(CFA cfa, ImmutableMap<String, String> extraOptions)
      throws Exception {
    ConfigurationBuilder configBuilder =
        TestUtils.configurationForTest().loadFromFile(DssTestUtils.DSS_FORWARD_CONFIGURATION_FILE);
    extraOptions.forEach(configBuilder::setOption);
    Configuration config = configBuilder.build();
    LogManager logs = LogManager.createTestLogManager();
    ShutdownNotifier shutdown = ShutdownNotifier.createDummy();

    Specification spec =
        Specification.fromFiles(
            ImmutableList.of(Path.of("config/specification/default.spc")),
            cfa,
            config,
            logs,
            shutdown);

    return (PredicateCPA)
        PredicateCPA.factory()
            .setConfiguration(config)
            .setLogger(logs)
            .setShutdownNotifier(shutdown)
            .set(cfa, CFA.class)
            .set(spec, Specification.class)
            .set(AggregatedReachedSets.empty(), AggregatedReachedSets.class)
            .createInstance();
  }

  /** Follows the first {@code numEdges} leaving edges from the main function's entry node. */
  private static PathFormula advancePathFormula(PredicateCPA cpa, CFA cfa, int numEdges)
      throws Exception {
    CFANode node = cfa.getMainFunction();
    PathFormula pathFormula = cpa.getPathFormulaManager().makeEmptyPathFormula();
    for (int i = 0; i < numEdges; i++) {
      CFAEdge edge = node.getLeavingEdge(0);
      pathFormula = cpa.getPathFormulaManager().makeAnd(pathFormula, edge);
      node = edge.getSuccessor();
    }
    return pathFormula;
  }

  @Test
  public void boundarySeedsStayLocalAndDoNotAssumeEitherBranchOutcome() throws Exception {
    CFA cfa =
        TestCfaUtils.makeCfaFromString(
            "int main() { int x; if (x < 2) x = 0; else x = 1; if (x == 1) return 1; return 0; }");
    var branches =
        cfa.nodes().stream()
            .filter(n -> n.getNumLeavingEdges() > 0 && n.getLeavingEdge(0) instanceof AssumeEdge)
            .toList();
    assertThat(branches).hasSize(2);
    CFANode selected = branches.getFirst();
    try (PredicateCPA cpa = createPredicateCpa(cfa)) {
      var seeds =
          new DssBoundaryPredicatePrecision(
              cpa,
              ImmutableSet.of(selected),
              LogManager.createTestLogManager(),
              ShutdownNotifier.createDummy());
      var precision = seeds.getPrecision();
      assertThat(seeds.getPrecision()).isSameInstanceAs(precision);
      assertThat(precision.getLocalPredicates().keySet()).containsExactly(selected);
      assertThat(precision.getLocalPredicates().get(selected)).hasSize(1);
      assertThat(precision.getGlobalPredicates()).isEmpty();
      assertThat(precision.getFunctionPredicates()).isEmpty();
      var fmgr = cpa.getSolver().getFormulaManager();
      var atom = precision.getLocalPredicates().get(selected).iterator().next().getSymbolicAtom();
      assertThat(fmgr.uninstantiate(atom)).isEqualTo(atom);
      var initial =
          (PredicateAbstractState)
              cpa.getInitialState(selected, StateSpacePartition.getDefaultPartition());
      assertThat(initial.getAbstractionFormula().isTrue()).isTrue();
      for (var edge : selected.getLeavingEdges()) {
        var path = cpa.getPathFormulaManager().makeAnd(initial.getPathFormula(), edge);
        assertThat(cpa.getSolver().isUnsat(path.getFormula())).isFalse();
      }
    }
  }

  @Test
  public void boundarySeedingTraversesBlankEdgesButStopsAtAssignments() throws Exception {
    CFA cfa =
        TestCfaUtils.makeCfaFromString(
            "int main() { int x; if (x < 2) x = 0; else x = 1; if (x == 1) return 1; return 0; }");
    CFANode blank =
        cfa.nodes().stream()
            .filter(n -> n.getNumLeavingEdges() == 1 && n.getLeavingEdge(0) instanceof BlankEdge)
            .filter(n -> n.getLeavingEdge(0).getSuccessor().getNumLeavingEdges() == 2)
            .findFirst()
            .orElseThrow();
    CFANode assignment =
        cfa.nodes().stream()
            .filter(n -> n.getNumLeavingEdges() == 1)
            .filter(n -> n.getLeavingEdge(0).getRawStatement().equals("x = 0;"))
            .findFirst()
            .orElseThrow();
    try (PredicateCPA cpa = createPredicateCpa(cfa)) {
      var seeds =
          new DssBoundaryPredicatePrecision(
              cpa,
              ImmutableSet.of(blank, assignment),
              LogManager.createTestLogManager(),
              ShutdownNotifier.createDummy());
      assertThat(seeds.getPrecision().getLocalPredicates().keySet()).containsExactly(blank);
    }
  }

  @Test
  public void testBoundaryPrecisionRemainsAtTheSharedLocation() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromFile("doc/examples/example.c");
    try (PredicateCPA cpa = createPredicateCpa(cfa)) {
      CFANode entry = cfa.getMainFunction();
      CFANode shared = entry.getLeavingEdge(0).getSuccessor();
      CFANode exit = shared.getLeavingEdge(0).getSuccessor();
      AbstractionPredicate predicate =
          cpa.getAbstractionManager()
              .makePredicate(
                  cpa.getSolver()
                      .getFormulaManager()
                      .getBooleanFormulaManager()
                      .makeVariable("main::local"));
      PredicatePrecision sender =
          new PredicatePrecision(
              ImmutableListMultimap.of(),
              ImmutableListMultimap.of(entry, predicate, shared, predicate, exit, predicate),
              ImmutableListMultimap.of(),
              ImmutableSet.of());
      PredicatePrecision sent =
          DistributedPredicateCPA.boundaryPrecision(sender, ImmutableSet.of(entry, shared));
      PredicatePrecision received =
          DistributedPredicateCPA.boundaryPrecision(sent, ImmutableSet.of(shared, exit));
      assertThat(received.getLocalPredicates().keySet()).containsExactly(shared);
      assertThat(received.getPredicates(shared, 1)).containsExactly(predicate);
      assertThat(received.getPredicates(exit, 1)).isEmpty();
      assertThat(received.getGlobalPredicates()).isEmpty();
    }
  }

  @Test
  public void testBoundaryPrecisionPreservesConfiguredScopes() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromFile("doc/examples/example.c");
    try (PredicateCPA cpa = createPredicateCpa(cfa)) {
      CFANode entry = cfa.getMainFunction();
      CFANode shared = entry.getLeavingEdge(0).getSuccessor();
      CFANode exit = shared.getLeavingEdge(0).getSuccessor();
      var bmgr = cpa.getSolver().getFormulaManager().getBooleanFormulaManager();
      AbstractionPredicate global =
          cpa.getAbstractionManager().makePredicate(bmgr.makeVariable("g"));
      AbstractionPredicate function =
          cpa.getAbstractionManager().makePredicate(bmgr.makeVariable("main::local"));
      AbstractionPredicate otherFunction =
          cpa.getAbstractionManager().makePredicate(bmgr.makeVariable("other::local"));
      PredicatePrecision sender =
          new PredicatePrecision(
              ImmutableListMultimap.of(),
              ImmutableListMultimap.of(),
              ImmutableListMultimap.of(entry.getFunctionName(), function, "other", otherFunction),
              ImmutableSet.of(global));
      PredicatePrecision sent =
          DistributedPredicateCPA.boundaryPrecision(sender, ImmutableSet.of(entry, shared));
      PredicatePrecision received =
          DistributedPredicateCPA.boundaryPrecision(sent, ImmutableSet.of(shared, exit));
      assertThat(received.getPredicates(shared, 1)).containsExactly(global, function);
      assertThat(received.getPredicates(exit, 1)).containsExactly(global, function);
      assertThat(received.getGlobalPredicates()).containsExactly(global);
      assertThat(received.getFunctionPredicates().get(entry.getFunctionName()))
          .containsExactly(global, function);
    }
  }

  @Test
  public void testWorkerAbstractionLocationsExcludeInteriorLoopHeads() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromFunctionBody("int i = 0; while (i < 3) { i++; } return i;");
    CFANode entry = cfa.getMainFunction();
    CFANode exit = cfa.getMainFunction().getExitNode().orElseThrow();
    Configuration config =
        TestUtils.configurationForTest()
            .loadFromFile(DssTestUtils.DSS_FORWARD_CONFIGURATION_FILE)
            .setOption(
                "cpa.predicate.blk.alwaysAtGivenNodes",
                entry.getNodeNumber() + "," + exit.getNodeNumber())
            .build();
    var operator = new org.sosy_lab.cpachecker.util.predicates.BlockOperator();
    config.inject(operator);
    operator.setCFA(cfa);
    for (CFANode node : cfa.nodes()) {
      assertThat(operator.isBlockEnd(node, 100)).isEqualTo(node.equals(entry) || node.equals(exit));
    }
  }

  @Test
  public void testExactCombinationPreservesDisjunctionAndSsaContext() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromFile("doc/examples/example.c");
    PredicateCPA cpa = createPredicateCpa(cfa);
    FormulaManagerView fmgr = cpa.getSolver().getFormulaManager();
    PathFormula path = advancePathFormula(cpa, cfa, EDGES_PAST_DECLARATIONS);
    BooleanFormula left = fmgr.uninstantiate(path.getFormula());
    BooleanFormula right = fmgr.getBooleanFormulaManager().not(left);
    PredicateAbstractState first =
        PredicateAbstractState.mkAbstractionState(
            path,
            cpa.getPredicateManager().asAbstraction(left, path),
            PathCopyingPersistentTreeMap.of());
    PredicateAbstractState second =
        PredicateAbstractState.mkAbstractionState(
            path,
            cpa.getPredicateManager().asAbstraction(right, path),
            PathCopyingPersistentTreeMap.of());
    CombinePredicateStatePreconditionsOperator operator =
        new CombinePredicateStatePreconditionsOperator(cpa);
    PredicateAbstractState combined =
        (PredicateAbstractState)
            operator.combineIfPossible(ImmutableList.of(first, second)).orElseThrow();
    BooleanFormula expected = fmgr.getBooleanFormulaManager().or(left, right);
    BooleanFormula actual = combined.getAbstractionFormula().asFormula();
    assertThat(cpa.getSolver().implies(expected, actual)).isTrue();
    assertThat(cpa.getSolver().implies(actual, expected)).isTrue();
    assertThat(combined.getPathFormula().getSsa()).isEqualTo(path.getSsa());
    assertThat(
            operator.combineIfPossible(
                ImmutableList.of(
                    PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(path, first))))
        .isEmpty();
  }

  @Test
  public void testCombinedPreconditionConstrainsCounterexampleChecking() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromFile("doc/examples/example.c");
    try (PredicateCPA cpa = createPredicateCpa(cfa)) {
      FormulaManagerView fmgr = cpa.getSolver().getFormulaManager();
      var bv = fmgr.getBitvectorFormulaManager();
      BooleanFormula zero = bv.equal(bv.makeVariable(32, "main::i"), bv.makeBitvector(32, 0));
      BooleanFormula one = bv.equal(bv.makeVariable(32, "main::i"), bv.makeBitvector(32, 1));
      BooleanFormula two = bv.equal(bv.makeVariable(32, "main::i"), bv.makeBitvector(32, 2));
      PathFormula path = advancePathFormula(cpa, cfa, EDGES_PAST_DECLARATIONS);
      PathFormula laterPath =
          path.withContext(
              path.getSsa().builder().setIndex("main::i", CNumericTypes.INT, 7).build(),
              path.getPointerTargetSet());
      PredicateAbstractState first =
          PredicateAbstractState.mkAbstractionState(
              path,
              cpa.getPredicateManager().asAbstraction(zero, path),
              PathCopyingPersistentTreeMap.of());
      PredicateAbstractState second =
          PredicateAbstractState.mkAbstractionState(
              laterPath,
              cpa.getPredicateManager().asAbstraction(one, laterPath),
              PathCopyingPersistentTreeMap.of());
      var operator = new CombinePredicateStatePreconditionsOperator(cpa);
      for (var states :
          ImmutableList.of(ImmutableList.of(first), ImmutableList.of(first, second))) {
        PredicateAbstractState combined =
            (PredicateAbstractState)
                operator.combineIfPossible(ImmutableList.copyOf(states)).orElseThrow();
        PathFormula entry = combined.getPathFormula();
        BooleanFormula outsidePrecondition = fmgr.instantiate(two, entry.getSsa());
        // A path reaching i == 2 is feasible in isolation, but not from this block entry.
        assertThat(cpa.getSolver().isUnsat(outsidePrecondition)).isFalse();
        assertThat(cpa.getSolver().isUnsat(fmgr.makeAnd(entry.getFormula(), outsidePrecondition)))
            .isTrue();
        BooleanFormula expected =
            fmgr.instantiate(states.size() == 1 ? zero : fmgr.makeOr(zero, one), entry.getSsa());
        assertThat(cpa.getSolver().implies(expected, entry.getFormula())).isTrue();
        assertThat(cpa.getSolver().implies(entry.getFormula(), expected)).isTrue();
        assertThat(combined.getAbstractionFormula().asInstantiatedFormula())
            .isEqualTo(
                fmgr.instantiate(combined.getAbstractionFormula().asFormula(), entry.getSsa()));
      }
    }
  }

  @Test
  public void testPredicateSerializationOnFile() throws Exception {

    CFA cfa = TestCfaUtils.makeCfaFromFile("test/programs/dss/predicate_loop_invariant.c");
    PredicateCPA cpa = createPredicateCpa(cfa);

    DistributedConfigurableProgramAnalysisTestBase.testSerialization(cfa, cpa);
  }

  @Test
  public void testAbstractionStateSerialization() throws Exception {

    CFA cfa = TestCfaUtils.makeCfaFromFile("doc/examples/example.c");
    PredicateCPA cpa = createPredicateCpa(cfa);

    PathFormula emptyPf = cpa.getPathFormulaManager().makeEmptyPathFormula();

    FormulaManagerView formulaManagerView = cpa.getSolver().getFormulaManager();

    AbstractionFormula trueAbstraction =
        new AbstractionFormula(
            formulaManagerView,
            cpa.getAbstractionManager()
                .convertFormulaToRegion(
                    formulaManagerView.getBooleanFormulaManager().makeTrue()), // region = true
            formulaManagerView.getBooleanFormulaManager().makeTrue(), // formula = true
            formulaManagerView.getBooleanFormulaManager().makeTrue(), // instantiated formula
            emptyPf, // block formula
            ImmutableSet.of()); // id-generator set / no predicates

    AbstractState state =
        PredicateAbstractState.mkAbstractionState(
            emptyPf, trueAbstraction, PathCopyingPersistentTreeMap.of());

    // Abstraction states are only ever transmitted as postconditions in the real DSS analysis
    // (violation conditions are always built as non-abstraction states, see
    // PredicateViolationConditionOperator), and DeserializePredicateStateOperator relies on the
    // message type to tell the two apart. POST_CONDITION must be used here to match that.
    DistributedConfigurableProgramAnalysisTestBase.checkSingleStateSerialization(
        cpa, state, cfa, DssMessageType.POST_CONDITION);
  }

  @Test
  public void testAbstractionStateWithPredicateSerialization() throws Exception {

    CFA cfa = TestCfaUtils.makeCfaFromFile("doc/examples/example.c");
    PredicateCPA cpa = createPredicateCpa(cfa);

    PathFormula pathFormula = advancePathFormula(cpa, cfa, EDGES_PAST_DECLARATIONS);
    FormulaManagerView formulaManagerView = cpa.getSolver().getFormulaManager();
    AbstractionFormula abstraction =
        cpa.getPredicateManager()
            .asAbstraction(formulaManagerView.uninstantiate(pathFormula.getFormula()), pathFormula);

    // Mimics a postcondition: a non-trivial formula turned into an abstraction the same way
    // PredicatePrecisionAdjustment would do at a block boundary.
    AbstractState state =
        PredicateAbstractState.mkAbstractionState(
            pathFormula, abstraction, PathCopyingPersistentTreeMap.of());

    DistributedConfigurableProgramAnalysisTestBase.checkSingleStateSerialization(
        cpa, state, cfa, DssMessageType.POST_CONDITION);
  }

  @Test
  public void testNonAbstractionStateSerialization() throws Exception {

    CFA cfa = TestCfaUtils.makeCfaFromFile("doc/examples/example.c");
    PredicateCPA cpa = createPredicateCpa(cfa);

    PathFormula pathFormula = advancePathFormula(cpa, cfa, EDGES_PAST_DECLARATIONS);
    PredicateAbstractState initialState =
        (PredicateAbstractState)
            cpa.getInitialState(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());

    // Mimics a violation condition: PredicateViolationConditionOperator always builds a
    // non-abstraction state from a path formula accumulated along an ARG path.
    AbstractState state =
        PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(pathFormula, initialState);

    DistributedConfigurableProgramAnalysisTestBase.checkSingleStateSerialization(
        cpa, state, cfa, DssMessageType.VIOLATION_CONDITION);
  }

  @Test
  public void testPrecisionSerialization() throws Exception {

    CFA cfa = TestCfaUtils.makeCfaFromFile("doc/examples/example.c");
    PredicateCPA cpa = createPredicateCpa(cfa);

    PathFormula pathFormula = advancePathFormula(cpa, cfa, EDGES_PAST_DECLARATIONS);
    FormulaManagerView formulaManagerView = cpa.getSolver().getFormulaManager();
    ImmutableList<BooleanFormula> atoms =
        ImmutableList.copyOf(
            formulaManagerView
                .getBooleanFormulaManager()
                .toConjunctionArgs(pathFormula.getFormula(), true));
    AbstractionPredicate globalPredicate =
        cpa.getAbstractionManager()
            .makePredicate(formulaManagerView.uninstantiate(atoms.getFirst()));
    AbstractionPredicate localPredicate =
        cpa.getAbstractionManager().makePredicate(formulaManagerView.uninstantiate(atoms.get(1)));

    CFANode mainEntry = cfa.getMainFunction();

    // Exercises all four categories that SerializePredicatePrecisionOperator serializes
    // separately: global, per-function, per-location, and per-location-instance predicates.
    PredicatePrecision precision =
        new PredicatePrecision(
            ImmutableListMultimap.of(
                new PredicatePrecision.LocationInstance(mainEntry, 0), localPredicate),
            ImmutableListMultimap.of(mainEntry, localPredicate),
            ImmutableListMultimap.of(mainEntry.getFunctionName(), globalPredicate),
            ImmutableSet.of(globalPredicate));

    DistributedConfigurableProgramAnalysisTestBase.checkPrecisionSerialization(
        cpa, precision, cfa, DssMessageType.POST_CONDITION);
  }

  @Test
  public void testEmptyPrecisionSerialization() throws Exception {

    CFA cfa = TestCfaUtils.makeCfaFromFile("doc/examples/example.c");
    PredicateCPA cpa = createPredicateCpa(cfa);

    // The empty precision is what every block starts out with, so it is round-tripped constantly.
    // It used to come back holding the "false" predicate as a global one: the serializer writes the
    // global key unconditionally, joining no predicates into "", and the deserializer split "" into
    // a single blank element that AbstractionManager.parsePredicate maps to false. Since
    // PredicatePrecision propagates globals into every function and location key, that single
    // spurious predicate poisoned the entire precision.
    DistributedConfigurableProgramAnalysisTestBase.checkPrecisionSerialization(
        cpa, PredicatePrecision.empty(), cfa, DssMessageType.POST_CONDITION);
  }

  @Test
  public void testPrecisionWithoutGlobalPredicatesSerialization() throws Exception {

    CFA cfa = TestCfaUtils.makeCfaFromFile("doc/examples/example.c");
    PredicateCPA cpa = createPredicateCpa(cfa);

    PathFormula pathFormula = advancePathFormula(cpa, cfa, EDGES_PAST_DECLARATIONS);
    FormulaManagerView formulaManagerView = cpa.getSolver().getFormulaManager();
    AbstractionPredicate localPredicate =
        cpa.getAbstractionManager()
            .makePredicate(
                formulaManagerView.uninstantiate(
                    formulaManagerView
                        .getBooleanFormulaManager()
                        .toConjunctionArgs(pathFormula.getFormula(), true)
                        .iterator()
                        .next()));

    // A non-empty precision that nevertheless has no global predicates: same empty-global-key
    // problem as above, but here the spurious "false" would also be mixed in with real predicates.
    PredicatePrecision precision =
        new PredicatePrecision(
            ImmutableListMultimap.of(),
            ImmutableListMultimap.of(cfa.getMainFunction(), localPredicate),
            ImmutableListMultimap.of(),
            ImmutableSet.of());

    DistributedConfigurableProgramAnalysisTestBase.checkPrecisionSerialization(
        cpa, precision, cfa, DssMessageType.POST_CONDITION);
  }

  @Test
  public void testAbstractionStateWithPointerTargetSetSerialization() throws Exception {

    // The DSS default config disables the SMT aliasing memory model
    // (cpa.predicate.handlePointerAliasing=false in dss-block-analysis.properties), so the
    // PointerTargetSet stays empty in every other test in this class. Enable it explicitly here to
    // exercise the PointerTargetSet's own (de)serialization, which goes through a raw
    // Java-serialization blob (see SerializePredicateStateOperator.PTS_KEY) and is therefore the
    // most fragile part of the wire format.
    // Kept so when pointerAliasing is supported in the future, we do not get any surprises
    CFA cfa = TestCfaUtils.makeCfaFromFile("test/programs/dss/predicate_pointer_write.c");
    PredicateCPA cpa =
        createPredicateCpa(cfa, ImmutableMap.of("cpa.predicate.handlePointerAliasing", "true"));

    PathFormula pathFormula = advancePathFormula(cpa, cfa, 6); // up to and including "*p = 1;"
    assertThat(pathFormula.getPointerTargetSet().getBases()).isNotEmpty();

    FormulaManagerView formulaManagerView = cpa.getSolver().getFormulaManager();
    AbstractionFormula abstraction =
        cpa.getPredicateManager()
            .asAbstraction(formulaManagerView.uninstantiate(pathFormula.getFormula()), pathFormula);

    AbstractState state =
        PredicateAbstractState.mkAbstractionState(
            pathFormula, abstraction, PathCopyingPersistentTreeMap.of());

    DistributedConfigurableProgramAnalysisTestBase.checkSingleStateSerialization(
        cpa, state, cfa, DssMessageType.POST_CONDITION);
  }
}
