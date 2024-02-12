// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Objects;

@Immutable
public final class InformationRecord extends AbstractInformationRecord {
  public InformationRecord(
      @JsonProperty("value") String string,
      @JsonProperty("type") String pType,
      @JsonProperty("format") String pFormat) {
    super(string, pType, pFormat);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return o instanceof InformationRecord invariantStoreEntryLoopInvariant
        && Objects.equals(value, invariantStoreEntryLoopInvariant.value)
        && Objects.equals(type, invariantStoreEntryLoopInvariant.type)
        && Objects.equals(format, invariantStoreEntryLoopInvariant.format);
  }

  @Override
  public int hashCode() {
    int hashCode = value.hashCode();
    hashCode = 31 * hashCode + (type != null ? type.hashCode() : 0);
    hashCode = 31 * hashCode + (format != null ? format.hashCode() : 0);
    return hashCode;
  }

  @Override
  public String toString() {
    return "InformationRecord{"
        + " string='"
        + getValue()
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
