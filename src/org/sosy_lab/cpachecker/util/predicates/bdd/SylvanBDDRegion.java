// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.bdd;

import com.google.common.primitives.Longs;
import jsylvan.JSylvan;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;

/** Regions represented using Sylvan BDDs. */
public class SylvanBDDRegion implements Region {

  private final long bddRepr;

  SylvanBDDRegion(long pBDD) {
    bddRepr = pBDD;
  }

  @Override
  public boolean isTrue() {
    return bddRepr == JSylvan.getTrue();
  }

  @Override
  public boolean isFalse() {
    return bddRepr == JSylvan.getFalse();
  }

  long getBDD() {
    return bddRepr;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof SylvanBDDRegion) {
      return bddRepr == ((SylvanBDDRegion) o).bddRepr;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Longs.hashCode(bddRepr);
  }

  @Override
  public String toString() {
    if (isTrue()) {
      return "true";
    } else if (isFalse()) {
      return "false";
    } else {
      return bddRepr + "";
    }
  }
}
