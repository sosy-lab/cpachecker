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

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGListCandidate;
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;


public class SMGDoublyLinkedListCandidate implements SMGListCandidate {

  private final SMGObject startObject;
  private final CType pfoType;
  private final CType nfoType;
  private final MachineModel model;
  private final SMGDoublyLinkedListShape dllShape;

  public SMGDoublyLinkedListCandidate(SMGObject pObject, int pHfo, int pPfo, int pNfo,
      CType pPfoType, CType nNfoType, MachineModel pModel) {
    startObject = pObject;
    dllShape = new SMGDoublyLinkedListShape(pHfo, pPfo, pNfo);
    pfoType = pPfoType;
    nfoType = nNfoType;
    model = pModel;
  }

  public boolean hasRecursiveFields() {
    return SMGUtils.isRecursiveOnOffset(pfoType, dllShape.getPfo(), model)
        && SMGUtils.isRecursiveOnOffset(nfoType, dllShape.getNfo(), model);
  }

  @Override
  public String toString() {
    return "SMGDoublyLinkedListCandidate [startObject=" + startObject + ", hfo=" + dllShape.getHfo() + ", pfo="
        + dllShape.getPfo() + ", nfo=" + dllShape.getNfo() + "]";
  }

  public boolean isConsistentWith(SMGDoublyLinkedListCandidate other) {
    return dllShape.equals(other.dllShape);
  }

  public SMGObject getObject() {
    return startObject;
  }

  public int getHfo() {
    return dllShape.getHfo();
  }

  public int getPfo() {
    return dllShape.getPfo();
  }

  public int getNfo() {
    return dllShape.getNfo();
  }

  public SMGDoublyLinkedListShape getDllShape() {
    return dllShape;
  }
}