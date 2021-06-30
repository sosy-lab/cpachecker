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
public class InvariantStoreEntryMetadata {
  public final String formatVerison;

  public InvariantStoreEntryMetadata() {
    this.formatVerison = "0.1";
  }

  public String getFormatVerison() {
    return formatVerison;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof InvariantStoreEntryMetadata)) {
      return false;
    }
    InvariantStoreEntryMetadata other = (InvariantStoreEntryMetadata) o;
    return formatVerison.equals(other.formatVerison);
  }

  @Override
  public int hashCode() {
    return formatVerison.hashCode();
  }
}
