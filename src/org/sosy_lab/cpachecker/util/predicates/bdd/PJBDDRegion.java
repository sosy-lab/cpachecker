/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.bdd;

import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.pjbdd.node.BDD;

public class PJBDDRegion implements Region {

  private BDD bddRep;

  public PJBDDRegion(BDD bdd) {
    this.bddRep = bdd;
  }

  @Override
  public boolean isTrue() {
    return this.bddRep.isTrue();
  }

  @Override
  public boolean isFalse() {
    return this.bddRep.isFalse();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof PJBDDRegion) {
      return bddRep.equals(((PJBDDRegion) o).bddRep);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return bddRep.hashCode();
  }

  @Override
  public String toString() {
    return bddRep.toString();
  }

  public static Region wrap(BDD bdd) {
    return new PJBDDRegion(bdd);
  }

  public static BDD unwrap(Region pRegion) {
    if (pRegion instanceof PJBDDRegion) {
      return ((PJBDDRegion) pRegion).bddRep;
    }
    throw new IllegalArgumentException("Wrong region type: " + pRegion.getClass());
  }
}
