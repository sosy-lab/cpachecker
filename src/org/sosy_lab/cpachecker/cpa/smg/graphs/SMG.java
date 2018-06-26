/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.graphs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.TreeMultimap;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsToFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;

public class SMG implements UnmodifiableSMG {
  private PersistentSet<SMGObject> objects;
  private PersistentSet<SMGValue> values;
  private SMGHasValueEdges hv_edges;
  private SMGPointsToEdges pt_edges;

  /** {@link #validObjects} is a subset of {@link #objects} */
  private PersistentSet<SMGObject> validObjects;

  private PersistentSet<SMGObject> externalObjectAllocation;
  private NeqRelation neq = new NeqRelation();

  private final PredRelation pathPredicate = new PredRelation();
  private PredRelation errorPredicate = new PredRelation();


  private final MachineModel machine_model;

  private final static SMGEdgePointsTo NULL_POINTER = new SMGEdgePointsTo(SMGZeroValue.INSTANCE, SMGNullObject.INSTANCE, 0);

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
  }

  @Override
  public SMG copyOf() {
    return new SMG(this);
  }

  @Override
  public int hashCode() {
    return Objects.hash(machine_model, hv_edges, neq, validObjects, objects, pt_edges, values);
  }

  @Override
  public boolean equals(Object obj) {
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
   * Keeps consistency: no.
   *
   * @param pObj object to add.
   *
   */
  final public void addObject(final SMGObject pObj) {
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
  }
  /**
   * Remove pObj from the SMG. This method does not remove
   * any edges leading from/to the removed object.
   *
   * Keeps consistency: no
   *
   * @param pObj Object to remove
   */
  @VisibleForTesting
  final public void removeObject(final SMGObject pObj) {
    objects = objects.removeAndCopy(pObj);
    validObjects = validObjects.removeAndCopy(pObj);
    externalObjectAllocation = externalObjectAllocation.removeAndCopy(pObj);
  }

  /**
   * Remove pObj and all edges leading from/to it from the SMG
   *
   * Keeps consistency: no
   *
   * @param pObj Object to remove
   */
  final public void removeObjectAndEdges(final SMGObject pObj) {
    Preconditions.checkArgument(pObj != SMGNullObject.INSTANCE, "Can not remove NULL from SMG");
    removeObject(pObj);
    hv_edges = hv_edges.removeAllEdgesOfObjectAndCopy(pObj);
    pt_edges = pt_edges.removeAllEdgesOfObjectAndCopy(pObj);
  }

  /**
   * Add pObj object to the SMG, with validity set to pValidity.
   *
   * Keeps consistency: no.
   *
   * @param pObj      Object to add
   * @param pValidity Validity of the newly added object.
   *
   */
  final public void addObject(final SMGObject pObj, final boolean pValidity, final boolean pExternal) {
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
   * Keeps consistency: no.
   *
   * @param pEdge Points-To edge to add.
   */
  final public void addPointsToEdge(SMGEdgePointsTo pEdge) {
    Preconditions.checkArgument(values.contains(pEdge.getValue()), "adding an edge without source");
    pt_edges = pt_edges.addAndCopy(pEdge);
  }

  /**
   * Add pEdge Has-Value edge to the SMG.
   *
   * Keeps consistency: no
   *
   * @param pEdge Has-Value edge to add
   */
  final public void addHasValueEdge(SMGEdgeHasValue pEdge) {
    Preconditions.checkArgument(values.contains(pEdge.getValue()), "adding edge without target");
    hv_edges = hv_edges.addEdgeAndCopy(pEdge);
  }

  /**
   * Remove pEdge Has-Value edge from the SMG.
   *
   * Keeps consistency: no
   *
   * @param pEdge Has-Value edge to remove
   */
  final public void removeHasValueEdge(SMGEdgeHasValue pEdge) {
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
   * Sets the validity of the object pObject to pValidity.
   * Throws {@link IllegalArgumentException} if pObject is
   * not present in SMG.
   *
   * Keeps consistency: no
   *
   * @param pObject An object.
   * @param pValidity Validity to set.
   */
  public void setValidity(SMGObject pObject, boolean pValidity) {
    Preconditions.checkArgument(objects.contains(pObject), "Object [" + pObject + "] not in SMG");
    if (pValidity) {
      validObjects = validObjects.addAndCopy(pObject);
    } else {
      validObjects = validObjects.removeAndCopy(pObject);
    }
  }

  /**
   * Sets the ExternallyAllocatedFlag of the object pObject to pExternal.
   * Throws {@link IllegalArgumentException} if pObject is
   * not present in SMG.
   *
   * Keeps consistency: no
   *
   * @param pObject An object.
   * @param pExternal Validity to set.
   */
  public void setExternallyAllocatedFlag(SMGObject pObject, boolean pExternal) {
    Preconditions.checkArgument(objects.contains(pObject), "Object [" + pObject + "] not in SMG");
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
  public PredRelation getPathPredicateRelation() {
    return pathPredicate;
  }

  @Override
  public PredRelation getErrorPredicateRelation() {
    return errorPredicate;
  }

  public void resetErrorRelation() {
    errorPredicate = new PredRelation();
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
  public final Set<SMGValue> getValues() {
    return Collections.unmodifiableSet(values.asSet());
  }

  /**
   * Getter for obtaining unmodifiable view on objects set. Constant.
   * @return Unmodifiable view on objects set.
   */
  @Override
  final public Set<SMGObject> getObjects() {
    return Collections.unmodifiableSet(objects.asSet());
  }

  /**
   * Getter for obtaining unmodifiable view on Has-Value edges set. Constant.
   * @return Unmodifiable view on Has-Value edges set.
   */
  @Override
  final public Set<SMGEdgeHasValue> getHVEdges() {
    return Collections.unmodifiableSet(hv_edges.getHvEdges());
  }

  /**
   * Getter for obtaining unmodifiable view on Has-Value edges set, filtered by
   * a certain set of criteria.
   * @param pFilter Filtering object
   * @return A set of Has-Value edges for which the criteria in p hold
   */
  @Override
  final public Set<SMGEdgeHasValue> getHVEdges(SMGEdgeHasValueFilter pFilter) {
    return ImmutableSet.copyOf(pFilter.filter(hv_edges));
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
    Preconditions.checkArgument(values.contains(pValue), "Value [" + pValue + "] not in SMG");
    if (pt_edges.containsEdgeWithValue(pValue)) {
      return pt_edges.getEdgeWithValue(pValue).getObject();
    } else {
      return null;
    }
  }

  /**
   * Getter for determining if the object pObject is valid. Constant.
   * Throws {@link IllegalArgumentException} if pObject is
   * not present in the SMG.
   *
   * @param pObject An object.
   * @return True if Object is valid, False if it is invalid.
   */
  @Override
  final public boolean isObjectValid(SMGObject pObject) {
    Preconditions.checkArgument(objects.contains(pObject), "Object [" + pObject + "] not in SMG");
    return validObjects.contains(pObject);
  }

  /**
   * Getter for determing if the object pObject is externally allocated Throws {@link
   * IllegalAccessException} if pObject is not present is the SMG
   */
  @Override
  public final boolean isObjectExternallyAllocated(SMGObject pObject) {
    Preconditions.checkArgument(objects.contains(pObject), "Object [" + pObject + "] not in SMG");
    return externalObjectAllocation.contains(pObject);
  }

  /**
   * Getter for obtaining SMG machine model. Constant.
   * @return SMG machine model
   */
  @Override
  final public MachineModel getMachineModel() {
    return machine_model;
  }

  /**
   * Obtains a TreeMap offset to size signifying where the object bytes are nullified.
   *
   * Constant.
   *
   * @param pObj SMGObject for which the information is to be obtained
   * @return A TreeMap offsets to size which are covered by a HasValue edge leading from an
   * object to null value
   */
  @Override
  public TreeMap<Long, Integer> getNullEdgesMapOffsetToSizeForObject(SMGObject pObj) {
    SMGEdgeHasValueFilter objectFilter =
        SMGEdgeHasValueFilter.objectFilter(pObj).filterHavingValue(SMGZeroValue.INSTANCE);
    TreeMultimap<Long, Integer> offsetToSize = TreeMultimap.create();
    for (SMGEdgeHasValue edge : objectFilter.filter(hv_edges)) {
      offsetToSize.put(edge.getOffset(), edge.getSizeInBits(machine_model));
    }

    TreeMap<Long, Integer> resultOffsetToSize = new TreeMap<>();
    if (!offsetToSize.isEmpty()) {
      Iterator<Long> offsetsIterator = offsetToSize.keySet().iterator();
      long resultOffset = offsetsIterator.next();
      Integer resultSize = offsetToSize.get(resultOffset).last();
      while (offsetsIterator.hasNext()) {
        long nextOffset = offsetsIterator.next();
        if (nextOffset <= resultOffset + resultSize) {
          resultSize = Math.toIntExact(Long.max(offsetToSize.get(nextOffset).last() + nextOffset -
              resultOffset, resultSize));
        } else {
          resultOffsetToSize.put(resultOffset, resultSize);
          resultOffset = nextOffset;
          resultSize = offsetToSize.get(nextOffset).last();
        }
      }
      resultOffsetToSize.put(resultOffset, resultSize);
    }
    return resultOffsetToSize;
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
    return isCoveredByNullifiedBlocks(pEdge.getObject(), pEdge.getOffset(), pEdge.getSizeInBits(machine_model));
  }

  @Override
  public boolean isCoveredByNullifiedBlocks(SMGObject pObject, long pOffset, CType pType ) {
    return isCoveredByNullifiedBlocks(pObject, pOffset, machine_model.getSizeofInBits(pType));
  }

  private boolean isCoveredByNullifiedBlocks(SMGObject pObject, long pOffset, int size) {
    long expectedMinClear = pOffset + size;

    TreeMap<Long, Integer> nullEdgesOffsetToSize = getNullEdgesMapOffsetToSizeForObject(pObject);
    Entry<Long, Integer> floorEntry = nullEdgesOffsetToSize.floorEntry(pOffset);
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

    removeValue(old);

    for (SMGEdgeHasValue old_hve : getHVEdges(SMGEdgeHasValueFilter.valueFilter(old))) {
      SMGEdgeHasValue newHvEdge =
          new SMGEdgeHasValue(old_hve.getType(), old_hve.getOffset(), old_hve.getObject(), fresh);
      hv_edges = hv_edges.removeEdgeAndCopy(old_hve);
      hv_edges = hv_edges.addEdgeAndCopy(newHvEdge);
    }

    if (pt_edges.containsEdgeWithValue(old)) {
      SMGEdgePointsTo pt_edge = pt_edges.getEdgeWithValue(old);
      pt_edges = pt_edges.removeAndCopy(pt_edge);
      Preconditions.checkArgument(!pt_edges.containsEdgeWithValue(fresh));
      pt_edges =
          pt_edges.addAndCopy(new SMGEdgePointsTo(fresh, pt_edge.getObject(), pt_edge.getOffset()));
    }
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
}