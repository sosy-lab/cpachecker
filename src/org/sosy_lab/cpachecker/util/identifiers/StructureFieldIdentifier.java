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
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

@SuppressWarnings("EqualsGetClass") // should be refactored
public class StructureFieldIdentifier extends StructureIdentifier {

  public StructureFieldIdentifier(String pNm, CType pTp, int dereference, AbstractIdentifier own) {
    super(pNm, pTp, dereference, own);
  }

  @Override
  public String toString() {
    String info = Identifiers.getCharsOf(dereference);
    info += "(?.";
    info += name;
    info += ")";
    return info;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hashCode(type);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj) || getClass() != obj.getClass()) {
      return false;
    }
    StructureFieldIdentifier other = (StructureFieldIdentifier) obj;
    return Objects.equals(type, other.type);
  }

  @Override
  public StructureFieldIdentifier cloneWithDereference(int deref) {
    return new StructureFieldIdentifier(name, type, deref, owner);
  }

  @Override
  public Collection<AbstractIdentifier> getComposedIdentifiers() {
    return ImmutableSet.of();
  }

  @Override
  public String toLog() {
    return "f;" + name + ";" + dereference;
  }

  @Override
  public GeneralIdentifier getGeneralId() {
    return new GeneralStructureFieldIdentifier(name, dereference);
  }

  @Override
  public int compareTo(AbstractIdentifier pO) {
    if (pO instanceof GlobalVariableIdentifier || pO instanceof LocalVariableIdentifier) {
      return -1;
    } else if (pO instanceof StructureFieldIdentifier) {
      return super.compareTo(pO);
    } else {
      return 1;
    }
  }
}
