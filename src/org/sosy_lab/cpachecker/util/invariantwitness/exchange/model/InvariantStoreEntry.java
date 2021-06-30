// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.model;

import com.google.errorprone.annotations.Immutable;

@Immutable
public class InvariantStoreEntry {
  public final String entryType;
  public final InvariantStoreEntryMetadata metadata;
  public final InvariantStoreEntryLocation location;
  public final InvariantStoreEntryLoopInvariant loopInvariant;

  public InvariantStoreEntry(
      String entryType,
      InvariantStoreEntryMetadata metadata,
      InvariantStoreEntryLocation location,
      InvariantStoreEntryLoopInvariant loopInvariant) {
    this.entryType = entryType;
    this.metadata = metadata;
    this.location = location;
    this.loopInvariant = loopInvariant;
  }

  public String getEntryType() {
    return entryType;
  }

  public InvariantStoreEntryMetadata getMetadata() {
    return metadata;
  }

  public InvariantStoreEntryLocation getLocation() {
    return location;
  }

  public InvariantStoreEntryLoopInvariant getLoopInvariant() {
    return loopInvariant;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof InvariantStoreEntry)) {
      return false;
    }
    InvariantStoreEntry other = (InvariantStoreEntry) o;
    return entryType.equals(other.entryType)
        && metadata.equals(other.metadata)
        && location.equals(other.location)
        && loopInvariant.equals(other.loopInvariant);
  }

  @Override
  public int hashCode() {
    int hashCode = entryType.hashCode();
    hashCode = 31 * hashCode + metadata.hashCode();
    hashCode = 31 * hashCode + location.hashCode();
    hashCode = 31 * hashCode + loopInvariant.hashCode();
    return hashCode;
  }
}
