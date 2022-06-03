// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object;

import com.google.errorprone.annotations.Immutable;
import java.util.Map;
import org.sosy_lab.cpachecker.cpa.smg.SMGCPA;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.generic.SMGObjectTemplate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

@Immutable
public final class SMGRegion extends SMGObject implements SMGObjectTemplate {

  public SMGRegion(long pSize, String pLabel) {
    super(pSize, pLabel, SMGObjectKind.REG);
  }

  public SMGRegion(long pSize) {
    super(pSize, "ID" + SMGCPA.getNewValue(), SMGObjectKind.REG);
  }

  public SMGRegion(SMGRegion pOther) {
    super(pOther);
  }

  public SMGRegion(long pSize, String pLabel, int pLevel) {
    super(pSize, pLabel, pLevel, SMGObjectKind.REG);
  }

  @Override
  public String toString() {
    return "REGION(" + getLabel() + ", " + getSize() + "b, " + "level=" + getLevel() + ")";
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
    if (pOther.isAbstract()) {
      // I am concrete, and the other is abstract: the abstraction should
      // know how to join with me
      return pOther.join(this, pDestLevel);
    } else if (getSize() == pOther.getSize()) {
      return new SMGRegion(getSize(), getLabel(), pDestLevel);
    }

    throw new UnsupportedOperationException("join() called on incompatible SMGObjects");
  }

  @Override
  public SMGRegion createConcreteObject(Map<SMGValue, SMGValue> pAbstractToConcretePointerMap) {
    return new SMGRegion(getSize(), getLabel() + " ID " + SMGCPA.getNewValue());
  }

  @Override
  public SMGObject copy(int pNewLevel) {
    return new SMGRegion(getSize(), "ID" + SMGCPA.getNewValue() + " Copy of " + getId(), pNewLevel);
  }

  @Override
  public boolean isMoreGeneral(SMGObject pOther) {
    /*There exists no object that is less general than a region.*/
    return false;
  }
}
