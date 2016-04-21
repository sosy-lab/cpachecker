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
package org.sosy_lab.cpachecker.cpa.smg.objects.dls;

import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObjectVisitor;

public class SMGDoublyLinkedList extends SMGObject implements SMGAbstractObject {

  private final int minimumLength;

  private final int hfo;
  private final int nfo;
  private final int pfo;
  private final int id = SMGValueFactory.getNewValue();

  public SMGDoublyLinkedList(int pSize, int pHfo, int pNfo, int pPfo,
      int pMinLength, int level) {
    super(pSize, "dls", level, SMGObjectKind.DLL);

    hfo = pHfo;
    nfo = pNfo;
    pfo = pPfo;
    minimumLength = pMinLength;
  }

  public SMGDoublyLinkedList(SMGDoublyLinkedList other) {
    super(other.getSize(), other.getLabel(), other.getLevel(), SMGObjectKind.DLL);

    hfo = other.hfo;
    nfo = other.nfo;
    pfo = other.pfo;
    minimumLength = other.minimumLength;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject#matchGenericShape(org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject)
   */
  @Override
  public boolean matchGenericShape(SMGAbstractObject pOther) {
    return pOther.getKind() == SMGObjectKind.DLL;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject#matchSpecificShape(org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject)
   */
  @Override
  public boolean matchSpecificShape(SMGAbstractObject obj) {

    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SMGDoublyLinkedList other = (SMGDoublyLinkedList) obj;
    if (hfo != other.hfo) {
      return false;
    }
    if (nfo != other.nfo) {
      return false;
    }
    if (pfo != other.pfo) {
      return false;
    }

    return true;
  }

  public int getMinimumLength() {
    return minimumLength;
  }

  @Override
  public void accept(SMGObjectVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public boolean isMoreGeneral(SMGObject pOther) {

    if (pOther.getKind() == SMGObjectKind.DLL && matchSpecificShape((SMGDoublyLinkedList) pOther)) {
      return minimumLength < ((SMGDoublyLinkedList) pOther).getMinimumLength();
    }

    if (pOther.getKind() == SMGObjectKind.REG) {
      return minimumLength < 2;
    }

    return false;
  }

  public int getHfo() {
    return hfo;
  }

  public int getNfo() {
    return nfo;
  }

  public int getPfo() {
    return pfo;
  }

  @Override
  public SMGObject join(SMGObject pOther, boolean pIncreaseLevel) {

    int maxLevel = Math.max(getLevel(), pOther.getLevel());

    if(pOther.getKind() == SMGObjectKind.DLL) {

      SMGDoublyLinkedList otherLinkedList = (SMGDoublyLinkedList) pOther;
      assert getSize() == otherLinkedList.getSize();
      assert getHfo() == otherLinkedList.getHfo();
      assert getNfo() == otherLinkedList.getNfo();
      assert getPfo() == otherLinkedList.getPfo();

      int minlength = Math.min(getMinimumLength(), otherLinkedList.getMinimumLength());

      if (pIncreaseLevel) {
        return new SMGDoublyLinkedList(getSize(), getHfo(), getNfo(), getPfo(), minlength,
            maxLevel + 1);
      } else {

        if (minimumLength == minlength && maxLevel == getLevel()) {
          return this;
        } else {
          return new SMGDoublyLinkedList(getSize(), getHfo(), getNfo(), getPfo(), minlength,
              maxLevel);
        }
      }

    } else if(pOther.getKind() == SMGObjectKind.REG) {
      assert getSize() == pOther.getSize();

      int minlength = Math.min(getMinimumLength(), 1);

      if(pIncreaseLevel) {
        return new SMGDoublyLinkedList(getSize(), getHfo(), getNfo(), getPfo(), minlength, maxLevel + 1);
      } else {
        if (minlength == getMinimumLength() && maxLevel == getLevel()) {
          return this;
        } else {
          return new SMGDoublyLinkedList(getSize(), getHfo(), getNfo(), getPfo(), minlength,
              maxLevel);
        }
      }

    } else {
      throw new IllegalArgumentException("join called on unjoinable Objects");
    }
  }

  @Override
  public String toString() {
    return "DLL(id=" + id + " size=" + getSize() + ", hfo=" + hfo + ", nfo=" + nfo + ", pfo=" + pfo
        + ", len=" + minimumLength + ", level=" + getLevel() + ")";
  }

  @Override
  public SMGObject copy() {
    return new SMGDoublyLinkedList(this);
  }

  @Override
  public SMGObject copy(int level) {
    return new SMGDoublyLinkedList(getSize(), getHfo(), getNfo(), getPfo(), getMinimumLength(), level);
  }

  @Override
  public boolean isAbstract() {
    return true;
  }
}