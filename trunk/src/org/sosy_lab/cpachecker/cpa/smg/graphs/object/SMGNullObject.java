// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object;

/** The {@link SMGNullObject} represents the target of NULL. */
public final class SMGNullObject extends SMGObject {

  public static final SMGNullObject INSTANCE = new SMGNullObject();

  private SMGNullObject() {
    super(0, "NULL", SMGObjectKind.NULL);
  }

  @Override
  public String toString() {
    return "NULL";
  }

  @Override
  public SMGObject copy(int level) {
    return this;
  }

  @Override
  public boolean isMoreGeneral(SMGObject pOther) {
    /*There is no object that can replace the null object in an smg.*/
    return false;
  }

  @Override
  public boolean isAbstract() {
    return false;
  }

  @Override
  public <T> T accept(SMGObjectVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SMGObject join(SMGObject pOther, int pDestLevel) {
    throw new UnsupportedOperationException("NULL does not join"); // TODO why not?
  }
}
