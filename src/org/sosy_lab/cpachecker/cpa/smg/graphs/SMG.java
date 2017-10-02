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
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsToFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;

public class SMG {
  private PersistentSet<SMGObject> objects;
  private PersistentSet<Integer> values;
  private SMGHasValueEdges hv_edges;
  private SMGPointsToEdges pt_edges;
  private PersistentMap<SMGObject, Boolean> object_validity;
  private PersistentMap<SMGObject, SMG.ExternalObjectFlag> objectAllocationIdentity;
  private NeqRelation neq = new NeqRelation();

  private PredRelation pathPredicate = new PredRelation();
  private PredRelation errorPredicate = new PredRelation();


  private final MachineModel machine_model;

  /**
   * An address of the special object representing null
   */
  public final static int NULL_ADDRESS = 0;
  private final static SMGEdgePointsTo NULL_POINTER = new SMGEdgePointsTo(NULL_ADDRESS, SMGNullObject.INSTANCE, 0);

  /**
   * Constructor.
   *
   * Consistent after call: yes.
   *
   * @param pMachineModel A machine model this SMG uses.
   *
   */
  public SMG(final MachineModel pMachineModel) {
    objects = PersistentSet.of();
    values = PersistentSet.of();
    hv_edges = new SMGHasValueEdgeSet();
    pt_edges = new SMGPointsToMap();
    object_validity = PathCopyingPersistentTreeMap.of();
    objectAllocationIdentity = PathCopyingPersistentTreeMap.of();
    machine_model = pMachineModel;

    initializeNullObject();
    initializeNullAddress();
  }

  /**
   * Copy constructor.
   *
   * Consistent after call: yes if pHeap is consistent, no otherwise.
   *
   * @param pHeap Original SMG.
   */
  public SMG(final SMG pHeap) {
    machine_model = pHeap.machine_model;
    hv_edges = pHeap.hv_edges;
    pt_edges = pHeap.pt_edges;
    neq = pHeap.neq;
    pathPredicate.putAll(pHeap.pathPredicate);
    errorPredicate.putAll(pHeap.errorPredicate);
    object_validity = pHeap.object_validity;
    objectAllocationIdentity = pHeap.objectAllocationIdentity;
    objects = pHeap.objects;
    values = pHeap.values;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        machine_model,
        hv_edges,
        neq,
        object_validity,
        objects,
        pt_edges,
        values);
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
        && Objects.equals(object_validity,other.object_validity)
        && Objects.equals(objects, other.objects)
        && Objects.equals(pt_edges, other.pt_edges)
        && Objects.equals(values, other.values);
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
   * Remove pValue from the SMG. This method does not remove
   * any edges leading from/to the removed value.
   *
   * Keeps consistency: no
   *
   * @param pValue Value to remove
   */
  final public void removeValue(final Integer pValue) {
    Preconditions.checkArgument(pValue != 0, "Can not remove NULL from SMG");
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
    object_validity = object_validity.removeAndCopy(pObj);
    objectAllocationIdentity = objectAllocationIdentity.removeAndCopy(pObj);
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
    object_validity = object_validity.putAndCopy(pObj, pValidity);
    objectAllocationIdentity = objectAllocationIdentity.putAndCopy(pObj, new ExternalObjectFlag(pExternal));
  }

