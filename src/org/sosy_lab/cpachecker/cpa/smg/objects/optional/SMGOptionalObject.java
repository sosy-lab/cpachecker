/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.objects.optional;

import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObjectVisitor;

/**
 * Abstraction that represents states either containing a concrete region
 * represented by this abstract object, or no region. Every field of this
 * object may only contain the same value. Every pointer leading to this
 * object either lead to the concrete region represented by this object,
 * or to the one value leading from this object.
 */
public class SMGOptionalObject extends SMGObject implements SMGAbstractObject {

  int id = SMGValueFactory.getNewValue();

  public SMGOptionalObject(int pSize) {
    super(pSize, "1/0", SMGObjectKind.OPTIONAL);
  }

  @Override
  public boolean isAbstract() {
    return true;
  }

  public SMGOptionalObject(int pSize, int pLevel) {
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
  public SMGObject copy() {
    return new SMGOptionalObject(getSize(), getLevel());
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
  public void accept(SMGObjectVisitor pVisitor) {
    pVisitor.visit(this);
  }

  @Override
  public SMGObject join(SMGObject pOther, int pDestLevel) {

    /*Only join if other is region or optional object, otherwise
     * let other object join.*/

    assert getSize() == pOther.getSize();

    int level = Math.max(this.getLevel(), pOther.getLevel());

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