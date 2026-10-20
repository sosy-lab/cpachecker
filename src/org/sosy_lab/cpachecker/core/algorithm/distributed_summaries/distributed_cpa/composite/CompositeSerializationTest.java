// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageWithStates;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStateAndPrecisionPayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class CompositeSerializationTest {

  private static final String CONFIGURATION_FILE =
      "config/distributed-summary-synthesis/dss-block-analysis.properties";

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void initialCompositeStateRoundTrip() throws Exception {
    Configuration configuration =
        TestUtils.configurationForTestWithOutput(tempFolder)
            .loadFromFile(CONFIGURATION_FILE)
            .setOption("language", "C")
            .setOption("cfa.export", "false")
            .setOption("cfa.exportPerFunction", "false")
            .setOption(
                "CompositeCPA.cpas",
                "cpa.block.BlockCPA, cpa.location.LocationCPA, "
                    + "cpa.callstack.DssCallstackCPA, cpa.functionpointer.FunctionPointerCPA, "
                    + "cpa.predicate.PredicateCPA")
            .build();
    LogManager logger = LogManager.createTestLogManager();
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();
    CFA cfa =
        TestCfaUtils.makeCfaFromString(
            ""
                + "void target(void) {}\n"
                + "int main(void) {\n"
                + "  void (*functionPointer)(void) = target;\n"
                + "  functionPointer();\n"
                + "  return 0;\n"
                + "}\n",
            Map.entry("language", "C"));

    ConfigurableProgramAnalysis cpa =
        new CPABuilder(
                configuration,
                logger,
                shutdownNotifier,
                new ReachedSetFactory(configuration, logger))
            .buildCPAs(cfa, Specification.alwaysSatisfied(), AggregatedReachedSets.empty());
    CompositeCPA compositeCpa = CPAs.retrieveCPA(cpa, CompositeCPA.class);
    CFANode initialLocation = cfa.getMainFunction();
    CFANode finalLocation = cfa.getMainFunction().getExitNode().orElseThrow();
    BlockNode blockNode =
        new BlockNode(
            "test-block",
            initialLocation,
            finalLocation,
            ImmutableSet.copyOf(cfa.nodes()),
            ImmutableSet.copyOf(cfa.edges()),
            ImmutableSet.of(),
            ImmutableSet.of());
    DssAnalysisOptions options = new DssAnalysisOptions(configuration);
    DistributedConfigurableProgramAnalysis distributedCpa =
        DssFactory.distribute(
            compositeCpa,
            blockNode,
            cfa,
            configuration,
            options,
            new DssMessageFactory(options),
            logger,
            shutdownNotifier);

    CompositeState state =
        (CompositeState)
            distributedCpa.getInitialState(
                initialLocation, StateSpacePartition.getDefaultPartition());
    Precision precision =
        compositeCpa.getInitialPrecision(
            initialLocation, StateSpacePartition.getDefaultPartition());
    DssStatePayload statePayload = distributedCpa.getSerializeOperator().serialize(state);
    var precisionPayload =
        distributedCpa.getSerializePrecisionOperator().serializePrecision(precision);

    assertThat(statePayload).isInstanceOf(DssCompositeStatePayload.class);
    DssCompositeStatePayload compositeStatePayload = (DssCompositeStatePayload) statePayload;
    assertThat(compositeStatePayload.components().keySet())
        .containsExactly(
            DssComponentKey.BLOCK_STATE.getSerializedName(),
            DssComponentKey.LOCATION_STATE.getSerializedName(),
            DssComponentKey.CALLSTACK_STATE.getSerializedName(),
            DssComponentKey.FUNCTION_POINTER_STATE.getSerializedName(),
            DssComponentKey.PREDICATE_STATE.getSerializedName());

    DssMessageWithStates message =
        new DssPostConditionMessage(
            "test",
            AlgorithmStatus.SOUND_AND_PRECISE,
            ImmutableList.of(new DssStateAndPrecisionPayload(statePayload, precisionPayload)));

    CompositeState deserializedState =
        (CompositeState)
            distributedCpa
                .getDeserializeOperator()
                .deserialize(message.getStates().getFirst().state(), message.getType());
    Precision deserializedPrecision =
        distributedCpa
            .getDeserializePrecisionOperator()
            .deserializePrecision(message.getStates().getFirst().precision(), message.getType());

    assertThat(deserializedState.getWrappedStates()).hasSize(5);
    assertThat(deserializedPrecision)
        .isInstanceOf(org.sosy_lab.cpachecker.cpa.composite.CompositePrecision.class);
  }
}
