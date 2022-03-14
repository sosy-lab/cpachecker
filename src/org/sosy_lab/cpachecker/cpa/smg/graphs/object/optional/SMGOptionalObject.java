// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.optional;

import org.sosy_lab.cpachecker.cpa.smg.SMGCPA;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGAbstractObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectVisitor;

/**
 * Abstraction that represents states either containing a concrete region represented by this
 * abstract object, or no region. Every field of this object may only contain the same value. Every
 * pointer leading to this object either lead to the concrete region represented by this object, or
 * to the one value leading from this object.
 */
public class SMGOptionalObject extends SMGObject implements SMGAbstractObject {

  private final int id = SMGCPA.getNewValue();

  public SMGOptionalObject(int pSize) {
    super(pSize, "1/0", SMGObjectKind.OPTIONAL);
  }

  @Override
  public boolean isAbstract() {
    return true;
  }

  public SMGOptionalObject(long pSize, int pLevel) {
    super(pSize, "1/0", pLevel, SMGObjectKind.OPTIONAL);
  }

  @Override
  public boolean matchGenericShape(SMGAbstractObject pOther) {
    return pOther.getKind() == SMGObjectKind.OPTIONAL;
  }

  @Override
  public boolean matchSpecificShape(SMGAbstractObject pOther) {
    return matchGenericShape(pOther) && getSize() == ((SMGObject) pOther).getSize();
  }

  @Override
  public SMGObject copy(int pNewLevel) {
    return new SMGOptionalObject(getSize(), pNewLevel);
  }

  @Override
  public boolean isMoreGeneral(SMGObject pOther) {

    switch (pOther.getKind()) {
      case REG:
        return getSize() == pOther.getSize();
      default:
        return false;
    }
  }

  @Override
  public <T> T accept(SMGObjectVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SMGObject join(SMGObject pOther, int pDestLevel) {

    /*Only join if other is region or optional object, otherwise
     * let other object join.*/

    assert getSize() == pOther.getSize();

    int level = Math.max(getLevel(), pOther.getLevel());

    switch (pOther.getKind()) {
      case REG:
      case OPTIONAL:
        return copy(level);
      default:
        return pOther.join(this, pDestLevel);
    }
  }

  @Override
  public String toString() {
    return "OPTIONAL(id=" + id + " size=" + getSize() + ", level=" + getLevel() + ")";
  }
}
