// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdge;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter.SMGEdgeHasValueFilterByObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsToFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentMultimap;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;

/**
 * A graph-based representation of memory-structures. The most important part is the bipartite
 * directed graph of {@link SMGValue}s and {@link SMGObject}s connected by {@link SMGEdge}s.
 */
public class SMG implements UnmodifiableSMG {
  private PersistentSet<SMGObject> objects;
  private PersistentSet<SMGValue> values;
  private SMGHasValueEdges hv_edges;
  private SMGPointsToEdges pt_edges;

  /** {@link #validObjects} is a subset of {@link #objects} */
  private PersistentSet<SMGObject> validObjects;

  private PersistentSet<SMGObject> externalObjectAllocation;
  private NeqRelation neq = new NeqRelation();
  private PersistentMultimap<SMGObject, SMGObject> possibleEquals;

  private final SMGPredicateRelation pathPredicate = new SMGPredicateRelation();
  private SMGPredicateRelation errorPredicate = new SMGPredicateRelation();

  private final MachineModel machine_model;

  private static final SMGEdgePointsTo NULL_POINTER =
      new SMGEdgePointsTo(SMGZeroValue.INSTANCE, SMGNullObject.INSTANCE, 0);

  /**
   * Constructor.
   *
   * <p>Consistent after call: yes.
   *
   * @param pMachineModel A machine model this SMG uses.
   */
  @VisibleForTesting
  public SMG(final MachineModel pMachineModel) {
    objects = PersistentSet.of();
    values = PersistentSet.of();
    hv_edges = new SMGHasValueEdgeSet();
    pt_edges = new SMGPointsToMap();
    validObjects = PersistentSet.of();
    externalObjectAllocation = PersistentSet.of();
    machine_model = pMachineModel;
    possibleEquals = PersistentMultimap.of();

    initializeNullObject();
    initializeNullAddress();
  }

  /**
   * Copy constructor.
   *
   * <p>Consistent after call: yes if pHeap is consistent, no otherwise.
   *
   * @param pHeap Original SMG.
   */
  protected SMG(final SMG pHeap) {
    machine_model = pHeap.machine_model;
    hv_edges = pHeap.hv_edges;
    pt_edges = pHeap.pt_edges;
    neq = pHeap.neq;
    pathPredicate.putAll(pHeap.pathPredicate);
    errorPredicate.putAll(pHeap.errorPredicate);
    validObjects = pHeap.validObjects;
    externalObjectAllocation = pHeap.externalObjectAllocation;
    objects = pHeap.objects;
    values = pHeap.values;
    possibleEquals = pHeap.possibleEquals;
  }

  @Override
  public SMG copyOf() {
    return new SMG(this);
  }

  @Override
  public final int hashCode() {
    return Objects.hash(machine_model, hv_edges, neq, validObjects, objects, pt_edges, values);
  }

