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
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGListCandidate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;

public class SMGDoublyLinkedListCandidate extends SMGListCandidate<SMGDoublyLinkedListShape> {

  private final CType pfoType;
  private final CType nfoType;

  public SMGDoublyLinkedListCandidate(SMGObject pObject, long pHfo, long pPfo, long pNfo,
      CType pPfoType, CType nNfoType, MachineModel pModel) {
    super(pObject, pModel, new SMGDoublyLinkedListShape(pHfo, pPfo, pNfo));
    pfoType = pPfoType;
    nfoType = nNfoType;
  }

  @Override
  public boolean hasRecursiveFields() {
    return SMGUtils.isRecursiveOnOffset(pfoType, getShape().getPfo(), model)
        && SMGUtils.isRecursiveOnOffset(nfoType, getShape().getNfo(), model);
  }

  @Override
  public String toString() {
    return "SMGDoublyLinkedListCandidate [startObject=" + getStartObject()
        + ", hfo=" + getShape().getHfo() + ", pfo=" + getShape().getPfo()
        + ", nfo=" + getShape().getNfo() + "]";
  }

  @Override
  public int hashCode() {
    return super.hashCode() * 13 + Objects.hash(pfoType, nfoType);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SMGDoublyLinkedListCandidate)) {
      return false;
    }
    SMGDoublyLinkedListCandidate other = (SMGDoublyLinkedListCandidate) o;
    return super.equals(other)
        && Objects.equals(pfoType, other.pfoType)
        && Objects.equals(nfoType, other.nfoType);
  }
}