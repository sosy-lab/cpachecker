/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.cpalien;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;

public class SMG {
  final private HashSet<SMGObject> objects = new HashSet<>();
  final private HashSet<Integer> values = new HashSet<>();
  final private HashSet<SMGEdgeHasValue> hv_edges = new HashSet<>();
  final private HashSet<SMGEdgePointsTo> pt_edges = new HashSet<>();
  final private HashMap<SMGObject, Boolean> object_validity = new HashMap<>();

  final private MachineModel machine_model;

  /**
   * A special object representing NULL
   */
  final private static SMGObject nullObject = new SMGObject();

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

    this.addObject(nullObject);
    this.object_validity.put(nullObject, false);

    this.addValue(nullAddress);
    this.addPointsToEdge(nullPointer);

    this.machine_model = pMachineModel;
  }

  /**
   * Copy constructor.
   *
   * Consistent after call: yes if pHeap is consistent, no otherwise.
   *
   * @param pHeap Original SMG.
   */
  public SMG(final SMG pHeap) {
    this.objects.addAll(pHeap.objects);
    this.values.addAll(pHeap.values);
    this.hv_edges.addAll(pHeap.hv_edges);
    this.pt_edges.addAll(pHeap.pt_edges);

    this.object_validity.putAll(pHeap.object_validity);

    this.machine_model = pHeap.machine_model;
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
    this.addObject(pObj, true);
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
    this.objects.add(pObj);
    this.object_validity.put(pObj, pValidity);
  }

  /**
   * Add {@link pValue} value to the SMG.
   *
   * Keeps consistency: no.
   *
   * @param pValue  Value to add.
   */
  final public void addValue(int pValue) {
    this.values.add(Integer.valueOf(pValue));
  }

  /**
   * Add {@link pEdge} Points-To edge to the SMG.
   *
   * Keeps consistency: no.
   *
   * @param pEdge Points-To edge to add.
   */
  final public void addPointsToEdge(SMGEdgePointsTo pEdge) {
    this.pt_edges.add(pEdge);
  }

  /**
   * Add {@link pEdge} Has-Value edge to the SMG.
   *
   * Keeps consistency: no
   *
   * @param pEdge Has-Value edge to add
   */
  final public void addHasValueEdge(SMGEdgeHasValue pEdge) {
    this.hv_edges.add(pEdge);
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
    if (! this.objects.contains(pObject)) {
      throw new IllegalArgumentException("Object [" + pObject + "] not in SMG");
    }

    this.object_validity.put(pObject, pValidity);
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
    return "values=" + this.values.toString();
  }

  /**
   * Getter for obtaining string representation of has-value edges set. Constant.
   * @return String representation of has-value edges set
   */
  final public String hvToString() {
    return "hasValue=" + this.hv_edges.toString();
  }

  /**
   * Getter for obtaining string representation of points-to edges set. Constant.
   * @return String representation of points-to edges set
   */
  final public String ptToString() {
    return "pointsTo=" + this.pt_edges.toString();
  }

  /**
   * Getter for obtaining unmodifiable view on values set. Constant.
   * @return Unmodifiable view on values set.
   */
  final public Set<Integer> getValues() {
    return Collections.unmodifiableSet(this.values);
  }

  /**
   * Getter for obtaining unmodifiable view on objects set. Constant.
   * @return Unmodifiable view on objects set.
   */
  final public Set<SMGObject> getObjects() {
    return Collections.unmodifiableSet(this.objects);
  }

  /**
   * Getter for obtaining unmodifiable view on Has-Value edges set. Constant.
   * @return Unmodifiable view on Has-Value edges set.
   */
  final public Set<SMGEdgeHasValue> getHVEdges() {
    return Collections.unmodifiableSet(this.hv_edges);
  }

  /**
   * Getter for obtaining unmodifiable view on Points-To edges set. Constant.
   * @return Unmodifiable view on Points-To edges set.
   */
  final public Set<SMGEdgePointsTo> getPTEdges() {
    return Collections.unmodifiableSet(this.pt_edges);
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
   * TODO: Long search (iteration) can be a performance problem
   */
  final public SMGObject getObjectPointedBy(Integer pValue) {
    if ( ! this.values.contains(pValue)) {
      throw new IllegalArgumentException("Value [" + pValue + "] not in SMG");
    }

    for (SMGEdgePointsTo edge: this.pt_edges) {
      if (pValue == edge.getValue()) {
        return edge.getObject();
      }
    }

    return null;
  }

  /**
   * Back-end method for other flavors of getValuesForObject. Constant.
   *
   * Throws {@link IllegalArgumentException} if {@link pObject} is
   * not present in the SMG.
   *
   * @param pObject An origin object.
   * @param pOffset Requested object offset, or null
   * @return An unmodifiable set of Has-Value edges leading from object
   * {@link pObject}. If {@link} pOffset is not null, only the edges with
   * offset=={@link pOffset} are included in the set.
   *
   * TODO: Long search (iteration) can be a performance problem
   */
  final private Set<SMGEdgeHasValue> getValuesForObject(SMGObject pObject, Integer pOffset) {
    if ( ! this.objects.contains(pObject)){
      throw new IllegalArgumentException("Object [" + pObject + "] not in SMG");
    }

    HashSet<SMGEdgeHasValue> toReturn = new HashSet<>();
    for (SMGEdgeHasValue edge: this.hv_edges) {
      if (edge.getObject() == pObject && (pOffset == null || edge.getOffset() == pOffset)) {
        toReturn.add(edge);
      }
    }

    return Collections.unmodifiableSet(toReturn);
  }

  /**
   * Getter for obtaining all Has-Value edges leading from object {@link pObject}.
   * Constant.
   *
   * Throws {@link IllegalArgumentException} if {@link pObject} is
   * not present in the SMG.
   *
   * @param pObject An origin object
   * @return An unmodifiable set of all Has-Value edges leading from {@link pObject}
   */
  final public Set<SMGEdgeHasValue> getValuesForObject(SMGObject pObject) {
    return getValuesForObject(pObject, null);
  }

  /**
   * Getter for obtaining Has-Value edges leading from object {@link pObject}
   * at offset {@link pOffset}. Constant.
   *
   * Throws {@link IllegalArgumentException} if {@link pObject} is
   * not present in the SMG.
   *
   * @param pObject Origin object.
   * @param pOffset Requested offset.
   * @return An unmodifiable set of all Has-Value edges leading from
   * {@link pObject} at the offset {
   */
  final public Set<SMGEdgeHasValue> getValuesForObject(SMGObject pObject, int pOffset) {
    return getValuesForObject(pObject, Integer.valueOf(pOffset));
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
    if ( ! this.objects.contains(pObject)) {
      throw new IllegalArgumentException("Object [" + pObject + "] not in SMG");
    }

    return this.object_validity.get(pObject).booleanValue();
  }

  /**
   * Getter for obtaining SMG machine model. Constant.
   * @return SMG machine model
   */
  final public MachineModel getMachineModel() {
    return this.machine_model;
  }
}

