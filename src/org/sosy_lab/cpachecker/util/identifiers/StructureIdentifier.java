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
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

@SuppressWarnings("EqualsGetClass") // should be refactored
public class StructureIdentifier extends SingleIdentifier {
  protected final AbstractIdentifier owner;

  public StructureIdentifier(String pNm, CType pTp, int dereference, AbstractIdentifier own) {
    super(pNm, pTp, dereference);
    this.owner = own;
  }

  @Override
  public String toString() {
    String info = Identifiers.getCharsOf(dereference);
    info += "((" + owner + ").";
    info += name + ")";
    return info;
  }

  @Override
  public StructureIdentifier cloneWithDereference(int pDereference) {
    return new StructureIdentifier(name, type, pDereference, owner);
  }

  public AbstractIdentifier getOwner() {
    return owner;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hashCode(owner);
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
    StructureIdentifier other = (StructureIdentifier) obj;
    return Objects.equals(owner, other.owner);
  }

  @Override
  public boolean isGlobal() {
    return owner.isGlobal();
  }

  @Override
  public boolean isDereferenced() {
    if (super.isDereferenced()) {
      return true;
    } else {
      return owner.isDereferenced();
    }
  }

  @Override
  public String toLog() {
    return "s;" + name + ";" + dereference;
  }

  @Override
  public GeneralIdentifier getGeneralId() {
    return new GeneralStructureFieldIdentifier(name, dereference);
  }

  public StructureFieldIdentifier toStructureFieldIdentifier() {
    if (owner instanceof SingleIdentifier) {
      // a.b and c->b should refer to the same structure field identifier, so, remove dereferences.
      CType ownerType = ((SingleIdentifier) owner).type;
      int ownerDereference = owner.getDereference();
      while (ownerDereference > 0 && ownerType instanceof CPointerType) {
        ownerDereference--;
        ownerType = ((CPointerType) ownerType).getType();
      }
      return new StructureFieldIdentifier(name, ownerType, dereference, null);
    } else {
      return new StructureFieldIdentifier(name, type, dereference, null);
    }
  }

  @Override
  public Collection<AbstractIdentifier> getComposedIdentifiers() {
    Set<AbstractIdentifier> result = Sets.newHashSet(owner);
    result.addAll(owner.getComposedIdentifiers());
    return result;
  }

  @Override
  public int compareTo(AbstractIdentifier pO) {
    if (pO instanceof GlobalVariableIdentifier
        || pO instanceof LocalVariableIdentifier
        || (!(this instanceof StructureFieldIdentifier)
            && pO instanceof StructureFieldIdentifier)) {
      return -1;
    } else if (pO instanceof StructureIdentifier) {
      int s = super.compareTo(pO);
      if (s != 0) {
        return s;
      }
      if (owner == null) {
        assert (((StructureIdentifier) pO).getOwner() == null);
        return 0;
      }
      return owner.compareTo(((StructureIdentifier) pO).getOwner());
    } else {
      return 1;
    }
  }
}
