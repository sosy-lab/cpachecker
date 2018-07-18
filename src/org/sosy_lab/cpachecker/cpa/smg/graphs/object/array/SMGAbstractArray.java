/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.graphs.object.array;

import org.sosy_lab.cpachecker.cpa.smg.SMGCPA;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGAbstractObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectVisitor;

public class SMGAbstractArray extends SMGObject implements SMGAbstractObject {

  public static class ArraySymVal {
    int low;
    int high;
  }

  private final ArraySymVal length;
  protected final int id = SMGCPA.getNewValue(); // just for logging, not for other usage!

  public SMGAbstractArray(int pSize, ArraySymVal pLength, int level) {
    super(pSize,"array", level, SMGObjectKind.ARRAY);
    length = pLength;
  }

  @Override
  public boolean matchGenericShape(SMGAbstractObject pOther) {
    return pOther.getKind() == getKind();
  }

  @Override
  public boolean matchSpecificShape(SMGAbstractObject pOther) {
    return matchGenericShape(pOther);
  }

  public ArraySymVal getSymbolicLength() {
    return length;
  }

  @Override
  public boolean isMoreGeneral(SMGObject pOther) {
    return false; // it is incomparable to everything except the same-length abst. arr.
  }

  @Override
  public SMGObject join(SMGObject pOther, int pDestLevel) {

    switch (pOther.getKind()) {
      case ARRAY:
        SMGAbstractArray otherAbstractArray = (SMGAbstractArray) pOther;
        assert matchSpecificShape(otherAbstractArray); // unnecessary
        assert (length == otherAbstractArray.length);
        return copy(length, pDestLevel);

      case REG:
        throw new IllegalArgumentException(
            "join called on possible fixed size AbstractArray and REG, which is not supported now"
        ); // TODO: make the extension to join with concrete arrays (regions)
//        assert getSize() == pOther.getSize();
//        int otherLength = pOther.getKind() == SMGObjectKind.REG ? 1 : 0;
//        minlength = Math.min(getMinimumLength(), otherLength);
//        return copy(minlength, pDestLevel);

      case OPTIONAL:
      default:
        throw new IllegalArgumentException("join called on unjoinable Objects");
    }
  }

  @Override
  public boolean isAbstract() {
    return true;
  }


  @Override
  public <T> T accept(SMGObjectVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "ARRAY(id=" + id + " size=" + getSize() + ", len=" + getLabel() + ", level=" + getLevel() + ")";
  }

  @Override
  public SMGObject copy(int level) {
    return copy(length, level);
  }

  //@Override
  protected SMGAbstractArray copy(ArraySymVal newLength, int newLevel) {
    return new SMGAbstractArray(getSize(), newLength, newLevel);
  }
}