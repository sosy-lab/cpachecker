// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
