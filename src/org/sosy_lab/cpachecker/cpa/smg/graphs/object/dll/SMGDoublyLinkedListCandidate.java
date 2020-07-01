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

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGListCandidate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;

public class SMGDoublyLinkedListCandidate extends SMGListCandidate<SMGDoublyLinkedListShape> {

  private SMGObject lastObject;
  private final long pfoSize;
  private final long nfoSize;

  public SMGDoublyLinkedListCandidate(
      SMGObject pStartObject,
      SMGObject pLastObject,
      long pHfo,
      long pPfo,
      long pNfo,
      long pPfoSize,
      long nNfoSize,
      MachineModel pModel) {
    super(pStartObject, pModel, new SMGDoublyLinkedListShape(pHfo, pPfo, pNfo));
    lastObject = pLastObject;
    pfoSize = pPfoSize;
    nfoSize = nNfoSize;
  }

  @Override
  public String toString() {
    return "SMGDoublyLinkedListCandidate [startObject="
        + getStartObject()
        + ", lastObject="
        + getLastObject()
        + ", hfo="
        + getShape().getHfo()
        + ", pfo="
        + getShape().getPfo()
        + ", nfo="
        + getShape().getNfo()
        + "]";
  }

  @Override
  public int hashCode() {
    return super.hashCode() * 13 + Objects.hash(pfoSize, nfoSize);
  }

  public SMGObject getLastObject() {
    return lastObject;
  }

  public void updateLastObject(SMGObject pLastObject) {
    lastObject = pLastObject;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SMGDoublyLinkedListCandidate)) {
      return false;
    }
    SMGDoublyLinkedListCandidate other = (SMGDoublyLinkedListCandidate) o;
    return super.equals(other)
        && Objects.equals(lastObject, other.getLastObject())
        && pfoSize == other.pfoSize
        && nfoSize == other.nfoSize;
  }
}
