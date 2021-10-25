// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;

@Immutable
public class InvariantStoreEntryLoopInvariant {
  @JsonProperty("string")
  private final String string;

  @JsonProperty("type")
  private final String type;

  @JsonProperty("format")
  private final String format;

  public InvariantStoreEntryLoopInvariant(
      @JsonProperty("string") String string,
      @JsonProperty("type") String type,
      @JsonProperty("format") String format) {
    this.string = string;
    this.type = type;
    this.format = format;
  }

  public String getString() {
    return string;
  }

  public String getType() {
    return type;
  }

  public String getFormat() {
    return format;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof InvariantStoreEntryLoopInvariant)) {
      return false;
    }
    InvariantStoreEntryLoopInvariant invariantStoreEntryLoopInvariant =
        (InvariantStoreEntryLoopInvariant) o;
    return string.equals(invariantStoreEntryLoopInvariant.string)
        && type.equals(invariantStoreEntryLoopInvariant.type)
        && format.equals(invariantStoreEntryLoopInvariant.format);
  }

  @Override
  public int hashCode() {
    int hashCode = string.hashCode();
    hashCode = 31 * hashCode + type.hashCode();
    hashCode = 31 * hashCode + format.hashCode();
    return hashCode;
  }

  @Override
  public String toString() {
    return "InvariantStoreEntryLoopInvariant{"
        + " string='"
        + getString()
        + "'"
        + ", type='"
        + getType()
        + "'"
        + ", format='"
        + getFormat()
        + "'"
        + "}";
  }
}
