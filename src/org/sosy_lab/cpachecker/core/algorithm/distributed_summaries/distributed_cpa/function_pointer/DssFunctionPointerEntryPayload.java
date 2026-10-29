// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.checkerframework.checker.nullness.qual.Nullable;

@JsonPropertyOrder({
  DssFunctionPointerEntryPayload.POINTER_VARIABLE,
  DssFunctionPointerEntryPayload.KIND,
  DssFunctionPointerEntryPayload.FUNCTION_NAME
})
public record DssFunctionPointerEntryPayload(
    @JsonProperty(POINTER_VARIABLE) String pointerVariable,
    @JsonProperty(KIND) TargetKind kind,
    @JsonProperty(FUNCTION_NAME) @Nullable String functionName) {
  public static final String POINTER_VARIABLE = "pointerVariable";
  public static final String KIND = "kind";
  public static final String FUNCTION_NAME = "functionName";

  public enum TargetKind {
    INVALID,
    NULL,
    UNKNOWN,
    NAMED
  }
}
