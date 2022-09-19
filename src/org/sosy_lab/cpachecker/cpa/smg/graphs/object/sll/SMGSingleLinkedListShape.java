// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll;

import com.google.common.collect.ComparisonChain;
import java.util.Objects;

public final class SMGSingleLinkedListShape implements Comparable<SMGSingleLinkedListShape> {

  /** head field offset */
  private final long hfo;
  /** next field offset */
  private final long nfo;

  public SMGSingleLinkedListShape(long pHfo, long pNfo) {
    hfo = pHfo;
    nfo = pNfo;
  }

  @Override
  public int hashCode() {
    return Objects.hash(hfo, nfo);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SMGSingleLinkedListShape other = (SMGSingleLinkedListShape) obj;
    return hfo == other.hfo && nfo == other.nfo;
  }

  @Override
  public String toString() {
    return "SMGSingleLinkedListShape [hfo=" + hfo + ", nfo=" + nfo + "]";
  }

  public long getHfo() {
    return hfo;
  }

  public long getNfo() {
    return nfo;
  }

  @Override
  public int compareTo(SMGSingleLinkedListShape other) {
    return ComparisonChain.start().compare(nfo, other.nfo).compare(hfo, other.hfo).result();
  }
}
