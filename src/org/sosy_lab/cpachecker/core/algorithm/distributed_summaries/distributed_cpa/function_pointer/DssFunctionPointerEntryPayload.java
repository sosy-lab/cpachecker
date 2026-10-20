// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Splitter;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public record DssFunctionPointerEntryPayload(
    String pointerVariable, TargetKind kind, @Nullable String functionName) {

  public enum TargetKind {
    INVALID,
    NULL,
    UNKNOWN,
    NAMED
  }

  @JsonValue
  public String serialize() {
    return switch (kind) {
      case INVALID -> "I:" + pointerVariable;
      case NULL -> "0:" + pointerVariable;
      case UNKNOWN -> "U:" + pointerVariable;
      case NAMED -> "N:" + pointerVariable + ":" + functionName;
    };
  }

  @JsonCreator(mode = Mode.DELEGATING)
  public static DssFunctionPointerEntryPayload deserialize(String pSerialized) {
    List<String> parts = Splitter.on(":").limit(3).splitToList(pSerialized);
    assert parts.size() >= 2;
    return switch (parts.getFirst()) {
      case "I" -> new DssFunctionPointerEntryPayload(parts.get(1), TargetKind.INVALID, null);
      case "0" -> new DssFunctionPointerEntryPayload(parts.get(1), TargetKind.NULL, null);
      case "U" -> new DssFunctionPointerEntryPayload(parts.get(1), TargetKind.UNKNOWN, null);
      case "N" -> new DssFunctionPointerEntryPayload(parts.get(1), TargetKind.NAMED, parts.get(2));
      default -> throw new AssertionError("Unknown FunctionPointerState");
    };
  }
}
