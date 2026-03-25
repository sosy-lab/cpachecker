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
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.TestUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.SingleBlockDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class DistributedConfigurableProgramAnalysisTestUtil {

  record State(CFANode node, AbstractState absState) {}

  /** Maximum number of edges that the test follows */
  private static int MAX_DEPTH = 40;

  public static void testSerialization(
      String program_path, ConfigurableProgramAnalysis cpa, Precision prec) throws Exception {
    CFA cfa = TestUtil.buildTestCFA(program_path);
    testSerialization(cfa, cpa, prec);
  }

  public static void testSerialization(CFA cfa, ConfigurableProgramAnalysis cpa, Precision prec)
      throws Exception {

    Configuration config =
        TestDataTools.configurationForTest().loadFromFile(TestUtil.DSS_CONFIGURATION_FILE).build();
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

    CFANode initialNode = cfa.getMainFunction();
    AbstractState initialState =
        dcpa.getInitialState(initialNode, StateSpacePartition.getDefaultPartition());

    TransferRelation tr = dcpa.getTransferRelation();
    SerializeOperator serial = dcpa.getSerializeOperator();
    DeserializeOperator deserial = dcpa.getDeserializeOperator();
    CoverageOperator coverage = dcpa.getCoverageOperator();

    List<State> states = ImmutableList.of(new State(initialNode, initialState));

    for (int i = 0; i < MAX_DEPTH; i++) {

      if (states.isEmpty()) {
        break;
      }

      List<State> newStates = new ArrayList<>();

      for (State curr_state : states) {

        for (CFAEdge edge : curr_state.node.getAllLeavingEdges()) {
          for (AbstractState new_state : tr.getAbstractSuccessorsForEdge(curr_state.absState, prec, edge)) {


              DssMessage message = stateToDssMessage(new_state, serial, messageFactory);
              AbstractState afterSerialization = deserial.deserialize(message);

            assertWithMessage(
                    "For state %s, the operators for dcpa %s are wrong: \n serialized to %s,"
                        + "\n but deserialization %s not reported as covering the original",
                    new_state, dcpa.getClass(), message.asJson(), afterSerialization)
                .that(coverage.isSubsumed(new_state, afterSerialization))
                .isTrue();
            newStates.add(new State(edge.getSuccessor(), new_state));
          }
        }
      }

      states = newStates;
    }
  }

  private static DssMessage stateToDssMessage(AbstractState state, SerializeOperator serial, DssMessageFactory factory) {

    ImmutableMap<String, String> content = serial.serialize(state);

    return factory.createViolationConditionMessage(
        "test", AlgorithmStatus.NO_PROPERTY_CHECKED, false, content);
  }
}
