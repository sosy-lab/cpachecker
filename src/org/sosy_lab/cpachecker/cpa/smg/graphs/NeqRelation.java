// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs;

import com.google.common.base.Preconditions;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentMultimap;

/**
 * This class tracks Pairs of SMGValues. Implemented as an immutable map. The Multimap is used as
 * Bi-Map, i.e. for each pair (K,V) there exists also a pair (V,K).
 */
final class NeqRelation {

  private final PersistentMultimap<SMGValue, SMGValue> smgValues;

  public NeqRelation() {
    smgValues = PersistentMultimap.of();
  }

  private NeqRelation(PersistentMultimap<SMGValue, SMGValue> pMap) {
    smgValues = pMap;
  }

  public Set<SMGValue> getNeqsForValue(SMGValue pV) {
    return smgValues.get(pV);
  }

  public NeqRelation addRelationAndCopy(SMGValue pOne, SMGValue pTwo) {
    Preconditions.checkNotNull(pOne);
    Preconditions.checkNotNull(pTwo);
    return new NeqRelation(smgValues.putAndCopy(pOne, pTwo).putAndCopy(pTwo, pOne));
  }

  public NeqRelation removeRelationAndCopy(SMGValue pOne, SMGValue pTwo) {
    return new NeqRelation(smgValues.removeAndCopy(pOne, pTwo).removeAndCopy(pTwo, pOne));
  }

  public boolean neq_exists(SMGValue pOne, SMGValue pTwo) {
    return smgValues.get(pOne).contains(pTwo);
  }

  public NeqRelation removeValueAndCopy(SMGValue pOne) {
    PersistentMultimap<SMGValue, SMGValue> newSet = smgValues.removeAndCopy(pOne);
    for (SMGValue pTwo : smgValues.get(pOne)) {
      newSet = newSet.removeAndCopy(pTwo, pOne);
    }
    return new NeqRelation(newSet);
  }

  /**
   * replace an old value with a fresh one, i.e. transform all relations from (A->OLD) towards
   * (A->FRESH) and delete OLD.
   */
  public NeqRelation replaceValueAndCopy(SMGValue fresh, SMGValue old) {
    NeqRelation result = removeValueAndCopy(old);
    for (SMGValue value : getNeqsForValue(old)) {
      result = result.addRelationAndCopy(fresh, value);
    }
    return result;
  }

  @Override
  public String toString() {
    return "neq_rel=" + smgValues;
  }

  @Override
  public int hashCode() {
    return smgValues.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    NeqRelation other = (NeqRelation) obj;
    return other.smgValues != null && smgValues.equals(other.smgValues);
  }
}
