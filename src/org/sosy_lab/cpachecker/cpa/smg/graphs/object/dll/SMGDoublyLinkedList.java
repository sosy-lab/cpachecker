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
package org.sosy_lab.cpachecker.cpa.smg.graphs.object.dll;

import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGAbstractList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectVisitor;

public class SMGDoublyLinkedList extends SMGAbstractList<SMGDoublyLinkedListShape> {

  public SMGDoublyLinkedList(int pSize, long pHfo, long pNfo, long pPfo,
      int pMinLength, int level) {
    super(
        pSize,
        "dll",
        level,
        SMGObjectKind.DLL,
        new SMGDoublyLinkedListShape(pHfo, pPfo, pNfo),
        pMinLength);
  }

  public SMGDoublyLinkedList(SMGDoublyLinkedList other) {
    super(other);
  }

  @Override
  public <T> T accept(SMGObjectVisitor<T> visitor) {
    return visitor.visit(this);
  }

  public long getHfo() {
    return getShape().getHfo();
  }

  public long getNfo() {
    return getShape().getNfo();
  }

  public long getPfo() {
    return getShape().getPfo();
  }

  @Override
  public String toString() {
    return "DLL(id=" + id + " size=" + getSize() + ", hfo=" + getHfo() + ", nfo=" + getNfo()
        + ", pfo=" + getPfo() + ", len=" + getMinimumLength() + ", level=" + getLevel() + ")";
  }

  @Override
  public SMGObject copy(int level) {
    return copy(getMinimumLength(), level);
  }

  @Override
  protected SMGDoublyLinkedList copy(int newLength, int newLevel) {
    return new SMGDoublyLinkedList(
        new SMGDoublyLinkedList(getSize(), getHfo(), getNfo(), getPfo(), newLength, newLevel));
  }
}