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
package org.sosy_lab.cpachecker.cpa.smg.graphs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsToFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SMG {
  private Set<SMGObject> objects = new HashSet<>();
  private Set<Integer> values = new HashSet<>();
  private SMGHasValueEdges hv_edges;
  private SMGPointsToEdges pt_edges;
  private Map<SMGObject, Boolean> object_validity = new HashMap<>();
  private Map<SMGObject, SMG.ExternalObjectFlag> objectAllocationIdentity = new HashMap<>();
  private NeqRelation neq = new NeqRelation();

  private PredRelation pathPredicate = new PredRelation();
  private PredRelation errorPredicate = new PredRelation();


  private final MachineModel machine_model;

  /**
   * A special object representing NULL
   */
  private final static SMGObject nullObject = SMGObject.getNullObject();

  /**
   * An address of the special object representing null
   */
  private final static int nullAddress = 0;

  /**
   * Constructor.
   *
   * Consistent after call: yes.
   *
   * @param pMachineModel A machine model this SMG uses.
   *
   */
  public SMG(final MachineModel pMachineModel) {
    SMGEdgePointsTo nullPointer = new SMGEdgePointsTo(nullAddress, nullObject, 0);

    hv_edges = new SMGHasValueEdgeSet();
    pt_edges = new SMGPointsToMap();

    addObject(nullObject);
    object_validity.put(nullObject, false);

    addValue(nullAddress);
    addPointsToEdge(nullPointer);

    machine_model = pMachineModel;
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
    hv_edges = pHeap.hv_edges.copy();
    pt_edges = pHeap.pt_edges.copy();
    neq.putAll(pHeap.neq);
    pathPredicate.putAll(pHeap.pathPredicate);
    errorPredicate.putAll(pHeap.errorPredicate);
    object_validity.putAll(pHeap.object_validity);
    objectAllocationIdentity.putAll(pHeap.objectAllocationIdentity);
    objects.addAll(pHeap.objects);
    values.addAll(pHeap.values);
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

    assert pValue != 0;

    values.remove(pValue);
    neq.removeValue(pValue);
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
    objects.remove(pObj);
    object_validity.remove(pObj);
    objectAllocationIdentity.remove(pObj);
  }

  /**
   * Remove pObj and all edges leading from/to it from the SMG
   *
   * Keeps consistency: no
   *
   * @param pObj Object to remove
   */
  final public void removeObjectAndEdges(final SMGObject pObj) {

    assert pObj.notNull();

    removeObject(pObj);
    hv_edges.removeAllEdgesOfObject(pObj);
    pt_edges.removeAllEdgesOfObject(pObj);
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
    objects.add(pObj);
    object_validity.put(pObj, pValidity);
    objectAllocationIdentity.put(pObj, new ExternalObjectFlag(pExternal));
  }

  /**
   * Add pValue value to the SMG.
   *
   * Keeps consistency: no.
   *
   * @param pValue  Value to add.
   */
  final public void addValue(Integer pValue) {
    values.add(pValue);
  }

  /**
   * Add pEdge Points-To edge to the SMG.
   *
   * Keeps consistency: no.
   *
   * @param pEdge Points-To edge to add.
   */
  final public void addPointsToEdge(SMGEdgePointsTo pEdge) {
    pt_edges.add(pEdge);
  }

  /**
   * Add pEdge Has-Value edge to the SMG.
   *
   * Keeps consistency: no
   *
   * @param pEdge Has-Value edge to add
   */
  final public void addHasValueEdge(SMGEdgeHasValue pEdge) {
    hv_edges.addEdge(pEdge);
  }

  /**
   * Remove pEdge Has-Value edge from the SMG.
   *
   * Keeps consistency: no
   *
   * @param pEdge Has-Value edge to remove
   */
  final public void removeHasValueEdge(SMGEdgeHasValue pEdge) {
    hv_edges.removeEdge(pEdge);
  }

  /**
   * Remove the Points-To edge from the SMG with the value pValue as Source.
   *
   * Keeps consistency: no
   *
   * @param pValue the Source of the Points-To edge to be removed
   */
  final public void removePointsToEdge(int pValue) {
    assert pValue != 0;

    pt_edges.removeEdgeWithValue(pValue);
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
    if (! objects.contains(pObject)) {
      throw new IllegalArgumentException("Object [" + pObject + "] not in SMG");
    }

    object_validity.put(pObject, pValidity);
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
    if (! objects.contains(pObject)) {
      throw new IllegalArgumentException("Object [" + pObject + "] not in SMG");
    }

    objectAllocationIdentity.put(pObject, new ExternalObjectFlag(pExternal));
  }

  /**
   * Replaces whole HasValue edge set with new set.
   * @param pNewHV
   *
   * Keeps consistency: no
   */
  public void replaceHVSet(Set<SMGEdgeHasValue> pNewHV) {
    hv_edges.replaceHvEdges(pNewHV);
  }

  /**
   * Adds a neq relation between two values to the SMG
   * Keeps consistency: no
   */
  public void addNeqRelation(Integer pV1, Integer pV2) {
    neq.add_relation(pV1, pV2);
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
   * Getter for obtaining designated NULL object. Constant.
   * @return An object guaranteed to be the only NULL object in the SMG
   */
  final public SMGObject getNullObject() {
    return SMG.nullObject;
  }

  /**
   * Getter for obtaining designated zero value. Constant.
   * @return A value guaranteed to be the only zero value in the SMG
   */
  final public int getNullValue() {
    return SMG.nullAddress;
  }

  /**
   * Getter for obtaining string representation of values set. Constant.
   * @return String representation of values set
   */
  final public String valuesToString() {
    return "values=" + values.toString();
  }

  /**
   * Getter for obtaining string representation of has-value edges set. Constant.
   * @return String representation of has-value edges set
   */
  final public String hvToString() {
    return "hasValue=" + hv_edges.toString();
  }

  /**
   * Getter for obtaining string representation of points-to edges set. Constant.
   * @return String representation of points-to edges set
   */
  final public String ptToString() {
    return "pointsTo=" + pt_edges.toString();
  }

  /**
   * Getter for obtaining unmodifiable view on values set. Constant.
   * @return Unmodifiable view on values set.
   */
  final public Set<Integer> getValues() {
    return Collections.unmodifiableSet(values);
  }

  /**
   * Getter for obtaining unmodifiable view on objects set. Constant.
   * @return Unmodifiable view on objects set.
   */
  final public Set<SMGObject> getObjects() {
    return Collections.unmodifiableSet(objects);
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
    return Collections.unmodifiableSet(hv_edges.filter(pFilter));
  }

  public Set<SMGEdgePointsTo> getPtEdges(SMGEdgePointsToFilter pFilter) {
    return ImmutableSet.copyOf(pt_edges.filter(pFilter));
  }

  protected SMGPointsToEdges getPTEdges() {
    return pt_edges;
  }

  /**
   * Getter for obtaining unmodifiable view on Points-To edges set. Constant.
   * @return Unmodifiable view on Points-To edges set.
   */
  final public Map<Integer, SMGEdgePointsTo> getPTEdgesAsMap() {
    return pt_edges.asMap();
  }

  final public Set<SMGEdgePointsTo> getPTEdgesAsSet() {
    return pt_edges.asSet();
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
  final public SMGObject getObjectPointedBy(Integer pValue) {
    if (!values.contains(
        pValue)) {
      throw new IllegalArgumentException("Value [" + pValue + "] not in SMG");
    }

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
    if ( ! objects.contains(pObject)) {
      throw new IllegalArgumentException("Object [" + pObject + "] not in SMG");
    }

    return object_validity.get(pObject);
  }

  /**
   * Getter for determing if the object pObject is externally allocated
   * Throws {@link IllegalAccessException} if pObject is not present is the SMG
   */
  final public Boolean isObjectExternallyAllocated(SMGObject pObject) {
    if ( ! objects.contains(pObject)) {
      throw new IllegalArgumentException("Object [" + pObject + "] not in SMG");
    }

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
   * Obtains a bitset signifying where the object bytes are nullified.
   *
   * Constant.
   *
   * @param pObj SMGObject for which the information is to be obtained
   * @return A bitset. A bit has 1 value if the appropriate byte is guaranteed
   * to be NULL (is covered by a HasValue edge leading from an object to null value,
   * 0 otherwise.
   */
  public BitSet getNullBytesForObject(SMGObject pObj) {
    BitSet bs = new BitSet(pObj.getSize());
    bs.clear();
    SMGEdgeHasValueFilter objectFilter = SMGEdgeHasValueFilter.objectFilter(pObj).filterHavingValue(getNullValue());

    for (SMGEdgeHasValue edge : getHVEdges(objectFilter)) {
      bs.set(edge.getOffset(), edge.getOffset() + edge.getSizeInBytes(machine_model));
    }

    return bs;
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
    return isCoveredByNullifiedBlocks(pEdge.getObject(), pEdge.getOffset(), pEdge.getSizeInBytes(machine_model));
  }

  public boolean isCoveredByNullifiedBlocks(SMGObject pObject, int pOffset, CType pType ) {
    return isCoveredByNullifiedBlocks(pObject, pOffset, machine_model.getSizeof(pType));
  }

  private boolean isCoveredByNullifiedBlocks(SMGObject pObject, int pOffset, int size) {
    BitSet objectNullBytes = getNullBytesForObject(pObject);
    int expectedMinClear = pOffset + size;

    return (objectNullBytes.nextClearBit(pOffset) >= expectedMinClear);
  }

  public void mergeValues(int pV1, int pV2) {

    if (!values.contains(pV2)) {
      /*Might merge predicates?*/
      addValue(pV2);
    }

    /* Value might not have been added yet */
    if (!values.contains(pV1)) {
      addValue(pV1);
    }

    if (pV1 == pV2) {
      return;
    }

    if (pV2 == nullAddress) {
      int tmp = pV1;
      pV1 = pV2;
      pV2 = tmp;
    }

    neq.mergeValues(pV1, pV2);
    pathPredicate.mergeValues(pV1, pV2);

    removeValue(pV2);

    Set<SMGEdgeHasValue> old_hv_edges = getHVEdges(SMGEdgeHasValueFilter.valueFilter(pV2));
    for (SMGEdgeHasValue old_hve : old_hv_edges) {
      SMGEdgeHasValue newHvEdge =
          new SMGEdgeHasValue(old_hve.getType(), old_hve.getOffset(), old_hve.getObject(), pV1);
      hv_edges.removeEdge(old_hve);
      hv_edges.addEdge(newHvEdge);
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
  }

  protected void clearValuesHvePte() {
    values.clear();
    hv_edges.clear();
    pt_edges.clear();
    neq.clear();
    pathPredicate.clear();
    addValue(nullAddress);
    SMGEdgePointsTo nullPointer = new SMGEdgePointsTo(nullAddress, nullObject, 0);
    addPointsToEdge(nullPointer);
  }

  public void clearObjects() {
    objects.clear();
    object_validity.clear();

    /*May not clear null objects*/
    addObject(nullObject);
    object_validity.put(nullObject, false);
  }
}