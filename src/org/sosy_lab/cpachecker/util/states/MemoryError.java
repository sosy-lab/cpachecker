// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.states;

import java.util.Arrays;

public enum MemoryError {

  INVALID_READ("has-invalid-reads"),
  INVALID_WRITE("has-invalid-writes"),
  INVALID_FREE("has-invalid-frees"),
  MEMORY_LEAK("has-leaks");

  private final String property;

  private MemoryError(String pProperty) {
    property = pProperty;
  }

  public String getDescription() {
    return property;
  }

  public static MemoryError getErrorForProperty(String pProperty) {
    return Arrays.stream(MemoryError.values())
        .filter(e -> e.getDescription().equals(pProperty))
        .findFirst()
        .orElseThrow(
            () -> new IllegalStateException(String.format("No such property: %s", pProperty)));
  }
}
