// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;

@JsonPropertyOrder({
  DssStatusPayload.SOUND,
  DssStatusPayload.PRECISE,
  DssStatusPayload.PROPERTY,
})
public record DssStatusPayload(
    @JsonProperty(value = SOUND, required = true) boolean sound,
    @JsonProperty(value = PRECISE, required = true) boolean precise,
    @JsonProperty(value = PROPERTY, required = true) boolean propertyChecked) {

  public static final String STATUS = "status";
  public static final String PRECISE = "precise";
  public static final String PROPERTY = "property";
  public static final String SOUND = "sound";

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
