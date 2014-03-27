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
package org.sosy_lab.cpachecker.cpa.pointer2.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class ExplicitLocationSet implements LocationSet {

  private final Set<Location> explicitSet = new HashSet<>();

  @Override
  public boolean mayPointTo(Location pElement) {
    return this.explicitSet.contains(pElement);
  }

  @Override
  public LocationSet addElement(Location pElement) {
    if (explicitSet.contains(pElement)) {
      return this;
    }
    ExplicitLocationSet result = new ExplicitLocationSet();
    result.explicitSet.addAll(explicitSet);
    result.explicitSet.add(pElement);
    return result;
  }

  @Override
  public LocationSet addElements(Iterable<Location> pElements) {
    ExplicitLocationSet result = new ExplicitLocationSet();
    for (Location target : pElements) {
      result.explicitSet.add(target);
    }
    if (!result.explicitSet.addAll(explicitSet) && result.explicitSet.size() == explicitSet.size()) {
      return this;
    }
    return result;
  }

  @Override
  public LocationSet removeElement(Location pElement) {
    if (!explicitSet.contains(pElement)) {
      return this;
    }
    ExplicitLocationSet result = new ExplicitLocationSet();
    result.explicitSet.addAll(explicitSet);
    result.explicitSet.remove(pElement);
    return result;
  }

  public static LocationSet from(Location pElement) {
    ExplicitLocationSet result = new ExplicitLocationSet();
    result.explicitSet.add(pElement);
    return result;
  }

  public static LocationSet from(Set<? extends Location> pElements) {
    ExplicitLocationSet result = new ExplicitLocationSet();
    result.explicitSet.addAll(pElements);
    return result;
  }

  public static LocationSet from(Iterable<? extends Location> pElements) {
    ExplicitLocationSet result = new ExplicitLocationSet();
    for (Location element : pElements) {
      result.explicitSet.add(element);
    }
    return result;
  }

  @Override
  public boolean isBot() {
    return explicitSet.isEmpty();
  }

  @Override
  public boolean isTop() {
    return false;
  }

  public Collection<Location> getElements() {
    return Collections.unmodifiableCollection(this.explicitSet);
  }

  @Override
  public LocationSet addElements(LocationSet pElements) {
    if (pElements == this) {
      return this;
    }
    if (pElements instanceof ExplicitLocationSet) {
      ExplicitLocationSet explicitLocationSet = (ExplicitLocationSet) pElements;
      return addElements(explicitLocationSet.explicitSet);
    }
    return pElements.addElements(this);
  }

  @Override
  public boolean containsAll(LocationSet pElements) {
    if (pElements == this) {
      return true;
    }
    if (pElements instanceof ExplicitLocationSet) {
      ExplicitLocationSet explicitLocationSet = (ExplicitLocationSet) pElements;
      return explicitSet.containsAll(explicitLocationSet.explicitSet);
    }
    return pElements.containsAll(this);
  }

  @Override
  public String toString() {
    return explicitSet.toString();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO instanceof LocationSet) {
      LocationSet o = (LocationSet) pO;
      if (o.isTop()) {
        return false;
      }
      if (o.isBot()) {
        return explicitSet.isEmpty();
      }
      if (o instanceof ExplicitLocationSet) {
        ExplicitLocationSet other = (ExplicitLocationSet) o;
        return explicitSet.equals(other.explicitSet);
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    if (isBot()) {
      return LocationSetBot.INSTANCE.hashCode();
    }
    if (isTop()) {
      assert false;
      return LocationSetTop.INSTANCE.hashCode();
    }
    return explicitSet.hashCode();
  }

}
