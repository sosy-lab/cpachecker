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
package org.sosy_lab.cpachecker.cpa.smg.objects.dls;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

public class SMGDoublyLinkedListShape implements Comparable<SMGDoublyLinkedListShape> {

  private final int hfo;
  private final int pfo;
  private final int nfo;

  public int getHfo() {
    return hfo;
  }

  public int getPfo() {
    return pfo;
  }

  public int getNfo() {
    return nfo;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + hfo;
    result = prime * result + nfo;
    result = prime * result + pfo;
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
    if (getClass() != obj.getClass()) {
      return false;
    }
    SMGDoublyLinkedListShape other = (SMGDoublyLinkedListShape) obj;
    if (hfo != other.hfo) {
      return false;
    }
    if (nfo != other.nfo) {
      return false;
    }
    if (pfo != other.pfo) {
      return false;
    }
    return true;
  }

  public SMGDoublyLinkedListShape(int pHfo, int pPfo, int pNfo) {
    super();
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
        .compare(nfo, other.nfo, Ordering.<Integer> natural().nullsFirst())
        .compare(pfo, other.pfo, Ordering.<Integer> natural().nullsFirst())
        .compare(hfo, other.hfo, Ordering.<Integer> natural().nullsFirst())
        .result();
  }
}