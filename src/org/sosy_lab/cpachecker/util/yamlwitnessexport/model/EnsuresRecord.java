// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;

@Immutable
public record EnsuresRecord(ImmutableList<String> clauses) {

  @JsonValue
  public ImmutableList<String> getClauses() {
    return clauses;
  }
}
