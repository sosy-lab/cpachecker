// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;

@JsonPropertyOrder({
  DssMessageFormat.SOUND_KEY,
  DssMessageFormat.PRECISE_KEY,
  DssMessageFormat.PROPERTY_KEY,
})
public record DssStatusPayload(
    @JsonProperty(DssMessageFormat.SOUND_KEY) boolean sound,
    @JsonProperty(DssMessageFormat.PRECISE_KEY) boolean precise,
    @JsonProperty(DssMessageFormat.PROPERTY_KEY) boolean propertyChecked) {

  @JsonCreator
  public DssStatusPayload {}

  public static DssStatusPayload fromAlgorithmStatus(AlgorithmStatus pStatus) {
    return new DssStatusPayload(
        pStatus.isSound(), pStatus.isPrecise(), pStatus.wasPropertyChecked());
  }

  public AlgorithmStatus toAlgorithmStatus() {
    if (!propertyChecked) {
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }
    if (sound && precise) {
      return AlgorithmStatus.SOUND_AND_PRECISE;
    }
    if (sound) {
      return AlgorithmStatus.SOUND_AND_IMPRECISE;
    }
    if (precise) {
      return AlgorithmStatus.UNSOUND_AND_PRECISE;
    }
    return AlgorithmStatus.UNSOUND_AND_IMPRECISE;
  }

  ImmutableMap<String, String> asLegacyContent() {
    return ContentBuilder.builder()
        .pushLevel(DssMessageFormat.STATUS_KEY)
        .put(DssMessageFormat.SOUND_KEY, Boolean.toString(sound))
        .put(DssMessageFormat.PRECISE_KEY, Boolean.toString(precise))
        .put(DssMessageFormat.PROPERTY_KEY, Boolean.toString(propertyChecked))
        .build();
  }
}
