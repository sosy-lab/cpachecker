// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.Formula;

@javax.annotation.concurrent.Immutable // cannot prove deep immutability
public final class PointerTargetSet implements Serializable {

  static String getBaseName(final String name) {
    return BASE_PREFIX + name;
  }

  public static boolean isBaseName(final String name) {
    return name.startsWith(BASE_PREFIX);
  }

  public static String getBase(final String baseName) {
    assert isBaseName(baseName);
    return baseName.substring(BASE_PREFIX.length());
  }

  PersistentList<PointerTarget> getAllTargets(final String regionName) {
    return targets.getOrDefault(regionName, PersistentLinkedList.of());
  }

  public static PointerTargetSet emptyPointerTargetSet() {
    return EMPTY_INSTANCE;
  }

  boolean isEmpty() {
    return bases.isEmpty()
        && fields.isEmpty()
        && deferredAllocations.isEmpty()
        && highestAllocatedAddresses.isEmpty()
        && allocationCount == 0;
  }

  @Override
  public String toString() {
    return joiner.join(bases.entrySet()) + " " + joiner.join(fields.entrySet());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + bases.hashCode();
    result = prime * result + fields.hashCode();
    result = prime * result + deferredAllocations.hashCode();
    result = prime * result + highestAllocatedAddresses.hashCode();
    result = prime * result + Integer.hashCode(allocationCount);
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof PointerTargetSet)) {
      return false;
    } else {
      PointerTargetSet other = (PointerTargetSet) obj;
      // No need to check for equality of targets
      // because if bases and fields are equal, targets is equal, too.
      return bases.equals(other.bases)
          && fields.equals(other.fields)
          && deferredAllocations.equals(other.deferredAllocations)
          && highestAllocatedAddresses.equals(other.getHighestAllocatedAddresses())
          && allocationCount == other.allocationCount;
    }
  }

  PointerTargetSet(
      final PersistentSortedMap<String, CType> bases,
      final PersistentSortedMap<CompositeField, Boolean> fields,
      final PersistentList<Pair<String, DeferredAllocation>> deferredAllocations,
      final PersistentSortedMap<String, PersistentList<PointerTarget>> targets,
      final PersistentList<Formula> pHighestAllocatedAddresess,
      final int pAllocationCount) {
    this.bases = bases;
    this.fields = fields;

    this.deferredAllocations = deferredAllocations;

    this.targets = targets;
    highestAllocatedAddresses = pHighestAllocatedAddresess;
    allocationCount = pAllocationCount;

    if (isEmpty()) {
      // Inside isEmpty(), we do not check the following the targets field.
      // so we assert here that isEmpty() implies that it is also empty.
      assert targets.isEmpty();
    }
  }

  public PersistentSortedMap<String, CType> getBases() {
    return bases;
  }

  /**
   * Returns, if a variable is the actual base of a pointer.
   *
   * @param name The name of the variable.
   * @return True, if the variable is an actual base, false otherwise.
   */
  public boolean isActualBase(final String name) {
    return bases.containsKey(name) && !PointerTargetSetManager.isFakeBaseType(bases.get(name));
  }

  PersistentSortedMap<CompositeField, Boolean> getFields() {
    return fields;
  }

  PersistentList<Pair<String, DeferredAllocation>> getDeferredAllocations() {
    return deferredAllocations;
  }

  PersistentSortedMap<String, PersistentList<PointerTarget>> getTargets() {
    return targets;
  }

  /**
   * Get the highest allocated addresses, i.e., which guarantee that a fresh address that is larger
   * than all addresses returned here was previously not yet allocated.
   */
  PersistentList<Formula> getHighestAllocatedAddresses() {
    return highestAllocatedAddresses;
  }

  /** Get the number of allocations of memory on the heap. */
  int getAllocationCount() {
    return allocationCount;
  }

  private static final PointerTargetSet EMPTY_INSTANCE =
      new PointerTargetSet(
          PathCopyingPersistentTreeMap.of(),
          PathCopyingPersistentTreeMap.of(),
          PersistentLinkedList.of(),
          PathCopyingPersistentTreeMap.of(),
          PersistentLinkedList.of(),
          0);

  private static final Joiner joiner = Joiner.on(" ");

  // The set of known memory objects.
  // This includes allocated memory regions and global/local structs/arrays.
  // The key of the map is the name of the base (without the BASE_PREFIX).
  // There are also "fake" bases in the map for variables that have their address
  // taken somewhere but are not yet tracked.
  private final PersistentSortedMap<String, CType> bases;

  // The set of "shared" fields that are accessed directly via pointers,
  // so they are represented with UFs instead of as variables.
  private final PersistentSortedMap<CompositeField, Boolean> fields;

  private final PersistentList<Pair<String, DeferredAllocation>> deferredAllocations;

  // The complete set of tracked memory locations.
  // The map key is the type of the memory location.
  // This set of locations is used to restore the values of the memory-access UF
  // when the SSA index is used (i.e, to create the *int@3(i) = *int@2(i) terms
  // for all values of i from this map).
  // This means that when a location is not present in this map,
  // its value is not tracked and might get lost.
  private final PersistentSortedMap<String, PersistentList<PointerTarget>> targets;

  private final PersistentList<Formula> highestAllocatedAddresses;

  private final int allocationCount;

  private static final String BASE_PREFIX = "__ADDRESS_OF_";

  private static final long serialVersionUID = 2102505458322248624L;

  private Object writeReplace() {
    return new SerializationProxy(this);
  }

  /**
   * javadoc to remove unused parameter warning
   *
   * @param in the input stream
   */
  @SuppressWarnings("UnusedVariable") // parameter is required by API
  private void readObject(ObjectInputStream in) throws IOException {
    throw new InvalidObjectException("Proxy required");
  }

  private static class SerializationProxy implements Serializable {

    private static final long serialVersionUID = 8022025017590667769L;
    private final PersistentSortedMap<String, CType> bases;
    private final PersistentSortedMap<CompositeField, Boolean> fields;
    private final List<Pair<String, DeferredAllocation>> deferredAllocations;
    private final Map<String, List<PointerTarget>> targets;
    private final List<String> highestAllocatedAddresses;
    private final int allocationCount;

    private SerializationProxy(PointerTargetSet pts) {
      bases = pts.bases;
      fields = pts.fields;
      deferredAllocations = new ArrayList<>(pts.deferredAllocations);
      targets = new HashMap<>(Maps.transformValues(pts.targets, ArrayList::new));
      FormulaManagerView mgr = GlobalInfo.getInstance().getPredicateFormulaManagerView();
      highestAllocatedAddresses =
          new ArrayList<>(
              Lists.transform(pts.highestAllocatedAddresses, mgr::dumpArbitraryFormula));
      allocationCount = pts.allocationCount;
    }

    private Object readResolve() {
      FormulaManagerView mgr = GlobalInfo.getInstance().getPredicateFormulaManagerView();
      PersistentList<Formula> highestAllocatedAddressesFormulas =
          PersistentLinkedList.copyOf(
              Lists.transform(highestAllocatedAddresses, mgr::parseArbitraryFormula));

      return new PointerTargetSet(
          bases,
          fields,
          PersistentLinkedList.copyOf(deferredAllocations),
          PathCopyingPersistentTreeMap.copyOf(
              Maps.transformValues(targets, PersistentLinkedList::copyOf)),
          highestAllocatedAddressesFormulas,
          allocationCount);
    }
  }

  public boolean hasEmptyDeferredAllocationsSet() {
    return deferredAllocations.isEmpty();
  }
}
