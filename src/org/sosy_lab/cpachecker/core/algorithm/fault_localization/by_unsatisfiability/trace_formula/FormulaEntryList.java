// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula;

import com.google.common.collect.ForwardingList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaEntryList.FormulaEntry;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class FormulaEntryList extends ForwardingList<FormulaEntry> {

  private List<FormulaEntry> entries;

  public FormulaEntryList() {
    entries = new ArrayList<>();
  }

  public FormulaEntryList(List<FormulaEntry> pList) {
    entries = new ArrayList<>(pList);
  }

  public void addEntry(
      int pos, int pAtomId, SSAMap pSSAMap, Selector pSelector, BooleanFormula pAtom) {
    entries.add(pos, new FormulaEntry(pAtomId, pSSAMap, pSelector, pAtom));
  }

  public void addEntry(int pAtomId, SSAMap pSSAMap, Selector pSelector, BooleanFormula pAtom) {
    entries.add(new FormulaEntry(pAtomId, pSSAMap, pSelector, pAtom));
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

  public List<BooleanFormula> toAtomList() {
    return toTList(entry -> entry.atom);
  }

  public List<SSAMap> toSSAMapList() {
    return toTList(entry -> entry.map);
  }

  public List<Selector> toSelectorList() {
    return toTList(entry -> entry.selector);
  }

  public List<CFAEdge> toEdgeList() {
    return toTList(entry -> entry.selector.getEdge());
  }

  private <T> List<T> toTList(Function<FormulaEntry, T> mapping) {
    return entries.stream()
        .map(mapping::apply)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Override
  protected List<FormulaEntry> delegate() {
    return entries;
  }

  public static class FormulaEntry {

    private SSAMap map;
    private Selector selector;
    private BooleanFormula atom;
    private int atomId;

    public FormulaEntry(int pAtomId, SSAMap pSSAMap, Selector pSelector, BooleanFormula pAtom) {
      map = pSSAMap;
      selector = pSelector;
      atom = pAtom;
      atomId = pAtomId;
    }

    public int getAtomId() {
      return atomId;
    }

    public void setMap(SSAMap pMap) {
      map = pMap;
    }

    public void setSelector(Selector pSelector) {
      selector = pSelector;
    }

    public void setAtom(BooleanFormula pAtom) {
      atom = pAtom;
    }

    public Selector getSelector() {
      return selector;
    }

    public SSAMap getMap() {
      return map;
    }

    public BooleanFormula getAtom() {
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
}