  @Override
  // refactoring would be better, but currently safe for the existing subclass
  @SuppressWarnings("EqualsGetClass")
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SMG other = (SMG) obj;
    return machine_model == other.machine_model
        && Objects.equals(hv_edges, other.hv_edges)
        && Objects.equals(neq, other.neq)
        && Objects.equals(validObjects, other.validObjects)
        && Objects.equals(objects, other.objects)
        && Objects.equals(pt_edges, other.pt_edges)
        && Objects.equals(values, other.values)
        && Objects.equals(externalObjectAllocation, other.externalObjectAllocation);
  }

  /**
   * Add an object to the SMG.
   *
   * <p>Keeps consistency: no.
   *
   * @param pObj object to add.
   */
  public final void addObject(final SMGObject pObj) {
    Preconditions.checkArgument(
        SMGNullObject.INSTANCE != pObj, "NULL can not be added as valid object");
    addObject(pObj, true, false);
  }

  /**
   * Remove pValue from the SMG. This method does not remove any edges leading from/to the removed
   * value.
   *
   * <p>Keeps consistency: no
   *
   * @param pValue Value to remove
   */
  public final void removeValue(final SMGValue pValue) {
    Preconditions.checkArgument(!pValue.isZero(), "Can not remove NULL from SMG");
    values = values.removeAndCopy(pValue);
    neq = neq.removeValueAndCopy(pValue);
    pathPredicate.removeValue(pValue);
    errorPredicate.removeValue(pValue);
    assert !hv_edges.filter(SMGEdgeHasValueFilter.valueFilter(pValue)).iterator().hasNext();
  }
  /**
   * Remove pObj from the SMG. This method does not remove any edges leading from/to the removed
   * object.
   *
   * <p>Keeps consistency: no
   *
   * @param pObj Object to remove
   */
  @VisibleForTesting
  public final void removeObject(final SMGObject pObj) {
    objects = objects.removeAndCopy(pObj);
    validObjects = validObjects.removeAndCopy(pObj);
    externalObjectAllocation = externalObjectAllocation.removeAndCopy(pObj);
    for (SMGObject object : possibleEquals.get(pObj)) {
      possibleEquals = possibleEquals.removeAndCopy(object, pObj);
    }
    possibleEquals = possibleEquals.removeAndCopy(pObj);
  }

  /**
   * Mark pObj as deleted (invalid) and remove hasValueEdges leading from it from the SMG
   *
   * <p>Keeps consistency: no
   *
   * @param pObj Object to remove
   */
  public final void markObjectDeletedAndRemoveEdges(final SMGObject pObj) {
    Preconditions.checkArgument(pObj != SMGNullObject.INSTANCE, "Can not remove NULL from SMG");
    setValidity(pObj, false);
    hv_edges = hv_edges.removeAllEdgesOfObjectAndCopy(pObj);
  }

  /**
   * Add pObj object to the SMG, with validity set to pValidity.
   *
   * <p>Keeps consistency: no.
   *
   * @param pObj Object to add
   * @param pValidity Validity of the newly added object.
   */
  public final void addObject(
      final SMGObject pObj, final boolean pValidity, final boolean pExternal) {
    objects = objects.addAndCopy(pObj);
    setValidity(pObj, pValidity);
    setExternallyAllocatedFlag(pObj, pExternal);
  }

  /**
   * Add pValue value to the SMG.
   *
   * <p>Keeps consistency: no.
   *
   * @param pValue Value to add.
   */
  public final void addValue(SMGValue pValue) {
    values = values.addAndCopy(pValue);
  }

  /**
   * Add pEdge Points-To edge to the SMG.
   *
   * <p>Keeps consistency: no.
   *
   * @param pEdge Points-To edge to add.
   */
  public final void addPointsToEdge(SMGEdgePointsTo pEdge) {
    Preconditions.checkArgument(values.contains(pEdge.getValue()), "adding an edge without source");
    pt_edges = pt_edges.addAndCopy(pEdge);
  }

  /**
   * Add pEdge Has-Value edge to the SMG.
   *
   * <p>Keeps consistency: no
   *
   * @param pEdge Has-Value edge to add
   */
  public final void addHasValueEdge(SMGEdgeHasValue pEdge) {
    Preconditions.checkArgument(values.contains(pEdge.getValue()), "adding edge without target");
    hv_edges = hv_edges.addEdgeAndCopy(pEdge);
  }

  /**
   * Quick add pEdge Has-Value edge to the SMG.
   *
   * <p>Keeps consistency: no
   *
   * @param pEdgesSet Has-Value edges set to add
   */
  public void addHasValueEdges(Iterable<SMGEdgeHasValue> pEdgesSet) {
    // TODO: add values check
    hv_edges = hv_edges.addEdgesForObject(pEdgesSet);
  }
  /**
   * Remove pEdge Has-Value edge from the SMG.
   *
   * <p>Keeps consistency: no
   *
   * @param pEdge Has-Value edge to remove
   */
  public final void removeHasValueEdge(SMGEdgeHasValue pEdge) {
    hv_edges = hv_edges.removeEdgeAndCopy(pEdge);
  }

  /**
   * Remove the Points-To edge from the SMG with the value pValue as Source.
   *
   * <p>Keeps consistency: no
   *
   * @param pValue the Source of the Points-To edge to be removed
   */
  public final void removePointsToEdge(SMGValue pValue) {
    Preconditions.checkArgument(!pValue.isZero(), "Can not remove NULL from SMG");
    pt_edges = pt_edges.removeEdgeWithValueAndCopy(pValue);
  }

  /**
   * Sets the validity of the object pObject to pValidity. Throws {@link IllegalArgumentException}
   * if pObject is not present in SMG.
   *
   * <p>Keeps consistency: no
   *
   * @param pObject An object.
   * @param pValidity Validity to set.
   */
  public void setValidity(SMGObject pObject, boolean pValidity) {
    checkArgument(objects.contains(pObject), "Object [%s] not in SMG", pObject);
    if (pValidity) {
      validObjects = validObjects.addAndCopy(pObject);
    } else {
      validObjects = validObjects.removeAndCopy(pObject);
    }
  }

  /**
   * Sets the ExternallyAllocatedFlag of the object pObject to pExternal. Throws {@link
   * IllegalArgumentException} if pObject is not present in SMG.
   *
   * <p>Keeps consistency: no
   *
   * @param pObject An object.
   * @param pExternal Validity to set.
   */
  public void setExternallyAllocatedFlag(SMGObject pObject, boolean pExternal) {
    checkArgument(objects.contains(pObject), "Object [%s] not in SMG", pObject);
    if (pExternal) {
      externalObjectAllocation = externalObjectAllocation.addAndCopy(pObject);
    } else {
      externalObjectAllocation = externalObjectAllocation.removeAndCopy(pObject);
    }
  }

  /** Adds a neq relation between two values to the SMG Keeps consistency: no */
  public void addNeqRelation(SMGValue pV1, SMGValue pV2) {
    neq = neq.addRelationAndCopy(pV1, pV2);
  }

  @Override
  public SMGPredicateRelation getPathPredicateRelation() {
    return pathPredicate;
  }

  @Override
  public SMGPredicateRelation getErrorPredicateRelation() {
    return errorPredicate;
  }

  public void resetErrorRelation() {
    errorPredicate = new SMGPredicateRelation();
  }

  /* ********************************************* */
  /* Non-modifying functions: getters and the like */
  /* ********************************************* */

  /**
   * Getter for obtaining unmodifiable view on values set. Constant.
   *
   * @return Unmodifiable view on values set.
   */
  @Override
  public final PersistentSet<SMGValue> getValues() {
    return values;
  }

  /**
   * Getter for obtaining unmodifiable view on objects set. Constant.
   *
   * @return Unmodifiable view on objects set.
   */
  @Override
  public final PersistentSet<SMGObject> getObjects() {
    return objects;
  }

  /**
   * Getter for obtaining unmodifiable view on valid objects set. Constant.
   *
   * @return Unmodifiable view on valid objects set.
   */
  @Override
  public final PersistentSet<SMGObject> getValidObjects() {
    return validObjects;
  }

  /**
   * Getter for obtaining unmodifiable view on Has-Value edges set. Constant.
   *
   * @return Unmodifiable view on Has-Value edges set.
   */
  @Override
  public final SMGHasValueEdges getHVEdges() {
    return hv_edges;
  }

  /**
   * Getter for obtaining unmodifiable view on Has-Value edges set, filtered by a certain set of
   * criteria.
   *
   * @param pFilter Filtering object
   * @return A set of Has-Value edges for which the criteria in p hold
   */
  @Override
  public final Iterable<SMGEdgeHasValue> getHVEdges(SMGEdgeHasValueFilter pFilter) {
    return pFilter.filter(hv_edges);
  }

  @Override
  public final SMGHasValueEdges getHVEdges(SMGEdgeHasValueFilterByObject pFilter) {
    return pFilter.filter(hv_edges);
  }

  @Override
  public Set<SMGEdgePointsTo> getPtEdges(SMGEdgePointsToFilter pFilter) {
    return ImmutableSet.copyOf(pFilter.filter(pt_edges));
  }

  @Override
  public SMGPointsToEdges getPTEdges() {
    return pt_edges;
  }

  /**
   * Getter for obtaining an object, pointed by a value pValue. Constant.
   *
   * @param pValue An origin value.
   * @return The object pointed by the value pValue, if such exists. Null, if pValue does not point
   *     to any object.
   *     <p>Throws {@link IllegalArgumentException} if pValue is not present in the SMG.
   *     <p>TODO: Test TODO: Consistency check: no value can point to more objects
   */
  @Override
  public final @Nullable SMGObject getObjectPointedBy(SMGValue pValue) {
    // TODO: precondition check is slow because values size is commonly bigger then pt_edges size
    checkArgument(values.contains(pValue), "Value [%s] not in SMG", pValue);
    SMGEdgePointsTo ptEdge = pt_edges.getEdgeWithValue(pValue);
    if (ptEdge != null) {
      return ptEdge.getObject();
    } else {
      return null;
    }
  }

  /**
   * Getter for determining if the object pObject is valid. Constant. Throws {@link
   * IllegalArgumentException} if pObject is not present in the SMG.
   *
   * @param pObject An object.
   * @return True if Object is valid, False if it is invalid.
   */
  @Override
  public final boolean isObjectValid(SMGObject pObject) {
    // Preconditions.checkArgument(objects.contains(pObject), "Object [" + pObject + "] not in
    // SMG");
    return validObjects.contains(pObject);
  }

  /**
   * Getter for determing if the object pObject is externally allocated Throws {@link
   * IllegalAccessException} if pObject is not present is the SMG
   */
  @Override
  public final boolean isObjectExternallyAllocated(SMGObject pObject) {
    // Preconditions.checkArgument(objects.contains(pObject), "Object [" + pObject + "] not in
    // SMG");
    return externalObjectAllocation.contains(pObject);
  }

  /**
   * Getter for obtaining SMG machine model. Constant.
   *
   * @return SMG machine model
   */
  @Override
  public final MachineModel getMachineModel() {
    return machine_model;
  }

  /**
   * Getter for obtaining size of pointer. Constant.
   *
   * @return Size of pointer in bits
   */
  @Override
  public final int getSizeofPtrInBits() {
    return machine_model.getSizeofPtrInBits();
  }

  /**
   * Obtains a TreeMap offset to size signifying where the object bytes are nullified.
   *
   * <p>Constant.
   *
   * <p>Example: an entry "{0:32,48:16}" represents a region of at least 64b with two ZERO values
   * located at offset 0 (length 32) and 48 (length 16). We assure that the returned intervals do
   * not overlap, i.e., we never return something like {0:32,16:32} or {0:32,32:32}, but would
   * return {0:48} or {0:64} here.
   *
   * @param pObj SMGObject for which the information is to be obtained
   * @return A mapping of offsets to sizes which are covered by a HasValue edge leading from the
   *     object to NULL value
   */
  @Override
  public TreeMap<Long, Long> getNullEdgesMapOffsetToSizeForObject(SMGObject pObj) {

    SMGEdgeHasValueFilter nullValueFilter =
        SMGEdgeHasValueFilter.objectFilter(pObj)
            .filterHavingValue(SMGZeroValue.INSTANCE)
            .filterWithoutSize();
    TreeMap<Long, Long> offsetToSize = new TreeMap<>();
    for (SMGEdgeHasValue edge : nullValueFilter.filter(hv_edges)) {
      long offset = edge.getOffset();
      long size = edge.getSizeInBits();
      offsetToSize.put(offset, size);
    }
    return offsetToSize;
  }

  /**
   * Checks, whether a {@link SMGEdgePointsTo} edge exists with the given value as source.
   *
   * @param value the source of the {@link SMGEdgePointsTo} edge.
   * @return true, if the {@link SMGEdgePointsTo} edge with the source value exists, otherwise
   *     false.
   */
  @Override
  public boolean isPointer(SMGValue value) {
    return pt_edges.containsEdgeWithValue(value);
  }

  /**
   * Returns the {@link SMGEdgePointsTo} edge with the given value as source.
   *
   * @param value the source of the {@link SMGEdgePointsTo} edge.
   * @return the {@link SMGEdgePointsTo} edge with the value as source.
   */
  @Override
  public SMGEdgePointsTo getPointer(SMGValue value) {
    return pt_edges.getEdgeWithValue(value);
  }

  @Override
  public boolean isCoveredByNullifiedBlocks(SMGEdgeHasValue pEdge) {
    return isCoveredByNullifiedBlocks(pEdge.getObject(), pEdge.getOffset(), pEdge.getSizeInBits());
  }

  private boolean isCoveredByNullifiedBlocks(SMGObject pObject, long pOffset, long size) {
    long expectedMinClear = pOffset + size;

    TreeMap<Long, Long> nullEdgesOffsetToSize = getNullEdgesMapOffsetToSizeForObject(pObject);
    Entry<Long, Long> floorEntry = nullEdgesOffsetToSize.floorEntry(pOffset);
    return (floorEntry != null && floorEntry.getValue() + floorEntry.getKey() >= expectedMinClear);
  }

  /**
   * replace the old value with a fresh one.
   *
   * <p>deletes the old value from the SMG and redirects all HV- and PT-edges.
   *
   * <p>Precondition: the old value must never be ZERO.
   */
  public void replaceValue(SMGValue fresh, SMGValue old) {
    if (fresh == old) {
      return;
    }

    Preconditions.checkArgument(
        !old.isZero(), "cannot replace ZERO (%s) with other value (%s)", old, fresh);

    addValue(fresh);

    neq = neq.replaceValueAndCopy(fresh, old);
    pathPredicate.replace(fresh, old);
    errorPredicate.replace(fresh, old);

    for (SMGEdgeHasValue old_hve : getHVEdges(SMGEdgeHasValueFilter.valueFilter(old))) {
      SMGEdgeHasValue newHvEdge =
          new SMGEdgeHasValue(
              old_hve.getSizeInBits(), old_hve.getOffset(), old_hve.getObject(), fresh);
      hv_edges = hv_edges.removeEdgeAndCopy(old_hve);
      hv_edges = hv_edges.addEdgeAndCopy(newHvEdge);
    }

    if (pt_edges.containsEdgeWithValue(old)) {
      SMGEdgePointsTo pt_edge = pt_edges.getEdgeWithValue(old);
      pt_edges = pt_edges.removeAndCopy(pt_edge);
      // Workaround for removed object
      if (pt_edges.containsEdgeWithValue(fresh) && !fresh.isZero()) {
        assert !getHVEdges(SMGEdgeHasValueFilter.valueFilter(fresh)).iterator().hasNext();
        pt_edges = pt_edges.removeEdgeWithValueAndCopy(fresh);
      }
      Preconditions.checkArgument(
          !pt_edges.containsEdgeWithValue(fresh)
              || fresh.equals(SMGZeroValue.INSTANCE)
              || (old instanceof SMGKnownAddressValue
                  && !isObjectValid(((SMGKnownAddressValue) old).getObject())));
      pt_edges =
          pt_edges.addAndCopy(
              new SMGEdgePointsTo(
                  fresh, pt_edge.getObject(), pt_edge.getOffset(), pt_edge.getTargetSpecifier()));
    }

    removeValue(old);
  }

  @Override
  public boolean haveNeqRelation(SMGValue pV1, SMGValue pV2) {
    return neq.neq_exists(pV1, pV2);
  }

  @Override
  public Set<SMGValue> getNeqsForValue(SMGValue pV) {
    return neq.getNeqsForValue(pV);
  }

  protected void clearValuesHvePte() {
    values = PersistentSet.of();
    hv_edges = new SMGHasValueEdgeSet();
    pt_edges = new SMGPointsToMap();
    neq = new NeqRelation();
    pathPredicate.clear();
    initializeNullAddress();
  }

  private void initializeNullAddress() {
    addValue(SMGZeroValue.INSTANCE);
    addPointsToEdge(NULL_POINTER);
  }

  public void clearObjects() {
    objects = PersistentSet.of();
    validObjects = PersistentSet.of();
    initializeNullObject();
  }

  private void initializeNullObject() {
    objects = objects.addAndCopy(SMGNullObject.INSTANCE);
  }

  public void addPossibleEqualObjects(SMGObject pObject1, SMGObject pObject2) {
    assert !SMGNullObject.INSTANCE.equals(pObject1);
    assert !SMGNullObject.INSTANCE.equals(pObject2);
    possibleEquals = possibleEquals.putAndCopy(pObject1, pObject2);
    possibleEquals = possibleEquals.putAndCopy(pObject2, pObject1);
  }

  public boolean arePossibleEquals(SMGObject pObject1, SMGObject pObject2) {
    return possibleEquals.containsEntry(pObject1, pObject2);
  }
}