  /**
   * Add pValue value to the SMG.
   *
   * Keeps consistency: no.
   *
   * @param pValue  Value to add.
   */
  final public void addValue(Integer pValue) {
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
   * Keeps consistency: no
   *
   * @param pValue the Source of the Points-To edge to be removed
   */
  final public void removePointsToEdge(int pValue) {
    Preconditions.checkArgument(pValue != 0, "Can not remove NULL from SMG");
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
    object_validity = object_validity.putAndCopy(pObject, pValidity);
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
    objectAllocationIdentity = objectAllocationIdentity.putAndCopy(pObject, new ExternalObjectFlag(pExternal));
  }

  /**
   * Replaces whole HasValue edge set with new set.
   * @param pNewHV
   *
   * Keeps consistency: no
   */
  public void replaceHVSet(Set<SMGEdgeHasValue> pNewHV) {
    SMGHasValueEdges tmp = new SMGHasValueEdgeSet();
    for (SMGEdgeHasValue edge : pNewHV) {
      tmp = tmp.addEdgeAndCopy(edge);
    }
    hv_edges = tmp;
  }

  /**
   * Adds a neq relation between two values to the SMG
   * Keeps consistency: no
   */
  public void addNeqRelation(Integer pV1, Integer pV2) {
    neq = neq.addRelationAndCopy(pV1, pV2);
  }

  /**
   * Adds a predicate relation between two values to the SMG
   * Keeps consistency: no
   */
  public void addPredicateRelation(SMGSymbolicValue pV1, Integer pCType1,
                                   SMGSymbolicValue pV2, Integer pCType2,
                                   BinaryOperator pOp, CFAEdge pEdge) {
    CAssumeEdge assumeEdge = (CAssumeEdge) pEdge;
    if (assumeEdge.getTruthAssumption()) {
      pathPredicate.addRelation(pV1, pCType1, pV2, pCType2, pOp);
    } else {
      pathPredicate.addRelation(pV1, pCType1, pV2, pCType2, pOp.getOppositLogicalOperator());
    }
  }

  public void addPredicateRelation(SMGSymbolicValue pSymbolicValue, Integer pCType1,
                                   SMGExplicitValue pExplicitValue, Integer pCType2,
                                   BinaryOperator pOp, CFAEdge pEdge) {
    if (pEdge instanceof CAssumeEdge) {
      CAssumeEdge assumeEdge = (CAssumeEdge) pEdge;
      if (assumeEdge.getTruthAssumption()) {
        pathPredicate.addExplicitRelation(pSymbolicValue, pCType1, pExplicitValue, pCType2, pOp);
      } else {
        pathPredicate.addExplicitRelation(pSymbolicValue, pCType1, pExplicitValue, pCType2, pOp.getOppositLogicalOperator());
      }
    }
  }

  public PredRelation getPathPredicateRelation() {
    return pathPredicate;
  }

  public void addErrorRelation(SMGSymbolicValue pSMGSymbolicValue, Integer pCType1,
                               SMGExplicitValue pExplicitValue, Integer pCType2) {
    errorPredicate.addExplicitRelation(pSMGSymbolicValue, pCType1, pExplicitValue, pCType2, BinaryOperator.GREATER_THAN);
  }

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
   * @return Unmodifiable view on values set.
   */
  final public Set<Integer> getValues() {
    return values.asSet();
  }

  /**
   * Getter for obtaining unmodifiable view on objects set. Constant.
   * @return Unmodifiable view on objects set.
   */
  final public Set<SMGObject> getObjects() {
    return objects.asSet();
  }

  /**
   * Getter for obtaining unmodifiable view on Has-Value edges set. Constant.
   * @return Unmodifiable view on Has-Value edges set.
   */
  final public Set<SMGEdgeHasValue> getHVEdges() {
    return hv_edges.getHvEdges();
  }

  /**
   * Getter for obtaining unmodifiable view on Has-Value edges set, filtered by
   * a certain set of criteria.
   * @param pFilter Filtering object
   * @return A set of Has-Value edges for which the criteria in p hold
   */
  final public Set<SMGEdgeHasValue> getHVEdges(SMGEdgeHasValueFilter pFilter) {
    return ImmutableSet.copyOf(pFilter.filter(hv_edges));
  }

  public Set<SMGEdgePointsTo> getPtEdges(SMGEdgePointsToFilter pFilter) {
    return ImmutableSet.copyOf(pFilter.filter(pt_edges));
  }

  public SMGPointsToEdges getPTEdges() {
    return pt_edges;
  }

  /**
   * Getter for obtaining an object, pointed by a value pValue. Constant.
   *
   * @param pValue An origin value.
   * @return The object pointed by the value pValue, if such exists.
   * Null, if pValue does not point to any
   * object.
   *
   * Throws {@link IllegalArgumentException} if pValue is
   * not present in the SMG.
   *
   * TODO: Test
   * TODO: Consistency check: no value can point to more objects
   */
  public final @Nullable SMGObject getObjectPointedBy(Integer pValue) {
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
  final public boolean isObjectValid(SMGObject pObject) {
    Preconditions.checkArgument(objects.contains(pObject), "Object [" + pObject + "] not in SMG");
    return object_validity.get(pObject);
  }

  /**
   * Getter for determing if the object pObject is externally allocated
   * Throws {@link IllegalAccessException} if pObject is not present is the SMG
   */
  final public Boolean isObjectExternallyAllocated(SMGObject pObject) {
    Preconditions.checkArgument(objects.contains(pObject), "Object [" + pObject + "] not in SMG");
    return objectAllocationIdentity.get(pObject).isExternal();
  }

  /**
   * Getter for obtaining SMG machine model. Constant.
   * @return SMG machine model
   */
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
  public TreeMap<Long, Integer> getNullEdgesMapOffsetToSizeForObject(SMGObject pObj) {
    SMGEdgeHasValueFilter objectFilter = new SMGEdgeHasValueFilter().filterByObject(pObj).filterHavingValue(SMG.NULL_ADDRESS);
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
   * Checks, whether a {@link SMGEdgePointsTo} edge exists with the
   * given value as source.
   *
   *
   * @param value the source of the {@link SMGEdgePointsTo} edge.
   * @return true, if the {@link SMGEdgePointsTo} edge with the source
   * value exists, otherwise false.
   */
  public boolean isPointer(Integer value) {
    return pt_edges.containsEdgeWithValue(value);
  }

  /**
   * Returns the {@link SMGEdgePointsTo} edge with the
   * given value as source.
   *
   * @param value the source of the {@link SMGEdgePointsTo} edge.
   * @return the {@link SMGEdgePointsTo} edge with the
   * value as source.
   */
  public SMGEdgePointsTo getPointer(Integer value) {
    return pt_edges.getEdgeWithValue(value);
  }

  public boolean isCoveredByNullifiedBlocks(SMGEdgeHasValue pEdge) {
    return isCoveredByNullifiedBlocks(pEdge.getObject(), pEdge.getOffset(), pEdge.getSizeInBits(machine_model));
  }

  public boolean isCoveredByNullifiedBlocks(SMGObject pObject, long pOffset, CType pType ) {
    return isCoveredByNullifiedBlocks(pObject, pOffset, machine_model.getSizeofInBits(pType));
  }

  private boolean isCoveredByNullifiedBlocks(SMGObject pObject, long pOffset, int size) {
    long expectedMinClear = pOffset + size;

    TreeMap<Long, Integer> nullEdgesOffsetToSize = getNullEdgesMapOffsetToSizeForObject(pObject);
    Entry<Long, Integer> floorEntry = nullEdgesOffsetToSize.floorEntry(pOffset);
    return (floorEntry != null && floorEntry.getValue() + floorEntry.getKey() >= expectedMinClear);
  }

  public void mergeValues(int pV1, int pV2) {

    /*Might merge predicates?*/
    addValue(pV2);

    /* Value might not have been added yet */
    addValue(pV1);

    if (pV1 == pV2) {
      return;
    }

    if (pV2 == NULL_ADDRESS) { // swap
      int tmp = pV1;
      pV1 = pV2;
      pV2 = tmp;
    }

    neq = neq.mergeValuesAndCopy(pV1, pV2);
    pathPredicate.mergeValues(pV1, pV2);

    removeValue(pV2);

    for (SMGEdgeHasValue old_hve : getHVEdges(SMGEdgeHasValueFilter.valueFilter(pV2))) {
      SMGEdgeHasValue newHvEdge =
          new SMGEdgeHasValue(old_hve.getType(), old_hve.getOffset(), old_hve.getObject(), pV1);
      hv_edges = hv_edges.removeEdgeAndCopy(old_hve);
      hv_edges = hv_edges.addEdgeAndCopy(newHvEdge);
    }

    // TODO: Handle PT Edges: I'm not entirely sure how they should be handled
  }

  public boolean haveNeqRelation(Integer pV1, Integer pV2) {
    return neq.neq_exists(pV1, pV2);
  }

  public Set<Integer> getNeqsForValue(Integer pV) {
    return neq.getNeqsForValue(pV);
  }

  private static class ExternalObjectFlag {
    private final boolean external;

    public ExternalObjectFlag(boolean pExternal) {
      external = pExternal;
    }

    public boolean isExternal() {
      return external;
    }

    @Override
    public String toString() {
      return "" + external;
    }
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
    addValue(NULL_ADDRESS);
    addPointsToEdge(NULL_POINTER);
  }

  public void clearObjects() {
    objects = PersistentSet.of();
    object_validity = PathCopyingPersistentTreeMap.of();
    initializeNullObject();
  }

  private void initializeNullObject() {
    addObject(SMGNullObject.INSTANCE);
    object_validity = object_validity.putAndCopy(SMGNullObject.INSTANCE, false);
  }
}