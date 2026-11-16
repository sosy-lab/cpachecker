// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

/**
 * Symbolic knowledge that a block of memory was zeroed by its allocation (e.g., by {@code calloc})
 * and was changed since only by the given writes. A read of any type from bytes of the block that
 * none of these writes covered thus yields zero.
 *
 * <p>This is needed because the heap is encoded separately for each type, so zeroing the block for
 * the type it is allocated with does not zero it for reads with other types.
 *
 * @param start address of the block
 * @param size number of bytes of the block. This is a variable that is only constrained on paths
 *     with the allocation, such that the fact has no effect on other paths after a merge.
 * @param writes the writes to memory since the allocation
 */
record ZeroedMemoryFact(Formula start, Formula size, PersistentList<Write> writes) {

  /** A write of {@code size} bytes at {@code address}. */
  record Write(Formula address, long size) {}

  /** A fact with more writes is dropped, which limits the size of the read constraints. */
  private static final int MAX_WRITES = 100;

  /** Only the newest facts are kept, which limits the size of the read constraints. */
  private static final int MAX_FACTS = 20;

  ZeroedMemoryFact {
    checkNotNull(start);
    checkNotNull(size);
    checkNotNull(writes);
  }

  /** Returns the given facts with the new fact of a freshly zeroed block. */
  static PersistentList<ZeroedMemoryFact> add(
      PersistentList<ZeroedMemoryFact> facts, Formula start, Formula size) {
    return limit(facts.with(new ZeroedMemoryFact(start, size, PersistentLinkedList.of())));
  }

  /** Returns the facts that hold after a write of {@code size} bytes at {@code address}. */
  static PersistentList<ZeroedMemoryFact> afterWrite(
      PersistentList<ZeroedMemoryFact> facts, Formula address, long size) {
    if (facts.isEmpty()) {
      return facts;
    }
    Write write = new Write(address, size);
    PersistentList<ZeroedMemoryFact> result = PersistentLinkedList.of();
    for (ZeroedMemoryFact fact : facts.reversed()) {
      if (fact.writes().size() < MAX_WRITES) {
        result =
            result.with(new ZeroedMemoryFact(fact.start(), fact.size(), fact.writes().with(write)));
      }
    }
    return result;
  }

  /**
   * Merges the facts of two paths. A fact of only one path is kept because its size is
   * unconstrained on the other path. A fact of both paths gets the writes of both paths.
   */
  static PersistentList<ZeroedMemoryFact> merge(
      PersistentList<ZeroedMemoryFact> facts1, PersistentList<ZeroedMemoryFact> facts2) {
    if (facts1.equals(facts2)) {
      return facts1;
    }
    Map<Formula, ZeroedMemoryFact> onlyIn2 = new HashMap<>();
    for (ZeroedMemoryFact fact : facts2) {
      onlyIn2.put(fact.size(), fact);
    }
    List<ZeroedMemoryFact> result = new ArrayList<>();
    for (ZeroedMemoryFact fact1 : facts1) {
      ZeroedMemoryFact fact2 = onlyIn2.remove(fact1.size());
      if (fact2 == null) {
        result.add(fact1);
      } else if (fact1.start().equals(fact2.start())) {
        Set<Write> writes1 = new HashSet<>(fact1.writes());
        PersistentList<Write> writes = fact1.writes();
        for (Write write : fact2.writes()) {
          if (!writes1.contains(write)) {
            writes = writes.with(write);
          }
        }
        if (writes.size() <= MAX_WRITES) {
          result.add(new ZeroedMemoryFact(fact1.start(), fact1.size(), writes));
        }
      }
    }
    result.addAll(onlyIn2.values());
    return limit(PersistentLinkedList.copyOf(result));
  }

  private static PersistentList<ZeroedMemoryFact> limit(PersistentList<ZeroedMemoryFact> facts) {
    if (facts.size() <= MAX_FACTS) {
      return facts;
    }
    return PersistentLinkedList.copyOf(ImmutableList.copyOf(facts.subList(0, MAX_FACTS)));
  }

