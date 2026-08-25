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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.TestUtil;
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
        TestUtils.configurationForTest().loadFromFile(TestUtil.DSS_FORWARD_CONFIGURATION_FILE);
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
  public void testPredicateSerializationOnFile() throws Exception {

    // TODO find program which tests something interesting!!
    CFA cfa = TestUtil.buildTestCFA("doc/examples/example.c");
    PredicateCPA cpa = createPredicateCpa(cfa);

    DistributedConfigurableProgramAnalysisTestBase.testSerialization(cfa, cpa);
  }

  @Test
  public void testAbstractionStateSerialization() throws Exception {

    CFA cfa = TestUtil.buildTestCFA("doc/examples/example.c");
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

    CFA cfa = TestUtil.buildTestCFA("doc/examples/example.c");
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

    CFA cfa = TestUtil.buildTestCFA("doc/examples/example.c");
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

    CFA cfa = TestUtil.buildTestCFA("doc/examples/example.c");
    PredicateCPA cpa = createPredicateCpa(cfa);

    PathFormula pathFormula = advancePathFormula(cpa, cfa, EDGES_PAST_DECLARATIONS);
    FormulaManagerView formulaManagerView = cpa.getSolver().getFormulaManager();
    ImmutableList<BooleanFormula> atoms =
        ImmutableList.copyOf(
            formulaManagerView
                .getBooleanFormulaManager()
                .toConjunctionArgs(pathFormula.getFormula(), true));
    AbstractionPredicate globalPredicate =
        cpa.getAbstractionManager().makePredicate(formulaManagerView.uninstantiate(atoms.get(0)));
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
  public void testAbstractionStateWithPointerTargetSetSerialization() throws Exception {

    // The DSS default config disables the SMT aliasing memory model
    // (cpa.predicate.handlePointerAliasing=false in dss-block-analysis.properties), so the
    // PointerTargetSet stays empty in every other test in this class. Enable it explicitly here to
    // exercise the PointerTargetSet's own (de)serialization, which goes through a raw
    // Java-serialization blob (see SerializePredicateStateOperator.PTS_KEY) and is therefore the
    // most fragile part of the wire format.
    // Kept so when pointerAliasing is supported in the future, we do not get any surprises
    CFA cfa = TestUtil.buildTestCFA("test/programs/dss/predicate_pointer_write.c");
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
