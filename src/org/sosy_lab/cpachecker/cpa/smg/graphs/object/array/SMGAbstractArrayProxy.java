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

import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.smg.SMGCPA;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGAbstractObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectVisitor;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGArraySymbolicValue;

class SMGAbstratArrayBehindTheScenes {
  Set<SMGAbstractArrayProxy> proxies = new LinkedHashSet<>();
  private final SMGArraySymbolicValue length;
  private final int size;
  private final String initalLabel;

  SMGAbstratArrayBehindTheScenes(int pSize, SMGArraySymbolicValue pLength, String pInitalLabel) {
    size = pSize;
    length = pLength;
    initalLabel = pInitalLabel;
  }

  public SMGArraySymbolicValue getSymbolicLength() {
    return length;
  }
  public int getSize() { return size; }
  public java.lang.String getInitalLabel() { return initalLabel; }
}

public abstract class SMGAbstractArrayProxy extends SMGObject implements SMGAbstractObject {
  SMGAbstratArrayBehindTheScenes backendObj;
  protected final int id = SMGCPA.getNewValue(); // just for logging, not for other usage!

  public SMGAbstractArrayProxy(SMGAbstratArrayBehindTheScenes pBackendObj, int level, String pLabel) {
    super(-1, pLabel, level, SMGObjectKind.ARRAY);
    backendObj = pBackendObj;
  }

  public static SMGAbstractArrayProxySegment createArray(int pSize, SMGArraySymbolicValue pLength, int pLevel, String pLabel)
  {
    SMGAbstratArrayBehindTheScenes backendObj = new SMGAbstratArrayBehindTheScenes(pSize, pLength, pLabel);
    return new SMGAbstractArrayProxySegment(backendObj, pLevel, pLabel);
  }

  public static class SMGAbstractArrayProxySingleElemenet extends SMGAbstractArrayProxy {
    private SMGAbstractArrayProxySingleElemenet(SMGAbstratArrayBehindTheScenes pBackendObj, int pLevel, String pLabel) {
      super(pBackendObj, pLevel, pLabel);
    }
  }

  public static class SMGAbstractArrayProxySegment extends SMGAbstractArrayProxy {
    private SMGAbstractArrayProxySegment(SMGAbstratArrayBehindTheScenes pBackendObj, int pLevel, String pLabel) {
      super(pBackendObj, pLevel, pLabel);
    }
  }

  public boolean isPartOfTheSameAbstractArray(SMGAbstractArrayProxy pOther)
  {
    return this.backendObj == pOther.backendObj;
  }

  public SMGAbstractArrayProxySingleElemenet materializeElement(SMGArraySymbolicValue offset)
  {
    SMGAbstractArrayProxySingleElemenet newEl = new SMGAbstractArrayProxySingleElemenet(backendObj, getLevel(), backendObj.getInitalLabel() + offset.label);
    backendObj.proxies.add(newEl);
  }

  @Override
  public int getSize() {
    return backendObj.getSize();
  }

  @Override
  public boolean matchGenericShape(SMGAbstractObject pOther) {
    return pOther.getKind() == getKind();
  }

  @Override
  public boolean matchSpecificShape(SMGAbstractObject pOther) {
    return matchGenericShape(pOther);
  }

  public SMGArraySymbolicValue getSymbolicLength() {
    return backendObj.getSymbolicLength();
  }

  @Override
  public boolean isMoreGeneral(SMGObject pOther) {
    return false; // it is incomparable to everything except the same-length abst. arr.
  }

  @Override
  public SMGObject join(SMGObject pOther, int pDestLevel) {

    switch (pOther.getKind()) {
      case ARRAY:
        throw new UnsupportedOperationException("You can not copy join proxy elements directly. Join the manually.");
        //TODO: need to try this out, this code is probably incomplete / wrong with the subclasses
        /*
        SMGAbstractArrayProxy otherAbstractArray = (SMGAbstractArrayProxy) pOther;
        if (isPartOfTheSameAbstractArray(otherAbstractArray))
        {
          return
        }
        assert matchSpecificShape(otherAbstractArray); // unnecessary
        assert (getSymbolicLength() == otherAbstractArray.getSymbolicLength());
        return copy(getSymbolicLength(), pDestLevel);
        */

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
    throw new UnsupportedOperationException("You can not copy this proxy element directly. Use XXXXX.");
  }
}