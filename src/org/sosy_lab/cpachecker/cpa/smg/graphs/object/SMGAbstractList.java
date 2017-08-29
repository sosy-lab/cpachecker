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

import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;

public abstract class SMGAbstractList<S> extends SMGObject implements SMGAbstractObject {

  private final int minimumLength;
  private final S shape;
  protected final int id = SMGValueFactory.getNewValue(); // just for logging, not for other usage!

  public SMGAbstractList(
      int pSize, String label, int level, SMGObjectKind kind, S pShape, int pMinLength) {
    super(pSize, label, level, kind);
    shape = pShape;
    minimumLength = pMinLength;
  }

  public SMGAbstractList(SMGAbstractList<S> other) {
    super(other.getSize(), other.getLabel(), other.getLevel(), other.getKind());
    shape = other.shape;
    minimumLength = other.minimumLength;
  }

  @Override
  public boolean matchGenericShape(SMGAbstractObject pOther) {
    return pOther.getKind() == getKind();
  }

  @Override
  public boolean matchSpecificShape(SMGAbstractObject pOther) {
    return matchGenericShape(pOther) && shape.equals(((SMGAbstractList<?>) pOther).shape);
  }

  public int getMinimumLength() {
    return minimumLength;
  }

  public S getShape() {
    return shape;
  }

  @Override
  public boolean isMoreGeneral(SMGObject pOther) {

    switch (pOther.getKind()) {
      case REG:
        return minimumLength < 2;
      case OPTIONAL:
        return minimumLength == 0;
      case DLL:
      case SLL:
        return matchSpecificShape((SMGAbstractObject) pOther)
            && minimumLength < ((SMGAbstractList<?>) pOther).minimumLength;
      default:
        return false;
    }
  }

  @Override
  public SMGObject join(SMGObject pOther, int pDestLevel) {

    switch (pOther.getKind()) {
      case DLL:
      case SLL:
        SMGAbstractList<?> otherLinkedList = (SMGAbstractList<?>) pOther;
        assert matchSpecificShape(otherLinkedList);
        int minlength = Math.min(getMinimumLength(), otherLinkedList.getMinimumLength());
        return copy(minlength, pDestLevel);

      case REG:
      case OPTIONAL:
        assert getSize() == pOther.getSize();
        int otherLength = pOther.getKind() == SMGObjectKind.REG ? 1 : 0;
        minlength = Math.min(getMinimumLength(), otherLength);
        return copy(minlength, pDestLevel);

      default:
        throw new IllegalArgumentException("join called on unjoinable Objects");
    }
  }

  @Override
  public boolean isAbstract() {
    return true;
  }

  protected abstract SMGAbstractList<S> copy(int newlength, int newLevel);
}
