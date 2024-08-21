// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll;

import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGAbstractList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectVisitor;

public final class SMGSingleLinkedList extends SMGAbstractList<SMGSingleLinkedListShape> {

  public SMGSingleLinkedList(long pSize, long pHfo, long pNfo, int pMinLength, int level) {
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
    return "SLL(id="
        + getId()
        + " size="
        + getSize()
        + ", hfo="
        + getHfo()
        + ", nfo="
        + getNfo()
        + ", len="
        + getMinimumLength()
        + ", level="
        + getLevel()
        + ")";
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
