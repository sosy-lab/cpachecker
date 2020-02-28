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
package org.sosy_lab.cpachecker.cpa.smg.graphs.object;

import com.google.errorprone.annotations.Immutable;
import java.util.Map;
import org.sosy_lab.cpachecker.cpa.smg.SMGCPA;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.generic.SMGObjectTemplate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

@Immutable
public final class SMGRegion extends SMGObject implements SMGObjectTemplate {

  public SMGRegion(int pSize, String pLabel) {
    super(pSize, pLabel, SMGObjectKind.REG);
  }

  public SMGRegion(int pSize) {
    super(pSize, "ID" + SMGCPA.getNewValue(), SMGObjectKind.REG);
  }

  public SMGRegion(SMGRegion pOther) {
    super(pOther);
  }

  public SMGRegion(int pSize, String pLabel, int pLevel) {
    super(pSize, pLabel, pLevel, SMGObjectKind.REG);
  }

  @Override
  public String toString() {
    return "REGION("+ getLabel() + ", " + getSize() + "b, " + "level=" + getLevel() + ")";
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
    return new SMGRegion(getSize(), "ID" + SMGCPA.getNewValue() + " Copy", pNewLevel);
  }

  @Override
  public boolean isMoreGeneral(SMGObject pOther) {
    /*There exists no object that is less general than a region.*/
    return false;
  }
}