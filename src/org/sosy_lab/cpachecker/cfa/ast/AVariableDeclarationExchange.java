// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;

public record AVariableDeclarationExchange(
    @JsonProperty("name") @NonNull String name,
    @JsonProperty("simpleType") @NonNull CBasicType simpleType) {

  public AVariableDeclarationExchange(
      @JsonProperty("name") String name,
      @JsonProperty("simpleType") @NonNull CBasicType simpleType) {
    checkNotNull(name);
    checkNotNull(simpleType);
    this.name = name;
    this.simpleType = simpleType;
  }
}
