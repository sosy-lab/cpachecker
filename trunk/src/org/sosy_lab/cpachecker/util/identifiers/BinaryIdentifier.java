// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.identifiers;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

public final class BinaryIdentifier implements AbstractIdentifier {
  final AbstractIdentifier id1;
  final AbstractIdentifier id2;
  final int dereference;

  public BinaryIdentifier(AbstractIdentifier i1, AbstractIdentifier i2, int deref) {
    id1 = i1;
    id2 = i2;
    dereference = deref;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + dereference;
    result = prime * result + Objects.hashCode(id1);
    result = prime * result + Objects.hashCode(id2);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    BinaryIdentifier other = (BinaryIdentifier) obj;
    return dereference == other.dereference
        && Objects.equals(id1, other.id1)
        && Objects.equals(id2, other.id2);
  }

  @Override
  public String toString() {
    return Identifiers.getCharsOf(dereference) + "(" + id1 + " # " + id2 + ")";
  }

  @Override
  public boolean isGlobal() {
    return (id1.isGlobal() || id2.isGlobal());
  }

  @Override
  public BinaryIdentifier cloneWithDereference(int pDereference) {
    return new BinaryIdentifier(id1, id2, pDereference);
  }

  public AbstractIdentifier getIdentifier1() {
    return id1;
  }

  public AbstractIdentifier getIdentifier2() {
    return id2;
  }

  @Override
  public int getDereference() {
    return dereference;
  }

  @Override
  public boolean isPointer() {
    return (dereference != 0 || id1.isPointer() || id2.isPointer());
  }

  @Override
  public boolean isDereferenced() {
    return (dereference != 0 || id1.isDereferenced() || id2.isDereferenced());
  }

  @Override
  public int compareTo(AbstractIdentifier pO) {
    if (pO instanceof SingleIdentifier) {
      return -1;
    } else if (pO instanceof BinaryIdentifier) {
      int result = id1.compareTo(((BinaryIdentifier) pO).id1);
      return (result != 0 ? result : id2.compareTo(((BinaryIdentifier) pO).id2));
    } else {
      return 1;
    }
  }

  @Override
  public Collection<AbstractIdentifier> getComposedIdentifiers() {
    // Is important to get from *(a + i) -> *a
    int deref = id1.getDereference();
    AbstractIdentifier tmp1 = id1.cloneWithDereference(dereference + deref);
    deref = id2.getDereference();
    AbstractIdentifier tmp2 = id2.cloneWithDereference(dereference + deref);
    Set<AbstractIdentifier> result = Sets.newHashSet(tmp1, tmp2);
    result.addAll(tmp1.getComposedIdentifiers());
    result.addAll(tmp2.getComposedIdentifiers());
    return result;
  }
}
