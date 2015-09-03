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



public enum LocationSetTop implements LocationSet {

  INSTANCE;

  @Override
  public boolean mayPointTo(String pTarget) {
    return true;
  }

  @Override
  public LocationSet addElement(String pTarget) {
    return this;
  }

  /**
   * This operation does not remove the given target from the set, thus
   * the resulting set is only an over-approximation of the represented
   * conceptual set. For a precise representation, it is necessary to
   * know the complete set of potential targets and remove the given
   * target from it.
   *
   * @param pTarget the target to remove.
   *
   * @return the same unchanged object.
   */
  @Override
  public LocationSet removeElement(String pTarget) {
    return this;
  }

  @Override
  public LocationSet addElements(Iterable<String> pTargets) {
    return this;
  }

  @Override
  public boolean isBot() {
    return false;
  }

  @Override
  public boolean isTop() {
    return true;
  }

  @Override
  public LocationSet addElements(LocationSet pElements) {
    return this;
  }

  @Override
  public boolean containsAll(LocationSet pElements) {
    return true;
  }

  @Override
  public String toString() {
    return Character.toString('\u22A4');
  }

}
