// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer;

import org.checkerframework.checker.nullness.qual.Nullable;

public record DssFunctionPointerEntry(
    String pointerVariable, TargetKind kind, @Nullable String functionName) {
  public enum TargetKind {
    INVALID,
    NULL,
    UNKNOWN,
    NAMED
  }

  @Override
  public String toString() {
    return switch (kind) {
      case INVALID -> "I:" + pointerVariable;
      case NULL -> "0:" + pointerVariable;
      case UNKNOWN -> "U:" + pointerVariable;
      case NAMED -> "N:" + pointerVariable + ":" + functionName;
    };
  }
}
