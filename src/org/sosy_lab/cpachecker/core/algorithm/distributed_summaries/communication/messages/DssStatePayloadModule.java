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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack.DssCallstackStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DssCompositePrecisionPayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DssCompositeStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa.DssBlockStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer.DssFunctionPointerStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.location.DssLocationStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.DssPredicatePrecisionPayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.DssPredicateStatePayload;

public final class DssStatePayloadModule extends SimpleModule {
  public static final String NO_PRECISION = "noPrecision";
  @Serial private static final long serialVersionUID = -4429488243946317868L;

  public DssStatePayloadModule() {
    super("DssPayloadModule");

    registerSubtypes(
        new NamedType(
            DssLocationStatePayload.class, DssComponentKey.LOCATION_STATE.getSerializedName()),
        new NamedType(DssBlockStatePayload.class, DssComponentKey.BLOCK_STATE.getSerializedName()),
        new NamedType(
            DssCallstackStatePayload.class, DssComponentKey.CALLSTACK_STATE.getSerializedName()),
        new NamedType(
            DssFunctionPointerStatePayload.class,
            DssComponentKey.FUNCTION_POINTER_STATE.getSerializedName()),
        new NamedType(
            DssPredicateStatePayload.class, DssComponentKey.PREDICATE_STATE.getSerializedName()),
        new NamedType(
            DssCompositeStatePayload.class, DssComponentKey.COMPOSITE_STATE.getSerializedName()),
        new NamedType(
            DssPredicatePrecisionPayload.class,
            DssComponentKey.PREDICATE_PRECISION.getSerializedName()),
        new NamedType(
            DssCompositePrecisionPayload.class,
            DssComponentKey.COMPOSITE_PRECISION.getSerializedName()),
        new NamedType(DssNoPrecisionPayload.class, NO_PRECISION));
  }
}
