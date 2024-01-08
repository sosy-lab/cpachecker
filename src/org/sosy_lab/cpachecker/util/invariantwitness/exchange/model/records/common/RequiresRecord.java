// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;

@Immutable
public class RequiresRecord {

  private final ImmutableList<String> clauses;

  public RequiresRecord(ImmutableList<String> pClauses) {
    clauses = pClauses;
  }

  @JsonValue
  public ImmutableList<String> getClauses() {
    return clauses;
  }
}
