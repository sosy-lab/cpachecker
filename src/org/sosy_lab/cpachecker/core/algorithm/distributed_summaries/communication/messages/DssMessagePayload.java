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
 * Jackson payload representing the wire format of a {@link DssMessage}.
 *
 * <p>Each concrete message type has its own payload record. The record defines the JSON structure,
 * explicit property names, required and optional fields, and the conversion between the wire
 * representation and the Java message object.
 *
 * <p>When adding a payload subtype, register it in {@link DssMessagePayloadModule} with an
 * explicit serialized type name. Java class and field names must not determine the wire format.
 */
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = DssMessagePayload.TYPE)
public sealed interface DssMessagePayload
    permits DssPostConditionPayload,
        DssViolationConditionPayload,
        DssResultPayload,
        DssExceptionPayload,
        DssViolationWitnessPayload,
        DssCorrectnessWitnessPayload {
  String TYPE = "type";

  DssHeaderPayload header();

  DssMessagePayload withNewHeader(DssHeaderPayload pNewHeader);

  default DssMessagePayload withoutTimestamp() {
    return withNewHeader(header().withoutTimestamp());
  }
}
