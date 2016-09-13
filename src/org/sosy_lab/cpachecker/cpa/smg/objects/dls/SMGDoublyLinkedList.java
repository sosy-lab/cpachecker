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

  private final SMGDoublyLinkedListShape dllShape;
  private final int id = SMGValueFactory.getNewValue();

  public SMGDoublyLinkedList(int pSize, int pHfo, int pNfo, int pPfo,
      int pMinLength, int level) {
    super(pSize, "dls", level, SMGObjectKind.DLL);

    dllShape = new SMGDoublyLinkedListShape(pHfo, pPfo, pNfo);
    minimumLength = pMinLength;
  }

  public SMGDoublyLinkedList(SMGDoublyLinkedList other) {
    super(other.getSize(), other.getLabel(), other.getLevel(), SMGObjectKind.DLL);

    dllShape = other.dllShape;
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
    return dllShape.equals(other.dllShape);
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

    switch (pOther.getKind()) {
      case REG:
        return minimumLength < 2;
      case OPTIONAL:
        return minimumLength == 0;
      case DLL:
        return matchSpecificShape((SMGAbstractObject) pOther)
            && minimumLength < ((SMGDoublyLinkedList) pOther).minimumLength;
      default:
        return false;
    }
  }

  public int getHfo() {
    return dllShape.getHfo();
  }

  public int getNfo() {
    return dllShape.getNfo();
  }

  public int getPfo() {
    return dllShape.getPfo();
  }

  @Override
  public SMGObject join(SMGObject pOther, int pDestLevel) {

    switch (pOther.getKind()) {
      case DLL:

        SMGDoublyLinkedList otherLinkedList = (SMGDoublyLinkedList) pOther;
        assert matchSpecificShape(otherLinkedList);

        int minlength = Math.min(getMinimumLength(), otherLinkedList.getMinimumLength());

        return new SMGDoublyLinkedList(getSize(), getHfo(), getNfo(), getPfo(), minlength,
            pDestLevel);

      case REG:
      case OPTIONAL:
        assert getSize() == pOther.getSize();

        int otherLength = pOther.getKind() == SMGObjectKind.REG ? 1 : 0;
        minlength = Math.min(getMinimumLength(), otherLength);

        return new SMGDoublyLinkedList(getSize(), getHfo(), getNfo(), getPfo(), minlength,
            pDestLevel);

      default:
        throw new IllegalArgumentException("join called on unjoinable Objects");
    }
  }

  @Override
  public String toString() {
    return "DLL(id=" + id + " size=" + getSize() + ", hfo=" + dllShape.getHfo() + ", nfo=" + dllShape.getNfo() + ", pfo=" + dllShape.getPfo()
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