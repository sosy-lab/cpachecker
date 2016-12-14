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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import com.google.common.base.Joiner;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.Pair;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public final class PointerTargetSet implements Serializable {

  /**
   * The objects of the class are used to keep the set of currently tracked fields in a {@link PersistentSortedMap}.
   * Objects of {@link CompositeField} are used as keys and place-holders of type {@link Boolean} are used as values.
   * <p>
   * This allows one to check if a particular field is tracked using a temporary object of {@link CompositeField} and
   * keep the set of currently tracked fields in rather simple way (no special-case merging is required).
   * </p>
   */
  @Immutable
  static class CompositeField implements Comparable<CompositeField>, Serializable {

    private static final long serialVersionUID = -5194535211223682619L;

    private CompositeField(final String compositeType, final String fieldName) {
      this.compositeType = compositeType;
      this.fieldName = fieldName;
    }

    static CompositeField of(final String compositeType, final String fieldName) {
      return new CompositeField(compositeType, fieldName);
    }

    @Override
    public String toString() {
      return compositeType + "." + fieldName;
    }

    @Override
    public int compareTo(final CompositeField other) {
      return ComparisonChain.start()
          .compare(this.compositeType, other.compositeType)
          .compare(this.fieldName, other.fieldName)
          .result();
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      } else if (!(obj instanceof CompositeField)) {
        return false;
      } else {
        CompositeField other = (CompositeField) obj;
        return compositeType.equals(other.compositeType) && fieldName.equals(other.fieldName);
      }
    }

    @Override
    public int hashCode() {
      return compositeType.hashCode() * 17 + fieldName.hashCode();
    }

    private final String compositeType;
    private final String fieldName;
  }

  public static String getBaseName(final String name) {
    return BASE_PREFIX + name;
  }

  static boolean isBaseName(final String name) {
    return name.startsWith(BASE_PREFIX);
  }

  static String getBase(final String baseName) {
    return baseName.replaceFirst(BASE_PREFIX, "");
  }

  PersistentList<PointerTarget> getAllTargets(final String regionName) {
    return targets.getOrDefault(regionName, PersistentLinkedList.of());
  }

  public static PointerTargetSet emptyPointerTargetSet() {
    return EMPTY_INSTANCE;
  }

  boolean isEmpty() {
    return bases.isEmpty() && fields.isEmpty()
        && lastBase == null && deferredAllocations.isEmpty();
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
    result = prime * result + Objects.hashCode(lastBase);
    result = prime * result + deferredAllocations.hashCode();
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
      return Objects.equals(lastBase, other.lastBase)
          && bases.equals(other.bases)
          && fields.equals(other.fields)
          && deferredAllocations.equals(other.deferredAllocations);
    }
  }

  PointerTargetSet(
      final PersistentSortedMap<String, CType> bases,
      final @Nullable String lastBase,
      final PersistentSortedMap<CompositeField, Boolean> fields,
      final PersistentList<Pair<String, DeferredAllocation>> deferredAllocations,
      final PersistentSortedMap<String, PersistentList<PointerTarget>> targets) {
    this.bases = bases;
    this.lastBase = lastBase;
    this.fields = fields;

    this.deferredAllocations = deferredAllocations;

    this.targets = targets;

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

  @Nullable
  String getLastBase() {
    return lastBase;
  }

  private static final PointerTargetSet EMPTY_INSTANCE =
      new PointerTargetSet(
          PathCopyingPersistentTreeMap.<String, CType>of(),
          null,
          PathCopyingPersistentTreeMap.<CompositeField, Boolean>of(),
          PersistentLinkedList.<Pair<String, DeferredAllocation>>of(),
          PathCopyingPersistentTreeMap.<String, PersistentList<PointerTarget>>of());

  private static final Joiner joiner = Joiner.on(" ");

  // The set of known memory objects.
  // This includes allocated memory regions and global/local structs/arrays.
  // The key of the map is the name of the base (without the BASE_PREFIX).
  // There are also "fake" bases in the map for variables that have their address
  // taken somewhere but are not yet tracked.
  private final PersistentSortedMap<String, CType> bases;

  // The last added memory region (used to create the chain of inequalities between bases).
  private final @Nullable String lastBase;

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

  private static final String BASE_PREFIX = "__ADDRESS_OF_";

  private static final long serialVersionUID = 2102505458322248624L;

  private Object writeReplace() {
    return new SerializationProxy(this);
  }

  /**
   * javadoc to remove unused parameter warning
   * @param in the input stream
   */
  private void readObject(ObjectInputStream in) throws IOException {
    throw new InvalidObjectException("Proxy required");
  }

  private static class SerializationProxy implements Serializable {

    private static final long serialVersionUID = 8022025017590667769L;
    private final PersistentSortedMap<String, CType> bases;
    private final String lastBase;
    private final PersistentSortedMap<CompositeField, Boolean> fields;
    private final List<Pair<String, DeferredAllocation>> deferredAllocations;
    private final Map<String, List<PointerTarget>> targets;

    private SerializationProxy(PointerTargetSet pts) {
      bases = pts.bases;
      lastBase = pts.lastBase;
      fields = pts.fields;
      List<Pair<String, DeferredAllocation>> deferredAllocations =
          Lists.newArrayList(pts.deferredAllocations);
      this.deferredAllocations = deferredAllocations;
      Map<String, List<PointerTarget>> targets =
          Maps.newHashMapWithExpectedSize(pts.targets.size());
      for(Entry<String, PersistentList<PointerTarget>> entry : pts.targets.entrySet()) {
        targets.put(entry.getKey(), new ArrayList<>(entry.getValue()));
      }
      this.targets = targets;
    }

    private Object readResolve() {
      Map<String, PersistentList<PointerTarget>> targets =
          Maps.newHashMapWithExpectedSize(this.targets.size());
      for (Entry<String, List<PointerTarget>> entry : this.targets.entrySet()) {
        targets.put(entry.getKey(), PersistentLinkedList.copyOf(entry.getValue()));
      }
      return new PointerTargetSet(
          bases,
          lastBase,
          fields,
          PersistentLinkedList.copyOf(deferredAllocations),
          PathCopyingPersistentTreeMap.copyOf(targets));
    }
  }
}
