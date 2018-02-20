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

import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGAbstractList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectVisitor;


public final class SMGSingleLinkedList extends SMGAbstractList<SMGSingleLinkedListShape> {

  public SMGSingleLinkedList(int pSize, long pHfo, long pNfo,
      int pMinLength, int level) {
    super(
        pSize,
        "sll",
        level,
        SMGObjectKind.SLL,
        new SMGSingleLinkedListShape(pHfo, pNfo),
        pMinLength);
  }

  public SMGSingleLinkedList(SMGSingleLinkedList other) {
    super(other);
  }

  public long getNfo() {
    return getShape().getNfo();
  }

  public long getHfo() {
    return getShape().getHfo();
  }

  @Override
  public <T> T accept(SMGObjectVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "SLL(id=" + id + " size=" + getSize() + ", hfo=" + getHfo() + ", nfo=" + getNfo()
        + ", len=" + getMinimumLength() + ", level=" + getLevel() + ")";
  }

  @Override
  public SMGObject copy(int pNewLevel) {
    return copy(getMinimumLength(), pNewLevel);
  }

  @Override
  public SMGSingleLinkedList copy(int newLength, int pNewLevel) {
    return new SMGSingleLinkedList(getSize(), getHfo(), getNfo(), newLength, pNewLevel);
  }
}