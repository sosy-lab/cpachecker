// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.location;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.Mockito;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageKeys;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;

public class DssLocationStatePayloadTest {
  private DssMessageFactory defaultFactory() throws InvalidConfigurationException {
    return new DssMessageFactory(new DssAnalysisOptions(Configuration.defaultConfiguration()));
  }

  @Test
  public void locationPayloadConvertsToContentMap() {
    DssLocationStatePayload payload = new DssLocationStatePayload(42);

    assertThat(payload.asContentMap())
        .containsExactly(
            DssComponentKey.serializedNameFor(LocationState.class) + "." + DssMessageKeys.STATE,
            "42");
  }

  @Test
  public void locationPayloadParsesFromContentMap() {
    ImmutableMap<String, String> content =
        ImmutableMap.of(
            DssComponentKey.serializedNameFor(LocationState.class) + "." + DssMessageKeys.STATE,
            "42");
    DssLocationStatePayload payload = DssLocationStatePayload.fromContentMap(content);

    assertThat(payload.nodeId()).isEqualTo(42);
  }

  @Test
  public void locationPayloadRoundTrips() {
    DssLocationStatePayload original = new DssLocationStatePayload(42);
    DssLocationStatePayload parsed =
        DssLocationStatePayload.fromContentMap(original.asContentMap());

    assertThat(parsed).isEqualTo(original);
  }

  @Test
  public void deserializerCanReadLocationPayload()
      throws InvalidConfigurationException, InterruptedException {
    CFANode node = CFANode.newDummyCFANode();
    LocationState expectedState = Mockito.mock(LocationState.class);
    LocationStateFactory locationStateFactory = Mockito.mock(LocationStateFactory.class);
    Mockito.when(locationStateFactory.getState(node)).thenReturn(expectedState);

    ImmutableList<ImmutableMap<String, String>> states =
        ImmutableList.of(new DssLocationStatePayload(42).asContentMap());
    DssPostConditionMessage postConditionMessage =
        defaultFactory()
            .createDssPostConditionMessage("B1", AlgorithmStatus.SOUND_AND_PRECISE, states);
    DeserializeLocationState deserializer =
        new DeserializeLocationState(locationStateFactory, ImmutableMap.of(42, node));

    AbstractState parsed = deserializer.deserialize(postConditionMessage, 0);

    assertThat(parsed).isSameInstanceAs(expectedState);
  }
}
