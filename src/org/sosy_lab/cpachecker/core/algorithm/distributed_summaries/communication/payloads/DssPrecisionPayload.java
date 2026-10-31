// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DssCompositePrecisionPayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.DssPredicatePrecisionPayload;

/**
 * Jackson payload for a distributed-CPA precision.
 *
 * <p>A concrete precision payload must contain all information required by its deserialization
 * operator. If a CPA has no meaningful precision, use the shared no-precision sentinel {@link
 * DssNoPrecisionPayload#INSTANCE} instead of {@code null}.
 *
 * <p>Concrete precision payloads must be registered in {@link DssStatePayloadModule}, which is the
 * subtype registry for both state and precision payloads. Also register the payload to the
 * {@code @JsonSubTypes} annotation below.
 */
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = DssMessagePayload.TYPE)
@JsonSubTypes({
  @JsonSubTypes.Type(
      value = DssPredicatePrecisionPayload.class,
      name = DssComponentKey.SerializedNames.PREDICATE_PRECISION_KEY),
  @JsonSubTypes.Type(
      value = DssCompositePrecisionPayload.class,
      name = DssComponentKey.SerializedNames.COMPOSITE_PRECISION_KEY),
  @JsonSubTypes.Type(
      value = DssNoPrecisionPayload.class,
      name = DssComponentKey.SerializedNames.NO_PRECISION_KEY)
})
public interface DssPrecisionPayload {
  DssComponentKey getComponentKey();
}
