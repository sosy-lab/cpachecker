// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssTestUtils;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysisTestBase;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.DssCallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.DssCallstackState;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class DistributedCallstackCPATest {

  @Test
  public void testCallStackSerializationOnFile() throws Exception {
    Configuration config =
        TestUtils.configurationForTest().loadFromFile(DssTestUtils.DSS_CONFIGURATION_FILE).build();
    ConfigurableProgramAnalysis cpa =
        DssCallstackCPA.factory()
            .setConfiguration(config)
            .setLogger(LogManager.createTestLogManager())
            .setShutdownNotifier(ShutdownNotifier.createDummy())
            .createInstance();

    DistributedConfigurableProgramAnalysisTestBase.testSerialization(
        "test/programs/dss/function_graph_DAG.c", cpa);
  }

  @Test
  public void resetAndSpeculationRestoreTheCompleteInferredContext() throws Exception {
    var cfa = TestCfaUtils.makeCfaFromString("void f() {} int main() { f(); }");
    CFANode entry = cfa.getFunctionHead("f");
    CallstackState parent = new CallstackState(null, "main", cfa.getMainFunction());
    CallstackState known = new CallstackState(parent, "f", cfa.getMainFunction());
    BlockNode block =
        new BlockNode(
                "f",
                entry,
                entry,
                ImmutableSet.of(entry),
                ImmutableSet.of(),
                ImmutableSet.of("caller"),
                ImmutableSet.of())
            .withKnownEntryCallstack(known);
    var cpa =
        new DssCallstackCPA(
            Configuration.defaultConfiguration(), LogManager.createTestLogManager());
    var distributed = new DistributedCallstackCPA(cpa, block, cfa, true, ImmutableBiMap.of());
    distributed.setIgnoreTransfer(true);
    var speculative =
        (DssCallstackState)
            distributed.getInitialState(entry, StateSpacePartition.getDefaultPartition());
    assertThat(speculative.canBeTopState()).isFalse();
    assertThat(speculative.getWrappedState()).isSameInstanceAs(known);
    var reset =
        (DssCallstackState)
            distributed.reset(new DssCallstackState(new CallstackState(null, "f", entry), false));
    assertThat(reset.getWrappedState()).isSameInstanceAs(known);
    assertThat(reset.getPreviousState()).isSameInstanceAs(parent);
  }
}
