/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Objects;

public final class ConstantIdentifier implements AbstractIdentifier {

  protected String name;
  protected int dereference;

  public ConstantIdentifier(String nm, int deref) {
    name = nm;
    dereference = deref;
  }

  @Override
  public ConstantIdentifier cloneWithDereference(int pDereference) {
    return new ConstantIdentifier(name, pDereference);
  }

  @Override
  public String toString() {
    String info = Identifiers.getCharsOf(dereference);
    info += name;
    return info;
  }

  @Override
  public boolean isGlobal() {
    return false;
  }

  @Override
  public int getDereference() {
    return dereference;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + dereference;
    result = prime * result + Objects.hashCode(name);
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
    ConstantIdentifier other = (ConstantIdentifier) obj;
    return dereference == other.dereference && Objects.equals(name, other.name);
  }

  @Override
  public boolean isPointer() {
    return isDereferenced();
  }

  @Override
  public boolean isDereferenced() {
    return (dereference > 0);
  }

  public String getName() {
    return name;
  }

  @Override
  public int compareTo(AbstractIdentifier pO) {
    if (pO instanceof ReturnIdentifier) {
      return 1;
    } else if (pO instanceof ConstantIdentifier) {
      return this.name.compareTo(((ConstantIdentifier) pO).name);
    } else {
      return -1;
    }
  }

  @Override
  public Collection<AbstractIdentifier> getComposedIdentifiers() {
    return ImmutableSet.of();
  }
}
