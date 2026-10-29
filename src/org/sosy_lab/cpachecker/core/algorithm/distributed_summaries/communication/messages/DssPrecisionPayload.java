// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * Jackson payload for a distributed-CPA precision.
 *
 * <p>A concrete precision payload must contain all information required by its deserialization
 * operator. If a CPA has no meaningful precision, use the shared no-precision sentinel {@link
 * DssNoPrecisionPayload#INSTANCE} instead of {@code null}.
 *
 * <p>Concrete precision payloads must be registered in {@link DssStatePayloadModule}, which is the
 * subtype registry for both state and precision payloads.
 */
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = DssMessagePayload.TYPE)
public interface DssPrecisionPayload {
  DssComponentKey getComponentKey();
}
