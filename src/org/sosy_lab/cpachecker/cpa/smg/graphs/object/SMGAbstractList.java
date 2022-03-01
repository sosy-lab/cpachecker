// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object;

public abstract class SMGAbstractList<S> extends SMGObject implements SMGAbstractObject {

  private final int minimumLength;
  private final S shape;

  protected SMGAbstractList(
      int pSize, String label, int level, SMGObjectKind kind, S pShape, int pMinLength) {
    super(pSize, label, level, kind);
    shape = pShape;
    minimumLength = pMinLength;
  }

  protected SMGAbstractList(SMGAbstractList<S> other) {
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
