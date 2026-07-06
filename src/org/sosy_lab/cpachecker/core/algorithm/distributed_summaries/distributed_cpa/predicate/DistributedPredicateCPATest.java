// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.TestUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysisTestBase;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class DistributedPredicateCPATest {

  @Test
  public void testPredicateSerializationOnFile() throws Exception {

    // TODO find program which tests something interesting!!
    CFA cfa = TestUtil.buildTestCFA("doc/examples/example.c");

    Configuration config =
        TestDataTools.configurationForTest()
            .loadFromFile(TestUtil.DSS_FORWARD_CONFIGURATION_FILE)
            .build();
    LogManager logs = LogManager.createTestLogManager();
    ShutdownNotifier shutdown = ShutdownNotifier.createDummy();

    Specification spec =
        Specification.fromFiles(
            ImmutableList.of(Path.of("config/specification/default.spc")),
            cfa,
            config,
            logs,
            shutdown);

    AggregatedReachedSets set = AggregatedReachedSets.empty();

    PredicateCPA cpa =
        (PredicateCPA)
            PredicateCPA.factory()
                .setConfiguration(config)
                .setLogger(logs)
                .setShutdownNotifier(shutdown)
                .set(cfa, CFA.class)
                .set(spec, Specification.class)
                .set(set, AggregatedReachedSets.class)
                .createInstance();

    DistributedConfigurableProgramAnalysisTestBase.testSerialization(cfa, cpa);
  }

  @Test
  public void testAbstractState() throws Exception {

    // TODO find program which tests something interesting!!
    CFA cfa = TestUtil.buildTestCFA("doc/examples/example.c");

    Configuration config =
        TestDataTools.configurationForTest()
            .loadFromFile(TestUtil.DSS_FORWARD_CONFIGURATION_FILE)
            .build();
    LogManager logs = LogManager.createTestLogManager();
    ShutdownNotifier shutdown = ShutdownNotifier.createDummy();

    Specification spec =
        Specification.fromFiles(
            ImmutableList.of(Path.of("config/specification/default.spc")),
            cfa,
            config,
            logs,
            shutdown);

    AggregatedReachedSets set = AggregatedReachedSets.empty();

    PredicateCPA cpa =
        (PredicateCPA)
            PredicateCPA.factory()
                .setConfiguration(config)
                .setLogger(logs)
                .setShutdownNotifier(shutdown)
                .set(cfa, CFA.class)
                .set(spec, Specification.class)
                .set(set, AggregatedReachedSets.class)
                .createInstance();

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
            java.util.Collections.emptySet()); // id-generator set / no predicates

    AbstractState state =
        PredicateAbstractState.mkAbstractionState(
            emptyPf, trueAbstraction, PathCopyingPersistentTreeMap.of());

    DistributedConfigurableProgramAnalysisTestBase.checkSingleStateSerialization(cpa, state, cfa);
  }
}