  /**
   * Returns the constraint that {@code value}, which was read from {@code readSize} bytes at {@code
   * address}, is zero if these bytes are still zero according to one of the facts.
   */
  static BooleanFormula makeReadConstraint(
      PersistentList<ZeroedMemoryFact> facts,
      Formula address,
      long readSize,
      Formula value,
      FormulaManagerView fmgr) {
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    FormulaType<?> valueType = fmgr.getFormulaType(value);
    FormulaType<?> addressType = fmgr.getFormulaType(address);
    if (!(valueType.isBitvectorType()
        || valueType.isIntegerType()
        || valueType.isRationalType()
        || valueType.isFloatingPointType())) {
      return bfmgr.makeTrue();
    }
    Formula readSizeFormula = fmgr.makeNumber(addressType, readSize);

    Map<Write, BooleanFormula> overlaps = new HashMap<>();
    List<BooleanFormula> zeroByFact = new ArrayList<>();
    for (ZeroedMemoryFact fact : facts) {
      if (!fmgr.getFormulaType(fact.start()).equals(addressType)
          || !fmgr.getFormulaType(fact.size()).equals(addressType)) {
        continue;
      }
      List<BooleanFormula> conditions = new ArrayList<>();
      conditions.add(
          contains(fact.start(), fact.size(), address, readSizeFormula, addressType, fmgr));
      for (Write write : fact.writes()) {
        BooleanFormula overlap =
            overlaps.computeIfAbsent(
                write,
                w ->
                    overlap(
                        w.address(),
                        fmgr.makeNumber(addressType, w.size()),
                        address,
                        readSizeFormula,
                        addressType,
                        fmgr));
        conditions.add(bfmgr.not(overlap));
      }
      zeroByFact.add(bfmgr.and(conditions));
    }
    if (zeroByFact.isEmpty()) {
      return bfmgr.makeTrue();
    }
    return bfmgr.implication(
        bfmgr.or(zeroByFact), fmgr.assignment(value, fmgr.makeNumber(valueType, 0)));
  }

  /**
   * Whether [b, b+bSize) lies within [a, a+aSize). For bitvectors, this is computed with
   * wrap-around arithmetic, which is exact also for blocks at the end of the address space.
   */
  private static BooleanFormula contains(
      Formula a,
      Formula aSize,
      Formula b,
      Formula bSize,
      FormulaType<?> type,
      FormulaManagerView fmgr) {
    if (type.isBitvectorType()) {
      Formula offset = fmgr.makeMinus(b, a);
      return fmgr.getBooleanFormulaManager()
          .and(
              fmgr.makeLessThan(offset, aSize, false),
              fmgr.makeLessOrEqual(bSize, fmgr.makeMinus(aSize, offset), false));
    }
    return fmgr.getBooleanFormulaManager()
        .and(
            fmgr.makeLessOrEqual(a, b, false),
            fmgr.makeLessOrEqual(fmgr.makePlus(b, bSize), fmgr.makePlus(a, aSize), false));
  }

  /** Whether [a, a+aSize) and [b, b+bSize) overlap, for sizes greater than zero. */
  private static BooleanFormula overlap(
      Formula a,
      Formula aSize,
      Formula b,
      Formula bSize,
      FormulaType<?> type,
      FormulaManagerView fmgr) {
    if (type.isBitvectorType()) {
      return fmgr.getBooleanFormulaManager()
          .or(
              fmgr.makeLessThan(fmgr.makeMinus(b, a), aSize, false),
              fmgr.makeLessThan(fmgr.makeMinus(a, b), bSize, false));
    }
    return fmgr.getBooleanFormulaManager()
        .and(
            fmgr.makeLessThan(a, fmgr.makePlus(b, bSize), false),
            fmgr.makeLessThan(b, fmgr.makePlus(a, aSize), false));
  }
}
