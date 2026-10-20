// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.Serial;

public class DssMessagePayloadModule extends SimpleModule {

  @Serial private static final long serialVersionUID = 433268480834651101L;

  public DssMessagePayloadModule() {
    super("DssMessagePayloadModule");

    registerSubtypes(
        new NamedType(DssPostConditionPayload.class, "postCondition"),
        new NamedType(DssViolationConditionPayload.class, "violationCondition"),
        new NamedType(DssResultPayload.class, "result"),
        new NamedType(DssExceptionPayload.class, "exception"),
        new NamedType(DssViolationWitnessPayload.class, "violationWitness"),
        new NamedType(DssCorrectnessWitnessPayload.class, "correctnessWitness"));
  }
}
