/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGListCandidate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;

public class SMGSingleLinkedListCandidate extends SMGListCandidate<SMGSingleLinkedListShape> {

  private final long nfoSize;

  public SMGSingleLinkedListCandidate(
      SMGObject pStartObject, long pNfo, long pHfo, long pNfoSize, MachineModel pModel) {
    super(pStartObject, pModel, new SMGSingleLinkedListShape(pHfo, pNfo));
    nfoSize = pNfoSize;
  }

  @Override
  public String toString() {
    return "SMGSingleLinkedListCandidate [startObject=" + getStartObject()
    + ", nfo=" + getShape().getNfo() + ", hfo=" + getShape().getHfo() + "]";
  }

  @Override
  public int hashCode() {
    return super.hashCode() * 13 + Long.hashCode(nfoSize);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SMGSingleLinkedListCandidate)) {
      return false;
    }
    SMGSingleLinkedListCandidate other = (SMGSingleLinkedListCandidate) o;
    return super.equals(other) && Objects.equals(nfoSize, other.nfoSize);
  }
}