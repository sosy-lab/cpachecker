// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object;

public class DummyAbstraction extends SMGObject implements SMGAbstractObject {

  public DummyAbstraction(SMGObject pPrototype) {
    super(pPrototype);
  }

  @Override
  public boolean matchGenericShape(SMGAbstractObject pOther) {
    return pOther instanceof DummyAbstraction;
  }

  @Override
  public boolean matchSpecificShape(SMGAbstractObject pOther) {
    return true;
  }

  @Override
  public boolean isAbstract() {
    return true;
  }

  @Override
  public SMGObject copy(int pNewLevel) {
    return new DummyAbstraction(this);
  }

  @Override
  public boolean isMoreGeneral(SMGObject pOther) {
    return false;
  }

  @Override
  public <T> T accept(SMGObjectVisitor<T> pVisitor) {
    throw new UnsupportedOperationException("Dummy does not support visitors"); // TODO why not?
  }

  @Override
  public SMGObject join(SMGObject pOther, int pDestLevel) {
    throw new UnsupportedOperationException("Dummy does not join"); // TODO why not?
  }
}
