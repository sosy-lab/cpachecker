/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.smg.graphs.object.dll;

import com.google.common.collect.ComparisonChain;
import java.util.Objects;

public class SMGDoublyLinkedListShape implements Comparable<SMGDoublyLinkedListShape> {

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