/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGListCandidate;
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

public class SMGSingleLinkedListCandidate implements SMGListCandidate {

  private final SMGObject startObject;
  private final CType nfoType;
  private final MachineModel model;
  private final SMGSingleLinkedListShape shape;

  public SMGSingleLinkedListCandidate(SMGObject pStartObject, int pNfo, int pHfo, CType pNfoType,
      MachineModel pModel) {
    startObject = pStartObject;
    nfoType = pNfoType;
    model = pModel;
    shape = new SMGSingleLinkedListShape(pHfo, pNfo);
  }

  public boolean hasRecursiveFieldType() {
    return SMGUtils.isRecursiveOnOffset(nfoType, shape.getNfo(), model);
  }

  public SMGObject getStartObject() {
    return startObject;
  }

  public int getHfo() {
    return shape.getHfo();
  }

  public int getNfo() {
    return shape.getNfo();
  }

  public SMGSingleLinkedListShape getShape() {
    return shape;
  }

  @Override
  public String toString() {
    return "SMGSingleLinkedListCandidate [startObject=" + startObject + ", nfo=" + getNfo() + ", hfo="
        + getHfo() + "]";
  }
}