class SMGConsistencyVerifier {
  private SMGConsistencyVerifier() {} /* utility class */

  /**
   * Simply log a message about a result of a single check.
   *
   * @param pResult A result of the check
   * @param pLogger A logger instance to which the message will be sent
   * @param pMessage A message to log
   * @return True, if the check was successful (equivalent to { @link pResult }
   */
  static private boolean verifySMGProperty(boolean pResult, LogManager pLogger, String pMessage) {
    pLogger.log(Level.FINEST, "Checking SMG consistency: ", pMessage, ":", pResult);
    return pResult;
  }

  /**
   * A consistency checks related to the NULL object
   *
   * @param pLogger A logger to record results
   * @param pSmg A SMG to verify
   * @return True, if {@link pSmg} satisfies all consistency criteria
   */
  static private boolean verifyNullObject(LogManager pLogger, SMG pSmg) {
    Integer null_value = null;

    // Find a null value in values
    for (Integer value: pSmg.getValues()) {
      if (pSmg.getObjectPointedBy(value) == pSmg.getNullObject()) {
        null_value = value;
        break;
      }
    }

    // Verify that one value pointing to NULL object is present in values
    if (null_value == null) {
      pLogger.log(Level.SEVERE, "SMG inconsistent: no value pointing to null object");
      return false;
    }

    // Verify that NULL value returned by getNullValue() points to NULL object
    if (pSmg.getObjectPointedBy(pSmg.getNullValue()) != pSmg.getNullObject()) {
      pLogger.log(Level.SEVERE, "SMG inconsistent: null value not pointing to null object");
      return false;
    }

    // Verify that the value found in values is the one returned by getNullValue()
    if (pSmg.getNullValue() != null_value){
      pLogger.log(Level.SEVERE, "SMG inconsistent: null value in values set not returned by getNullValue()");
      return false;
    }

    // Verify that NULL object has no value
    if (! pSmg.getValuesForObject(pSmg.getNullObject()).isEmpty()) {
      pLogger.log(Level.SEVERE, "SMG inconsistent: null object has some value");
      return false;
    }

    // Verify that the NULL object is invalid
    if (pSmg.isObjectValid(pSmg.getNullObject())) {
      pLogger.log(Level.SEVERE, "SMG inconsistent: null object is not invalid");
      return false;
    }

    // Verify that the size of the NULL object is zero
    if (pSmg.getNullObject().getSizeInBytes() != 0) {
      pLogger.log(Level.SEVERE, "SMG inconsistent: null object does not have zero size");
      return false;
    }

    return true;
  }

  /**
   * Verifies that invalid regions do not have any Has-Value edges, as this
   * is forbidden in consistent SMGs
   *
   * @param pLogger A logger to record results
   * @param pSmg A SMG to verify
   * @return True, if {@link pSmg} satisfies all consistency criteria.
   */
  static private boolean verifyInvalidRegionsHaveNoHVEdges(LogManager pLogger, SMG pSmg) {
    for (SMGObject obj : pSmg.getObjects()) {
      if (pSmg.isObjectValid(obj)) {
        continue;
      }
      // Verify that the HasValue edge set for this invalid object is empty
      if (pSmg.getValuesForObject(obj).size() > 0) {
        pLogger.log(Level.SEVERE, "SMG inconsistent: invalid object has a HVEdge");
        return false;
      }
    }

    return true;
  }

