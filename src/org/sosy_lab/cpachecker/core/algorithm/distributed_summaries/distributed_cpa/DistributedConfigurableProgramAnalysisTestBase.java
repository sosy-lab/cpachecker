// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa;

import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssTestUtils;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage.DssMessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageCodec;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageJsonUtils;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageWithStates;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads.DssNoPrecisionPayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads.DssPrecisionPayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads.DssStateAndPrecisionPayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads.DssStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.SingleBlockDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.location.DssLocationStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class DistributedConfigurableProgramAnalysisTestBase {

  record State(CFANode node, AbstractState absState) {}

  private record Harness(
      DssMessageFactory messageFactory, DistributedConfigurableProgramAnalysis dcpa) {}

  /** Maximum number of edges that the test follows */
  private static final int MAX_DEPTH = 40;

  private static Harness createHarness(ConfigurableProgramAnalysis cpa, CFA cfa)
      throws InvalidConfigurationException, IOException, InterruptedException, CPAException {
    Configuration config =
        TestUtils.configurationForTest()
            .loadFromFile(DssTestUtils.DSS_FORWARD_CONFIGURATION_FILE)
            .build();
    DssAnalysisOptions options = new DssAnalysisOptions(config);
    DssMessageFactory messageFactory = new DssMessageFactory(options);

    BlockNode node = new SingleBlockDecomposition().decompose(cfa).getRoot();

    DistributedConfigurableProgramAnalysis dcpa =
        DssFactory.distribute(
            cpa,
            node,
            cfa,
            config,
            options,
            messageFactory,
            LogManager.createTestLogManager(),
            ShutdownNotifier.createDummy());
    return new Harness(messageFactory, dcpa);
  }

  // Routes the message through the same JSON file round trip that DSS uses to export and import
  // messages to catch issues that only show up during that
  private static DssMessageWithStates wrapInMessage(
      DssMessageFactory messageFactory,
      DssStatePayload statePayload,
      DssPrecisionPayload precisionPayload,
      DssMessageType messageType)
      throws IOException {
    ImmutableList<DssStateAndPrecisionPayload> content =
        ImmutableList.of(new DssStateAndPrecisionPayload(statePayload, precisionPayload));
    DssMessage message =
        switch (messageType) {
          case POST_CONDITION ->
              messageFactory.createDssPostConditionMessage(
                  "test", AlgorithmStatus.NO_PROPERTY_CHECKED, content);
          case VIOLATION_CONDITION ->
              messageFactory.createViolationConditionMessage(
                  "test", AlgorithmStatus.NO_PROPERTY_CHECKED, content);
          default ->
              throw new AssertionError(
                  "Serialization test does not support message type: " + messageType);
        };
    Path tempFile = Files.createTempFile("dss-message-test", ".json");
    try {
      DssMessageJsonUtils.write(message.asJsonPayload(), tempFile);
      return (DssMessageWithStates) DssMessageCodec.fromJson(tempFile);
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  public static void testSerialization(String programPath, ConfigurableProgramAnalysis cpa)
      throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromFile(programPath);
    testSerialization(cfa, cpa);
  }

  public static void testSerialization(CFA cfa, ConfigurableProgramAnalysis cpa) throws Exception {

    Harness harness = createHarness(cpa, cfa);
    DssMessageFactory messageFactory = harness.messageFactory();
    DistributedConfigurableProgramAnalysis dcpa = harness.dcpa();

    CFANode initialNode = cfa.getMainFunction();
    AbstractState initialState =
        dcpa.getInitialState(initialNode, StateSpacePartition.getDefaultPartition());

    Precision prec =
        dcpa.getInitialPrecision(initialNode, StateSpacePartition.getDefaultPartition());

    TransferRelation tr = dcpa.getTransferRelation();

    List<State> states = ImmutableList.of(new State(initialNode, initialState));

    for (int i = 0; i < MAX_DEPTH; i++) {

      if (states.isEmpty()) {
        break;
      }

      List<State> newStates = new ArrayList<>();

      for (State currState : states) {

        for (CFAEdge edge : currState.node.getAllLeavingEdges()) {
          for (AbstractState newState :
              tr.getAbstractSuccessorsForEdge(currState.absState, prec, edge)) {

            checkSingleStateSerialization(
                messageFactory, dcpa, newState, DssMessageType.VIOLATION_CONDITION);
            newStates.add(new State(edge.getSuccessor(), newState));
          }
        }
      }

      states = newStates;
    }
  }

  public static void checkSingleStateSerialization(
      ConfigurableProgramAnalysis cpa, AbstractState state, CFA cfa)
      throws InvalidConfigurationException, IOException, InterruptedException, CPAException {
    checkSingleStateSerialization(cpa, state, cfa, DssMessageType.VIOLATION_CONDITION);
  }

  // Some DCPAs (e.g. the predicate DCPA) don't encode everything about a state in the serialized
  // content: they also rely on the DssMessageType of the enclosing message to decide how to
  // deserialize it (e.g. whether the result is an abstraction state). -> allow tests to specify
  public static void checkSingleStateSerialization(
      ConfigurableProgramAnalysis cpa, AbstractState state, CFA cfa, DssMessageType messageType)
      throws InvalidConfigurationException, IOException, InterruptedException, CPAException {
    Harness harness = createHarness(cpa, cfa);
    checkSingleStateSerialization(harness.messageFactory(), harness.dcpa(), state, messageType);
  }

  private static void checkSingleStateSerialization(
      DssMessageFactory messageFactory,
      DistributedConfigurableProgramAnalysis dcpa,
      AbstractState state,
      DssMessageType messageType)
      throws InterruptedException, CPAException, IOException {

    DssStatePayload content = dcpa.getSerializeOperator().serialize(state);
    DssMessageWithStates message =
        wrapInMessage(messageFactory, content, DssNoPrecisionPayload.INSTANCE, messageType);
    AbstractState afterSerialization =
        dcpa.getDeserializeOperator()
            .deserialize(message.getStates().getFirst().state(), message.getType());

    assertWithMessage(
            "For state %s, the operators for dcpa %s are wrong: \n serialized to %s,"
                + "\n but deserialization %s not reported as covering the original",
            state, dcpa.getClass(), message.asJsonPayload(), afterSerialization)
        .that(dcpa.getCoverageOperator().isSubsumed(state, afterSerialization))
        .isTrue();
  }

  // Precision must stay equal instead of using a coverage check
  public static void checkPrecisionSerialization(
      ConfigurableProgramAnalysis cpa, Precision precision, CFA cfa, DssMessageType messageType)
      throws InvalidConfigurationException, IOException, InterruptedException, CPAException {
    Harness harness = createHarness(cpa, cfa);
    DistributedConfigurableProgramAnalysis dcpa = harness.dcpa();

    DssPrecisionPayload content =
        dcpa.getSerializePrecisionOperator().serializePrecision(precision);
    // The precision needs to be transported with some state, which is ignored afterward.
    DssMessageWithStates message =
        wrapInMessage(
            harness.messageFactory(), new DssLocationStatePayload(0), content, messageType);
    Precision afterSerialization =
        dcpa.getDeserializePrecisionOperator()
            .deserializePrecision(message.getStates().getFirst().precision(), message.getType());

    assertWithMessage(
            "Precision %s for dcpa %s was serialized to %s but deserialized to %s",
            precision, dcpa.getClass(), message.asJsonPayload(), afterSerialization)
        .that(afterSerialization)
        .isEqualTo(precision);
  }
}
