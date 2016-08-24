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
package org.sosy_lab.cpachecker.cpa.smg.objects;

import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGObjectTemplate;

import java.util.Map;

public final class SMGRegion extends SMGObject implements SMGObjectTemplate {

  public SMGRegion(int pSize, String pLabel) {
    super(pSize, pLabel, SMGObjectKind.REG);
  }

  public SMGRegion(SMGRegion pOther) {
    super(pOther);
  }

  public SMGRegion(int pSize, String pLabel, int pLevel) {
    super(pSize, pLabel, pLevel, SMGObjectKind.REG);
  }

  @Override
  public String toString() {
    return "REGION("+ getLabel() + ", " + getSize() + "B)\n" + "level=" + getLevel();
  }

  public boolean propertiesEqual(SMGRegion pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther == null) {
      return false;
    }

    if (getLabel() == null) {
      if (pOther.getLabel() != null) {
        return false;
      }
    } else if (!getLabel().equals(pOther.getLabel())) {
      return false;
    }

    if (getSize() != pOther.getSize()) {
      return false;
    }
    return true;
  }

  @Override
  public boolean isAbstract() {
    return false;
  }

  @Override
  public void accept(SMGObjectVisitor visitor) {
    visitor.visit(this);
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
  public SMGRegion createConcreteObject(Map<Integer, Integer> pAbstractToConcretePointerMap) {
    return new SMGRegion(getSize(), getLabel() + " ID " + SMGValueFactory.getNewValue());
  }

  @Override
  public SMGObject copy() {
    return new SMGRegion(this);
  }

  @Override
  public SMGObject copy(int pNewLevel) {
    return new SMGRegion(getSize(), "ID" + SMGValueFactory.getNewValue() + " Copy", pNewLevel);
  }

  @Override
  public boolean isMoreGeneral(SMGObject pOther) {
    /*There exists no object that is less general than a region.*/
    return false;
  }
}