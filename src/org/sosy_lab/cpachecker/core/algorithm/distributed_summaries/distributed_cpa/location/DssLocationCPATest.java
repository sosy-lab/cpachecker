// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.location;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage.DssMessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysisTestBase;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.location.CachedLocationStateProvider;
import org.sosy_lab.cpachecker.cpa.location.DssLocationStateFactory;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;
import org.sosy_lab.cpachecker.cpa.location.LocationTransferRelationBackwards;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class DssLocationCPATest {

  @Test
  public void lazyStatesPreserveTransfersAndCallFollowing() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromString("void f(void) {} int main(void) { f(); return 0; }");
    for (boolean followCalls : new boolean[] {true, false}) {
      Configuration config =
          TestUtils.configurationForTest()
              .setOption("cpa.location.followFunctionCalls", Boolean.toString(followCalls))
              .build();
      LocationCPA eager = LocationCPA.create(cfa, config);
      DssLocationCPA lazy =
          (DssLocationCPA)
              DssLocationCPA.factory()
                  .setConfiguration(config)
                  .set(cfa, CFA.class)
                  .createInstance();
      assertThat(lazy.getStateProvider()).isInstanceOf(DssLocationStateFactory.class);
      LocationTransferRelationBackwards backwards =
          new LocationTransferRelationBackwards(lazy.getStateProvider());
      for (CFANode node : cfa.nodes()) {
        LocationState state = lazy.getInitialState(node, StateSpacePartition.getDefaultPartition());
        LocationState original = eager.getStateFactory().getState(node);
        assertThat(state).isSameInstanceAs(lazy.getStateProvider().getState(node));
        assertThat(state.getOutgoingEdges()).containsExactlyElementsIn(original.getOutgoingEdges());
        assertThat(state.getIncomingEdges()).containsExactlyElementsIn(original.getIncomingEdges());
        for (CFAEdge edge : node.getAllLeavingEdges()) {
          LocationState successor = lazy.getStateProvider().getState(edge.getSuccessor());
          assertThat(
                  lazy.getTransferRelation()
                      .getAbstractSuccessorsForEdge(state, SingletonPrecision.getInstance(), edge))
              .containsExactly(successor);
          assertThat(
                  backwards.getAbstractSuccessorsForEdge(
                      successor, SingletonPrecision.getInstance(), edge))
              .containsExactly(state);
        }
      }
    }
  }

  @Test
  public void serializesLazyStatesInBothMessageDirections() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromString("void f(void) {} int main(void) { f(); return 0; }");
    DssLocationCPA cpa =
        (DssLocationCPA)
            DssLocationCPA.factory()
                .setConfiguration(TestUtils.configurationForTest().build())
                .set(cfa, CFA.class)
                .createInstance();
    for (CFANode node : cfa.nodes()) {
      LocationState state = cpa.getStateProvider().getState(node);
      for (DssMessageType type :
          new DssMessageType[] {
            DssMessageType.POST_CONDITION, DssMessageType.VIOLATION_CONDITION,
          }) {
        DistributedConfigurableProgramAnalysisTestBase.checkSingleStateSerialization(
            cpa, state, cfa, type);
      }
    }
  }

  @Test
  public void rejectsMissingNodesWithinOriginalNumberRange() throws Exception {
    CFA original = TestCfaUtils.makeCfaFromString("int main(void) { return 0; }");
    MutableCFA cfa =
        MutableCFA.copyOf(
            original, TestUtils.configurationForTest().build(), LogManager.createTestLogManager());
    // Copying gives the CFA new IDs, leaving the original main node below its maximum ID.
    Configuration config = TestUtils.configurationForTest().build();
    LocationStateFactory eager = new LocationStateFactory(cfa, AnalysisDirection.FORWARD, config);
    CachedLocationStateProvider lazy = new DssLocationStateFactory(cfa, config);
    CFANode missing = original.getMainFunction();
    assertThrows(NullPointerException.class, () -> eager.getState(missing));
    assertThrows(NullPointerException.class, () -> lazy.getState(missing));
  }

  @Test
  public void supportsNodesAddedAfterConstruction() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromString("int main(void) { return 0; }");
    Configuration config = TestUtils.configurationForTest().build();
    LocationStateFactory eager = new LocationStateFactory(cfa, AnalysisDirection.FORWARD, config);
    CachedLocationStateProvider lazy = new DssLocationStateFactory(cfa, config);
    CFANode added = new CFANode(cfa.getMainFunction().getFunction());
    assertThat(lazy.getState(added).getLocationNode()).isSameInstanceAs(added);
    assertThat(eager.getState(added).getLocationNode()).isSameInstanceAs(added);
  }
}