  /**
   * Verifies that any fields (as designated by Has-Value edges) do not
   * exceed the boundary of the object.
   *
   * @param pLogger A logger to record results
   * @param pObject An object to verify
   * @param pSmg A SMG to verify
   * @return True, if {@link pObject} in {@link pSmg} satisfies all consistency criteria. False otherwise.
   */
  static private boolean checkSingleFieldConsistency(LogManager pLogger, SMGObject pObject, SMG pSmg) {

    // For all fields in the object, verify that sizeof(type)+field_offset < object_size
    for (SMGEdgeHasValue hvEdge : pSmg.getValuesForObject(pObject)) {
      if ((hvEdge.getOffset() + hvEdge.getSizeInBytes(pSmg.getMachineModel())) > pObject.getSizeInBytes()) {
        pLogger.log(Level.SEVERE, "SMG inconistent: field exceedes boundary of the object");
        pLogger.log(Level.SEVERE, "Object: ", pObject);
        pLogger.log(Level.SEVERE, "Field: ", hvEdge);
        return false;
      }
    }
    return true;
  }

  /**
   * Verify all objects satisfy the Field Consistency criteria
   * @param pLogger A logger to record results
   * @param pSmg A SMG to verify
   * @return True, if {@link pSmg} satisfies all consistency criteria. False otherwise.
   */
  static private boolean verifyFieldConsistency(LogManager pLogger, SMG pSmg) {
    for (SMGObject obj : pSmg.getObjects()) {
      if (! checkSingleFieldConsistency(pLogger, obj, pSmg)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Verify that the edges are consistent in the SMG
   *
   * @param pLogger A logger to record results
   * @param pSmg A SMG to verify
   * @param pEdges A set of edges for consistency verification
   * @return True, if all edges in {@link pEdges} satisfy consistency criteria. False otherwise.
   */
  static private boolean verifyEdgeConsistency(LogManager pLogger, SMG pSmg, Set<? extends SMGEdge> pEdges) {
    ArrayList<SMGEdge> to_verify = new ArrayList<>();
    to_verify.addAll(pEdges);

    while (to_verify.size() > 0) {
      SMGEdge edge = to_verify.get(0);
      to_verify.remove(0);

      // Verify that the object assigned to the edge exists in the SMG
      if (!pSmg.getObjects().contains(edge.getObject())) {
        pLogger.log(Level.SEVERE, "SMG inconsistent: Edge from a nonexistent object");
        pLogger.log(Level.SEVERE, "Edge :", edge);
        return false;
      }

      // Verify that the value assigned to the edge exists in the SMG
      if (! pSmg.getValues().contains(edge.getValue())) {
        pLogger.log(Level.SEVERE, "SMG inconsistent: Edge to a nonexistent value");
        pLogger.log(Level.SEVERE, "Edge :", edge);
        return false;
      }

      // Verify that the edge is consistent to all remaining edges
      //  - edges of different type are inconsistent
      //  - two Has-Value edges are inconsistent iff:
      //    - leading from the same object AND
      //    - have same type AND
      //    - have same offset AND
      //    - leading to DIFFERENT values
      //  - two Points-To edges are inconsistent iff:
      //    - different values point to same place (object, offse)
      //    - same values do not point to the same place
      for (SMGEdge other_edge : to_verify) {
        if (! edge.isConsistentWith(other_edge)) {
          pLogger.log(Level.SEVERE, "SMG inconsistent: inconsistent edges");
          pLogger.log(Level.SEVERE, "First edge:  ", edge);
          pLogger.log(Level.SEVERE, "Second edge: ", other_edge);
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Verify a single SMG if it meets all consistency criteria.
   * @param pLogger A logger to record results
   * @param pSmg A SMG to verify
   * @return True, if {@link pSmg} satisfies all consistency criteria
   */
  static public boolean verifySMG(LogManager pLogger, SMG pSmg) {
    boolean toReturn = true;
    pLogger.log(Level.FINEST, "Starting constistency check of a SMG");

    toReturn = toReturn && verifySMGProperty(
        verifyNullObject(pLogger, pSmg),
        pLogger,
        "null object invariants hold");
    toReturn = toReturn && verifySMGProperty(
        verifyInvalidRegionsHaveNoHVEdges(pLogger, pSmg),
        pLogger,
        "invalid regions have no outgoing edges");
    toReturn = toReturn && verifySMGProperty(
        verifyFieldConsistency(pLogger, pSmg),
        pLogger,
        "field consistency");
    toReturn = toReturn && verifySMGProperty(
        verifyEdgeConsistency(pLogger, pSmg, pSmg.getHVEdges()),
        pLogger,
        "Has Value edge consistency");
    toReturn = toReturn && verifySMGProperty(
        verifyEdgeConsistency(pLogger, pSmg, pSmg.getPTEdges()),
        pLogger,
        "Points To edge consistency");

    pLogger.log(Level.FINEST, "Ending consistency check of a SMG");

    return toReturn;
  }
}
