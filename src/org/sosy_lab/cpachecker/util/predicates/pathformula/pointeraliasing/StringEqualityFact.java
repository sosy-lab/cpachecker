// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

/**
 * Symbolic knowledge that the strings starting at two addresses are equal, as established by {@code
 * strcpy(first, second)}. This lets {@code strcmp} be encoded precisely without copying the
 * contents of the string byte by byte.
 *
 * <p>The fact only holds as long as {@link #valid()} holds. It is initially the condition that the
 * copied string was terminated within the bound of the encoding, and every later write to memory
 * conjoins that it did not touch either of the two strings.
 *
 * @param first address of the first string
 * @param second address of the second string
 * @param size number of characters of both strings including the terminating null character
 * @param valid condition under which the fact holds
 */
record StringEqualityFact(Formula first, Formula second, Formula size, BooleanFormula valid) {

  StringEqualityFact {
    checkNotNull(first);
    checkNotNull(second);
    checkNotNull(size);
    checkNotNull(valid);
  }

  /** Whether this fact holds for the string at the given address. */
  BooleanFormula isAbout(Formula address, FormulaManagerView fmgr) {
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    return bfmgr.and(
        valid, bfmgr.or(fmgr.makeEqual(address, first), fmgr.makeEqual(address, second)));
  }

  /** Whether this fact says that the strings at the two given addresses are equal. */
  BooleanFormula isAbout(Formula address1, Formula address2, FormulaManagerView fmgr) {
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    return bfmgr.and(
        valid,
        bfmgr.or(
            bfmgr.and(fmgr.makeEqual(address1, first), fmgr.makeEqual(address2, second)),
            bfmgr.and(fmgr.makeEqual(address1, second), fmgr.makeEqual(address2, first))));
  }

  /**
   * Returns the facts that still hold after a write of {@code writeSize} bytes at {@code
   * writeAddress}, or of arbitrarily many bytes if {@code writeIsBounded} does not hold.
   */
  static PersistentList<StringEqualityFact> afterWrite(
      PersistentList<StringEqualityFact> facts,
      Formula writeAddress,
      Formula writeSize,
      BooleanFormula writeIsBounded,
      FormulaManagerView fmgr) {
    if (facts.isEmpty()) {
      return facts;
    }
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    PersistentList<StringEqualityFact> result = PersistentLinkedList.of();
    for (StringEqualityFact fact : facts.reversed()) {
      BooleanFormula valid =
          bfmgr.and(
              fact.valid(),
              writeIsBounded,
              disjoint(writeAddress, writeSize, fact.first(), fact.size(), fmgr),
              disjoint(writeAddress, writeSize, fact.second(), fact.size(), fmgr));
      result = result.with(new StringEqualityFact(fact.first(), fact.second(), fact.size(), valid));
    }
    return result;
  }

  /** The ranges [a, a+aSize) and [b, b+bSize) do not overlap. */
  private static BooleanFormula disjoint(
      Formula a, Formula aSize, Formula b, Formula bSize, FormulaManagerView fmgr) {
    return fmgr.getBooleanFormulaManager()
        .or(
            fmgr.makeLessOrEqual(fmgr.makePlus(a, aSize), b, false),
            fmgr.makeLessOrEqual(fmgr.makePlus(b, bSize), a, false));
  }
}
