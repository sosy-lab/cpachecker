/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.identifiers;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

@SuppressWarnings("EqualsGetClass") // should be refactored
public class StructureIdentifier extends SingleIdentifier {
  protected AbstractIdentifier owner;

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
    return new GeneralStructureFieldIdentifier(name, type, dereference, owner);
  }

  public StructureFieldIdentifier toStructureFieldIdentifier() {
    if (owner instanceof SingleIdentifier) {
      return new StructureFieldIdentifier(name, ((SingleIdentifier) owner).type, dereference, null);
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
}
