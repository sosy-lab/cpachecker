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
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;

@JsonPropertyOrder({
  DssMessageKeys.SOUND,
  DssMessageKeys.PRECISE,
  DssMessageKeys.PROPERTY,
})
public record DssStatusPayload(
    @JsonProperty(value = DssMessageKeys.SOUND, required = true) boolean sound,
    @JsonProperty(value = DssMessageKeys.PRECISE, required = true) boolean precise,
    @JsonProperty(value = DssMessageKeys.PROPERTY, required = true) boolean propertyChecked) {

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
}
