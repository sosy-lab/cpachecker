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
package org.sosy_lab.cpachecker.cpa.smg.objects.sll;

import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObjectVisitor;


public final class SMGSingleLinkedList extends SMGObject implements SMGAbstractObject {

  private final int minimumLength;
  private final SMGSingleLinkedListShape shape;
  private final int id = SMGValueFactory.getNewValue();

  public SMGSingleLinkedList(int pSize, int pHfo, int pNfo,
      int pMinLength, int level) {
    super(pSize, "sll", level, SMGObjectKind.SLL);

    minimumLength = pMinLength;
    shape = new SMGSingleLinkedListShape(pHfo, pNfo);
  }

  public SMGSingleLinkedList(SMGSingleLinkedList other) {
    super(other.getSize(), other.getLabel(), other.getLevel(), SMGObjectKind.SLL);

    minimumLength = other.minimumLength;
    shape = other.shape;
  }

  public int getMinimumLength() {
    return minimumLength;
  }

  @Override
  public boolean isAbstract() {
    return true;
  }

  public int getNfo() {
    return shape.getNfo();
  }

  public int getHfo() {
    return shape.getHfo();
  }

  public SMGSingleLinkedListShape getShape() {
    return shape;
  }

  @Override
  public String toString() {
    return "SLL(id=" + id + " size=" + getSize() + ", hfo=" + shape.getHfo() + ", nfo=" + shape.getNfo()
        + ", len=" + minimumLength + ", level=" + getLevel() + ")";
  }

  @Override
  public void accept(SMGObjectVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public boolean matchGenericShape(SMGAbstractObject pOther) {
    return pOther.getKind() == SMGObjectKind.SLL;
  }

  @Override
  public boolean matchSpecificShape(SMGAbstractObject pOther) {
    if (!matchGenericShape(pOther)) {
      return false;
    }

    SMGSingleLinkedList sllOther = (SMGSingleLinkedList) pOther;

    return shape.equals(sllOther.shape);
  }

  @Override
  public boolean isMoreGeneral(SMGObject pOther) {

    switch (pOther.getKind()) {
      case REG:
        return minimumLength < 2;
      case OPTIONAL:
        return minimumLength == 0;
      case SLL:
        return matchSpecificShape((SMGAbstractObject) pOther)
            && minimumLength < ((SMGSingleLinkedList) pOther).minimumLength;
      default:
        return false;
    }
  }

  @Override
  public SMGObject join(SMGObject pOther, int pDestLevel) {

    switch (pOther.getKind()) {
      case SLL:
        SMGSingleLinkedList otherLinkedList = (SMGSingleLinkedList) pOther;
        assert matchSpecificShape(otherLinkedList);

        int minlength = Math.min(getMinimumLength(), otherLinkedList.getMinimumLength());

        return new SMGSingleLinkedList(getSize(), getHfo(), getNfo(), minlength,
            pDestLevel);
      case REG:
      case OPTIONAL:
        assert getSize() == pOther.getSize();

        int otherLength = pOther.getKind() == SMGObjectKind.REG ? 1 : 0;
        minlength = Math.min(getMinimumLength(), otherLength);

        return new SMGSingleLinkedList(getSize(), getHfo(), getNfo(), minlength,
            pDestLevel);

      default:
        throw new IllegalArgumentException("join called on unjoinable Objects");
    }
  }

  @Override
  public SMGObject copy() {
    return new SMGSingleLinkedList(this);
  }

  @Override
  public SMGObject copy(int pNewLevel) {
    return new SMGSingleLinkedList(getSize(), getHfo(), getNfo(), minimumLength, pNewLevel);
  }
}