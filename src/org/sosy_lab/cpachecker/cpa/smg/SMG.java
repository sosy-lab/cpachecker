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
package org.sosy_lab.cpachecker.cpa.smg;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

import com.google.common.collect.Iterables;

public class SMG {
  final private HashSet<SMGObject> objects = new HashSet<>();
  final private HashSet<Integer> values = new HashSet<>();
  final private HashSet<SMGEdgeHasValue> hv_edges = new HashSet<>();
  final private HashMap<Integer, SMGEdgePointsTo> pt_edges = new HashMap<>();
  final private HashMap<SMGObject, Boolean> object_validity = new HashMap<>();
  final private NeqRelation neq = new NeqRelation();

  final private MachineModel machine_model;

  /**
   * A special object representing NULL
   */
  final private static SMGObject nullObject = SMGObject.getNullObject();

  /**
   * An address of the special object representing null
   */
  final private static int nullAddress = 0;

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
    objects.addAll(pHeap.objects);
    values.addAll(pHeap.values);
    hv_edges.addAll(pHeap.hv_edges);
    pt_edges.putAll(pHeap.pt_edges);

    object_validity.putAll(pHeap.object_validity);

    machine_model = pHeap.machine_model;

    neq.putAll(pHeap.neq);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((hv_edges == null) ? 0 : hv_edges.hashCode());
    result = prime * result + ((machine_model == null) ? 0 : machine_model.hashCode());
    result = prime * result + ((neq == null) ? 0 : neq.hashCode());
    result = prime * result + ((object_validity == null) ? 0 : object_validity.hashCode());
    result = prime * result + ((objects == null) ? 0 : objects.hashCode());
    result = prime * result + ((pt_edges == null) ? 0 : pt_edges.hashCode());
    result = prime * result + ((values == null) ? 0 : values.hashCode());
    return result;
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
    if (hv_edges == null) {
      if (other.hv_edges != null) {
        return false;
      }
    } else if (!hv_edges.equals(other.hv_edges)) {
      return false;
    }
    if (machine_model != other.machine_model) {
      return false;
    }
    if (neq == null) {
      if (other.neq != null) {
        return false;
      }
    } else if (!neq.equals(other.neq)) {
      return false;
    }
    if (object_validity == null) {
      if (other.object_validity != null) {
        return false;
      }
    } else if (!object_validity.equals(other.object_validity)) {
      return false;
    }
    if (objects == null) {
      if (other.objects != null) {
        return false;
      }
    } else if (!objects.equals(other.objects)) {
      return false;
    }
    if (pt_edges == null) {
      if (other.pt_edges != null) {
        return false;
      }
    } else if (!pt_edges.equals(other.pt_edges)) {
      return false;
    }
    if (values == null) {
      if (other.values != null) {
        return false;
      }
    } else if (!values.equals(other.values)) {
      return false;
    }
    return true;
  }

  /**
   * Add an object {@link pObj} to the SMG.
   *
   * Keeps consistency: no.
   *
   * @param pObj object to add.
   *
   */
  final public void addObject(final SMGObject pObj) {
    addObject(pObj, true);
  }

  /**
   * Remove {@link pValue} from the SMG. This method does not remove
   * any edges leading from/to the removed value.
   *
   * Keeps consistency: no
   *
   * @param pValue Value to remove
   */
  final public void removeValue(final Integer pValue) {
    values.remove(pValue);
    neq.removeValue(pValue);
  }
  /**
   * Remove {@link pObj} from the SMG. This method does not remove
   * any edges leading from/to the removed object.
   *
   * Keeps consistency: no
   *
   * @param pObj Object to remove
   */
  final public void removeObject(final SMGObject pObj) {
    objects.remove(pObj);
    object_validity.remove(pObj);
  }

  /**
   * Remove {@link pObj} and all edges leading from/to it from the SMG
   *
   * Keeps consistency: no
   *
   * @param pObj Object to remove
   */
  final public void removeObjectAndEdges(final SMGObject pObj) {
    removeObject(pObj);
    Iterator<SMGEdgeHasValue> hv_iter = hv_edges.iterator();
    Iterator<SMGEdgePointsTo> pt_iter = pt_edges.values().iterator();
    while (hv_iter.hasNext()) {
      if (hv_iter.next().getObject() == pObj) {
        hv_iter.remove();
      }
    }

    while (pt_iter.hasNext()) {
      if (pt_iter.next().getObject() == pObj) {
        pt_iter.remove();
      }
    }
  }

  /**
   * Add {@link pObj} object to the SMG, with validity set to {@link pValidity}.
   *
   * Keeps consistency: no.
   *
   * @param pObj      Object to add
   * @param pValidity Validity of the newly added object.
   *
   */
  final public void addObject(final SMGObject pObj, final boolean pValidity) {
    objects.add(pObj);
    object_validity.put(pObj, pValidity);
  }

  /**
   * Add {@link pValue} value to the SMG.
   *
   * Keeps consistency: no.
   *
   * @param pValue  Value to add.
   */
  final public void addValue(Integer pValue) {
    values.add(pValue);
  }

  /**
   * Add {@link pEdge} Points-To edge to the SMG.
   *
   * Keeps consistency: no.
   *
   * @param pEdge Points-To edge to add.
   */
  final public void addPointsToEdge(SMGEdgePointsTo pEdge) {
    pt_edges.put(pEdge.getValue(), pEdge);
  }

  /**
   * Add {@link pEdge} Has-Value edge to the SMG.
   *
   * Keeps consistency: no
   *
   * @param pEdge Has-Value edge to add
   */
  final public void addHasValueEdge(SMGEdgeHasValue pEdge) {
    hv_edges.add(pEdge);
  }

  /**
   * Remove {@link pEdge} Has-Value edge from the SMG.
   *
   * Keeps consistency: no
   *
   * @param pEdge Has-Value edge to remove
   */
  final public void removeHasValueEdge(SMGEdgeHasValue pEdge) {
    hv_edges.remove(pEdge);
  }

  /**
   * Remove the Points-To edge from the SMG with the value {@link pValue} as Source.
   *
   * Keeps consistency: no
   *
   * @param pValue the Source of the Points-To edge to be removed
   */
  final public void removePointsToEdge(int pValue) {
    pt_edges.remove(pValue);
  }

  /**
   * Sets the validity of the object {@link pObject} to {@link pValidity}.
   * Throws {@link IllegalArgumentException} if {@link pObject} is
   * not present in SMG.
   *
   * Keeps consistency: no
   *
   * @param pObj An object.
   * @param pValidity Validity to set.
   */
  public void setValidity(SMGObject pObject, boolean pValidity) {
    if (! objects.contains(pObject)) {
      throw new IllegalArgumentException("Object [" + pObject + "] not in SMG");
    }

    object_validity.put(pObject, pValidity);
  }

  /**
   * Replaces whole HasValue edge set with new set.
   * @param pNewHV
   *
   * Keeps consistency: no
   */
  public void replaceHVSet(Set<SMGEdgeHasValue> pNewHV) {
    hv_edges.clear();
    hv_edges.addAll(pNewHV);
  }

  /**
   * Adds a neq relation between two values to the SMG
   *
   * @param pV1
   * @param pV2
   *
   * Keeps consistency: no
   */
  public void addNeqRelation(Integer pV1, Integer pV2) {
    neq.add_relation(pV1, pV2);
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
  final public Iterable<SMGEdgeHasValue> getHVEdges() {
    return Collections.unmodifiableSet(hv_edges);
  }

  /**
   * Getter for obtaining unmodifiable view on Has-Value edges set, filtered by
   * a certain set of criteria.
   * @param pFilter Filtering object
   * @return A set of Has-Value edges for which the criteria in p hold
   */
  final public Iterable<SMGEdgeHasValue> getHVEdges(SMGEdgeHasValueFilter pFilter) {
    return Iterables.filter(Collections.unmodifiableSet(hv_edges), pFilter.asPredicate());
  }

  /**
   * Getter for obtaining a unique edge adhering to certain criteria.
   * @param pFilter Criteria for filtering
   * @param pCheck Defines if a check for non-uniqueness should be done.
   * @return A HVEdge adhering to filter
   */
  final public SMGEdgeHasValue getUniqueHV(SMGEdgeHasValueFilter pFilter, boolean pCheck) {
    Iterator<SMGEdgeHasValue> it = getHVEdges(pFilter).iterator();
    SMGEdgeHasValue hv = it.next();
    if (pCheck && it.hasNext()) {
      throw new IllegalArgumentException("Applying filter does not result in unique HV edge");
    }
    return hv;
  }

  /**
   * Getter for obtaining unmodifiable view on Points-To edges set. Constant.
   * @return Unmodifiable view on Points-To edges set.
   */
  final public Map<Integer, SMGEdgePointsTo> getPTEdges() {
    return Collections.unmodifiableMap(pt_edges );
  }

  /**
   * Getter for obtaining an object, pointed by a value {@link pValue}. Constant.
   *
   * @param pValue An origin value.
   * @return The object pointed by the value {@link pValue}, if such exists.
   * Null, if {@link pValue} does not point to any
   * object.
   *
   * Throws {@link IllegalArgumentException} if {@link pValue} is
   * not present in the SMG.
   *
   * TODO: Test
   * TODO: Consistency check: no value can point to more objects
   */
  final public SMGObject getObjectPointedBy(Integer pValue) {
    if ( ! values.contains(pValue)) {
      throw new IllegalArgumentException("Value [" + pValue + "] not in SMG");
    }

    if (pt_edges.containsKey(pValue)) {
      return pt_edges.get(pValue).getObject();
    }
    else {
      return null;
    }
  }

  /**
   * Getter for determining if the object {@link pObject} is valid. Constant.
   * Throws {@link IllegalArgumentException} if {@link pObject} is
   * not present in the SMG.
   *
   * @param pObject An object.
   * @return True if {@link pObject} is valid, False if it is invalid.
   */
  final public boolean isObjectValid(SMGObject pObject) {
    if ( ! objects.contains(pObject)) {
      throw new IllegalArgumentException("Object [" + pObject + "] not in SMG");
    }

    return object_validity.get(pObject).booleanValue();
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
   * {@link value} exists, otherwise false.
   */
  public boolean isPointer(Integer value) {
    return pt_edges.containsKey(value);
  }

  /**
   * Returns the {@link SMGEdgePointsTo} edge with the
   * given value as source.
   *
   * @param value the source of the {@link SMGEdgePointsTo} edge.
   * @return the {@link SMGEdgePointsTo} edge with the
   * {@link value} as source.
   */
  public SMGEdgePointsTo getPointer(Integer value) {
    return pt_edges.get(value);
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
    if (pV1 == pV2) {
      return;
    }
    if (pV2 == nullAddress) {
      int tmp = pV1;
      pV1 = pV2;
      pV2 = tmp;
    }

    neq.mergeValues(pV1, pV2);
    removeValue(pV2);
    HashSet<SMGEdgeHasValue> new_hv_edges = new HashSet<>();
    for (SMGEdgeHasValue hv : hv_edges) {
      if (hv.getValue() != pV2) {
        new_hv_edges.add(hv);
      } else {
        new_hv_edges.add(new SMGEdgeHasValue(hv.getType(), hv.getOffset(), hv.getObject(), pV1));
      }
    }
    hv_edges.clear();
    hv_edges.addAll(new_hv_edges);
    // TODO: Handle PT Edges: I'm not entirely sure how they should be handled
  }

  public boolean haveNeqRelation(Integer pV1, Integer pV2) {
    return neq.neq_exists(pV1, pV2);
  }

  public Set<Integer> getNeqsForValue(Integer pV) {
    return neq.getNeqsForValue(pV);
  }
}

final class NeqRelation {
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((smgValues == null) ? 0 : smgValues.hashCode());
    return result;
  }

  public Set<Integer> getNeqsForValue(Integer pV) {
    if (smgValues.containsKey(pV)) {
      return Collections.unmodifiableSet(new HashSet<>(smgValues.get(pV)));
    }
    return Collections.unmodifiableSet(new HashSet<Integer>());
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
    NeqRelation other = (NeqRelation) obj;
    if (smgValues == null) {
      if (other.smgValues != null) {
        return false;
      }
    } else if (!smgValues.equals(other.smgValues)) {
      return false;
    }
    return true;
  }

  private final Map<Integer, List<Integer>> smgValues = new HashMap<>();

  public void add_relation(Integer pOne, Integer pTwo) {
    if (! smgValues.containsKey(pOne)) {
      smgValues.put(pOne, new ArrayList<Integer>());
    }
    if (! smgValues.containsKey(pTwo)) {
      smgValues.put(pTwo, new ArrayList<Integer>());
    }

    if (! smgValues.get(pOne).contains(pTwo)) {
      smgValues.get(pOne).add(pTwo);
      smgValues.get(pTwo).add(pOne);
    }
  }

  public void putAll(NeqRelation pNeq) {
    for (Integer key : pNeq.smgValues.keySet()) {
      smgValues.put(key, new ArrayList<Integer>());
      smgValues.get(key).addAll(pNeq.smgValues.get(key));
    }
  }

  public void remove_relation(Integer pOne, Integer pTwo) {
    if (smgValues.containsKey(pOne) && smgValues.containsKey(pTwo)) {
      List<Integer> listOne = smgValues.get(pOne);
      List<Integer> listTwo = smgValues.get(pTwo);

      if (listOne.contains(pTwo)) {
        listOne.remove(pTwo);
        listTwo.remove(pOne);
      }
    }
  }

  public boolean neq_exists(Integer pOne, Integer pTwo) {
    if (smgValues.containsKey(pOne) && smgValues.get(pOne).contains(pTwo)) {
      return true;
    }
    return false;
  }

  public void removeValue(Integer pOne) {
    if (smgValues.containsKey(pOne)) {
      for (Integer other : smgValues.get(pOne)) {
        smgValues.get(other).remove(pOne);
      }
      smgValues.remove(pOne);
    }
  }

  public void mergeValues(Integer pOne, Integer pTwo) {
    if (! smgValues.containsKey(pOne)) {
      smgValues.put(pOne, new ArrayList<Integer>());
    }
    if (! smgValues.containsKey(pTwo)) {
      smgValues.put(pTwo, new ArrayList<Integer>());
    }

    List<Integer> values = smgValues.get(pTwo);
    removeValue(pTwo);

    List<Integer> my = smgValues.get(pOne);
    for (Integer value : values) {
      List<Integer> other = smgValues.get(value);
      if ((! value.equals(pOne)) && (! other.contains(pOne))) {
        other.add(pOne);
        my.add(value);
      }
    }
  }
}
