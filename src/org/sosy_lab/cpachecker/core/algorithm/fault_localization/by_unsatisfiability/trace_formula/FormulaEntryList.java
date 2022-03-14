// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula;

import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingList;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaEntryList.FormulaEntry;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class FormulaEntryList extends ForwardingList<FormulaEntry> {

  private final List<FormulaEntry> entries;

  public FormulaEntryList() {
    entries = new ArrayList<>();
  }

  public FormulaEntryList(List<FormulaEntry> pList) {
    entries = new ArrayList<>(pList);
  }

  public void addEntry(int pAtomId, SSAMap pSSAMap, Selector pSelector, BooleanFormula pAtom) {
    entries.add(new FormulaEntry(pAtomId, pSSAMap, pSelector, pAtom));
  }

  public void addEntry(int pos, FormulaEntry entry) {
    entries.add(pos, entry);
  }

  public void addEntry(FormulaEntry entry) {
    entries.add(entry);
  }

  /**
   * If predicate remove holds on an entry, remove it from the set map it to T with <code>extract
   * </code> and return all mapped elements as a list to the user.
   *
   * @param remove predicate to test if entry should be removed
   * @param extract function to extract a certain type out of the FormulaEntry
   * @param <T> Type to extract
   * @return List of all removed entries mapped to the correct type.
   */
  public <T> List<T> removeExtract(
      Predicate<FormulaEntry> remove, Function<FormulaEntry, T> extract) {
    List<T> values = new ArrayList<>();
    for (int i = entries.size() - 1; i >= 0; i--) {
      FormulaEntry entry = entries.get(i);
      if (remove.test(entry)) {
        values.add(extract.apply(entry));
        remove(i);
      }
    }
    return values;
  }

  public ImmutableList<BooleanFormula> toAtomList() {
    return toTStream(entry -> entry.atom).collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<SSAMap> toSSAMapList() {
    return toTStream(entry -> entry.map).collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<Selector> toSelectorList() {
    return toTStream(entry -> entry.selector).collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<CFAEdge> toEdgeList() {
    return toTStream(entry -> entry.selector)
        .map(Selector::correspondingEdge)
        .collect(ImmutableList.toImmutableList());
  }

  private <T> Stream<T> toTStream(Function<FormulaEntry, T> mapping) {
    return entries.stream().map(mapping).filter(Objects::nonNull);
  }

  @Override
  protected List<FormulaEntry> delegate() {
    return entries;
  }

  public static class FormulaEntry {

    private final SSAMap map;
    private final @Nullable Selector selector;
    private @Nullable BooleanFormula atom;
    private final int atomId;

    public FormulaEntry(int pAtomId, SSAMap pSSAMap, Selector pSelector, BooleanFormula pAtom) {
      Preconditions.checkNotNull(pSSAMap);
      map = pSSAMap;
      selector = pSelector;
      atom = pAtom;
      atomId = pAtomId;
    }

    public int getAtomId() {
      return atomId;
    }

    public void setAtom(BooleanFormula pAtom) {
      atom = pAtom;
    }

    public @Nullable Selector getSelector() {
      return selector;
    }

    public SSAMap getMap() {
      return map;
    }

    public @Nullable BooleanFormula getAtom() {
      return atom;
    }

    @Override
    public boolean equals(Object pO) {
      if (!(pO instanceof FormulaEntry)) {
        return false;
      }
      FormulaEntry that = (FormulaEntry) pO;
      return atomId == that.atomId;
    }

    @Override
    public String toString() {
      return "FormulaEntry{" + "atomId=" + atomId + ", selector=" + selector + '}';
    }

    @Override
    public int hashCode() {
      return Objects.hash(atomId);
    }
  }

  public static class PreconditionEntry extends FormulaEntry {

    public PreconditionEntry(SSAMap pSSAMap) {
      super(-1, pSSAMap, null, null);
    }
  }
}
