// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll;

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
    return "SMGSingleLinkedListCandidate [startObject="
        + getStartObject()
        + ", nfo="
        + getShape().getNfo()
        + ", hfo="
        + getShape().getHfo()
        + "]";
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
    return super.equals(other) && nfoSize == other.nfoSize;
  }
}
