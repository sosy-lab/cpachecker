// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.dll;

import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGAbstractList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectVisitor;

public class SMGDoublyLinkedList extends SMGAbstractList<SMGDoublyLinkedListShape> {

  public SMGDoublyLinkedList(
      long pSize, long pHfo, long pNfo, long pPfo, int pMinLength, int level) {
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
    return "DLL(id="
        + getId()
        + " size="
        + getSize()
        + ", hfo="
        + getHfo()
        + ", nfo="
        + getNfo()
        + ", pfo="
        + getPfo()
        + ", len="
        + getMinimumLength()
        + ", level="
        + getLevel()
        + ")";
  }

  @Override
  public SMGObject copy(int level) {
    return copy(getMinimumLength(), level);
  }

  @Override
  protected SMGDoublyLinkedList copy(int newLength, int newLevel) {
    return new SMGDoublyLinkedList(getSize(), getHfo(), getNfo(), getPfo(), newLength, newLevel);
  }
}
