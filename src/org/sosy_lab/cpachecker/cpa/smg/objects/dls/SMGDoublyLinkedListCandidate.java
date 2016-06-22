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

import org.sosy_lab.cpachecker.cpa.smg.SMGListCandidate;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;


public class SMGDoublyLinkedListCandidate implements SMGListCandidate {

  private final SMGObject startObject;
  private final int hfo;
  private final int pfo;
  private final int nfo;

  public SMGDoublyLinkedListCandidate(SMGObject pObject, int pHfo, int pPfo, int pNfo) {
    startObject = pObject;
    hfo = pHfo;
    pfo = pPfo;
    nfo = pNfo;
  }

  @Override
  public String toString() {
    return "SMGDoublyLinkedListCandidate [startObject=" + startObject + ", hfo=" + hfo + ", pfo="
        + pfo + ", nfo=" + nfo + "]";
  }

  public boolean isConsistentWith(SMGDoublyLinkedListCandidate other) {
    return hfo == other.hfo && nfo == other.nfo && pfo == other.pfo;
  }

  public SMGObject getObject() {
    return startObject;
  }

  public int getHfo() {
    return hfo;
  }

  public int getPfo() {
    return pfo;
  }

  public int getNfo() {
    return nfo;
  }
}