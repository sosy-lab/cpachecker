// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "entry_type", visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = LoopInvariantEntry.class, name = "loop_invariant"),
  @JsonSubTypes.Type(
      value = LoopInvariantCertificateEntry.class,
      name = "loop_invariant_certificate")
})
public abstract class AbstractEntry {

  @JsonProperty("entry_type")
  protected final String entryType;

  public AbstractEntry(@JsonProperty("entry_type") String entry_type) {
    this.entryType = entry_type;
  }

  public String getEntryType() {
    return entryType;
  }

  @Override
  public String toString() {
    return "Entry{entry_type=" + entryType + "}";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((entryType == null) ? 0 : entryType.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof AbstractEntry)) {
      return false;
    }
    AbstractEntry other = (AbstractEntry) obj;
    if (entryType == null) {
      if (other.entryType != null) {
        return false;
      }
    } else if (!entryType.equals(other.entryType)) {
      return false;
    }
    return true;
  }
}
