// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.Serial;

/**
 * Jackson subtype registry for message payloads.
 *
 * <p>Every concrete {@link DssMessagePayload} must be registered here with the stable serialized
 * type name used in the JSON {@code type} property.
 */
public class DssMessagePayloadModule extends SimpleModule {

  @Serial private static final long serialVersionUID = 433268480834651101L;

  public DssMessagePayloadModule() {
    super("DssMessagePayloadModule");

    registerSubtypes(
        new NamedType(DssPostConditionPayload.class, DssMessagePayload.POST_CONDITION),
        new NamedType(DssViolationConditionPayload.class, DssMessagePayload.VIOLATION_CONDITION),
        new NamedType(DssResultPayload.class, DssMessagePayload.RESULT),
        new NamedType(DssExceptionPayload.class, DssMessagePayload.EXCEPTION),
        new NamedType(DssViolationWitnessPayload.class, DssMessagePayload.VIOLATION_WITNESS),
        new NamedType(DssCorrectnessWitnessPayload.class, DssMessagePayload.CORRECTNESS_WITNESS));
  }
}
