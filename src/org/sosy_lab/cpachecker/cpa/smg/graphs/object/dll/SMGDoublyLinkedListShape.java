// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.dll;

import com.google.common.collect.ComparisonChain;
import java.util.Objects;

public final class SMGDoublyLinkedListShape implements Comparable<SMGDoublyLinkedListShape> {

  /** head field offset */
  private final long hfo;
  /** prev field offset */
  private final long pfo;
  /** next field offset */
  private final long nfo;

  public long getHfo() {
    return hfo;
  }

  public long getPfo() {
    return pfo;
  }

  public long getNfo() {
    return nfo;
  }

  @Override
  public int hashCode() {
    return Objects.hash(hfo, nfo, pfo);
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
    SMGDoublyLinkedListShape other = (SMGDoublyLinkedListShape) obj;
    return hfo == other.hfo && nfo == other.nfo && pfo == other.pfo;
  }

  public SMGDoublyLinkedListShape(long pHfo, long pPfo, long pNfo) {
    hfo = pHfo;
    pfo = pPfo;
    nfo = pNfo;
  }

  @Override
  public String toString() {
    return "SMGDoublyLinkedListShape [hfo=" + hfo + ", pfo=" + pfo + ", nfo=" + nfo + "]";
  }

  @Override
  public int compareTo(SMGDoublyLinkedListShape other) {
    return ComparisonChain.start()
        .compare(nfo, other.nfo)
        .compare(pfo, other.pfo)
        .compare(hfo, other.hfo)
        .result();
  }
}
