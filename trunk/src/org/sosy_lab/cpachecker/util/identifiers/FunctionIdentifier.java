// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.identifiers;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class FunctionIdentifier extends SingleIdentifier {

  public FunctionIdentifier(String nm, CType tp, int deref) {
    super(nm, tp, deref);
  }

  @Override
  public int compareTo(AbstractIdentifier pO) {
    if (pO instanceof FunctionIdentifier) {
      return super.compareTo(pO);
    } else {
      return -1;
    }
  }

  @Override
  public boolean isGlobal() {
    return false;
  }

  @Override
  public SingleIdentifier cloneWithDereference(int pDereference) {
    return new FunctionIdentifier(name, type, pDereference);
  }

  @Override
  public String toString() {
    return super.toString() + "()";
  }

  @Override
  public String toLog() {
    return "func;" + name + ";" + dereference;
  }

  @Override
  public GeneralIdentifier getGeneralId() {
    return null;
  }

  @Override
  public Collection<AbstractIdentifier> getComposedIdentifiers() {
    return ImmutableSet.of();
  }
}
