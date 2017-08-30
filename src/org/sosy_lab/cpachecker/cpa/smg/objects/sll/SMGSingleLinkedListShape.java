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
package org.sosy_lab.cpachecker.cpa.smg.objects.sll;

import com.google.common.collect.ComparisonChain;
import java.math.BigInteger;

public class SMGSingleLinkedListShape implements Comparable<SMGSingleLinkedListShape> {

  private final BigInteger hfo;
  private final BigInteger nfo;

  public SMGSingleLinkedListShape(BigInteger pHfo, BigInteger pNfo) {
    super();
    hfo = pHfo;
    nfo = pNfo;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + hfo.hashCode();
    result = prime * result + nfo.hashCode();
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
    SMGSingleLinkedListShape other = (SMGSingleLinkedListShape) obj;
    if (hfo != other.hfo) {
      return false;
    }
    if (nfo != other.nfo) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "SMGSingleLinkedListShape [hfo=" + hfo + ", nfo=" + nfo + "]";
  }

  public BigInteger getHfo() {
    return hfo;
  }

  public BigInteger getNfo() {
    return nfo;
  }

  @Override
  public int compareTo(SMGSingleLinkedListShape other) {
    return ComparisonChain.start()
        .compare(nfo, other.nfo)
        .compare(hfo, other.hfo)
        .result();
  }
}