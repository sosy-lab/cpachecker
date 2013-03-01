/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

  public SMG(MachineModel pMachineModel){
    SMGEdgePointsTo nullPointer = new SMGEdgePointsTo(nullAddress, nullObject, 0);

    addObject(nullObject);
    object_validity.put(nullObject, false);

    addValue(nullAddress);
    addPointsToEdge(nullPointer);

    machine_model = pMachineModel;
  }

  public SMG(SMG pHeap) {
    objects.addAll(pHeap.objects);
    values.addAll(pHeap.values);
    hv_edges.addAll(pHeap.hv_edges);
    pt_edges.addAll(pHeap.pt_edges);
    object_validity.putAll(pHeap.object_validity);

    machine_model = pHeap.machine_model;
  }

  final public SMGObject getNullObject(){
    return nullObject;
  }

  final public int getNullValue(){
    return nullAddress;
  }

  final public void addObject(SMGObject pObj) {
    addObject(pObj, true);
  }

  final public void addObject(SMGObject pObj, boolean validity) {
    this.objects.add(pObj);
    this.object_validity.put(pObj, validity);
  }

  final public void addValue(int pValue) {
    this.values.add(Integer.valueOf(pValue));
  }

  final public void addPointsToEdge(SMGEdgePointsTo pEdge){
    this.pt_edges.add(pEdge);
  }

  final public void addHasValueEdge(SMGEdgeHasValue pNewEdge) {
    this.hv_edges.add(pNewEdge);
  }

  final public String valuesToString(){
    return "values=" + values.toString();
  }

  final public String hvToString(){
    return "hasValue=" + hv_edges.toString();
  }

  final public String ptToString(){
    return "pointsTo=" + pt_edges.toString();
  }

  final public Set<Integer> getValues(){
    return Collections.unmodifiableSet(values);
  }

  final public Set<SMGObject> getObjects(){
    return Collections.unmodifiableSet(objects);
  }

  final public Set<SMGEdgeHasValue> getHVEdges(){
    return Collections.unmodifiableSet(hv_edges);
  }

  final public Set<SMGEdgePointsTo> getPTEdges(){
    return Collections.unmodifiableSet(pt_edges);
  }

  /**
   * @param value
   * @return
   *
   * TODO: More documentation
   * TODO: Test
   * TODO: Consistency check: no value can point to more objects
   */
  final public SMGObject getObjectPointedBy(Integer value){
    for (SMGEdgePointsTo edge: pt_edges){
      if (value == edge.getValue()){
        return edge.getObject();
      }
    }

    return null;
  }

  final private HashSet<SMGEdgeHasValue> getValuesForObject(SMGObject pObject, Integer pOffset){
    HashSet<SMGEdgeHasValue> toReturn = new HashSet<>();
    for (SMGEdgeHasValue edge: hv_edges){
      if (edge.getObject() == pObject && (pOffset == null || edge.getOffset() == pOffset)){
        toReturn.add(edge);
      }
    }

    return toReturn;
  }

  final public HashSet<SMGEdgeHasValue> getValuesForObject(SMGObject pObject) {
    return getValuesForObject(pObject, null);
  }

  final public HashSet<SMGEdgeHasValue> getValuesForObject(SMGObject pObject, int pOffset) {
    return getValuesForObject(pObject, Integer.valueOf(pOffset));
  }

  public void setValidity(SMGObject obj, boolean value){
    if (this.objects.contains(obj)){
      this.object_validity.put(obj, value);
    }
    else{
      throw new IllegalArgumentException("Object [" + obj + "] not in SMG");
    }
  }

  public boolean isObjectValid(SMGObject obj){
    if (this.objects.contains(obj)){
      return object_validity.get(obj).booleanValue();
    }
    else{
      throw new IllegalArgumentException("Object [" + obj + "] not in SMG");
    }
  }

  public MachineModel getMachineModel() {
    return machine_model;
  }
}

class SMGConsistencyVerifier{
  private SMGConsistencyVerifier() {} /* utility class */

  static private boolean verifySMGProperty(boolean result, LogManager pLogger, String message){
    pLogger.log(Level.FINEST, "Checking SMG consistency: ", message, ":", result);
    return result;
  }

  static private boolean verifyNullObject(LogManager pLogger, SMG smg){
    Integer null_value = null;

    for(Integer value: smg.getValues()){
      if (smg.getObjectPointedBy(value) == smg.getNullObject()){
        null_value = value;
      }
    }

    if (smg.getObjectPointedBy(smg.getNullValue()) != smg.getNullObject()){
      pLogger.log(Level.SEVERE, "SMG inconsistent: null value not pointing to null object");
      return false;
    }

    if (null_value == null){
      pLogger.log(Level.SEVERE, "SMG inconsistent: no value pointing to null object");
      return false;
    }

    if (! smg.getValuesForObject(smg.getNullObject()).isEmpty()){
      pLogger.log(Level.SEVERE, "SMG inconsistent: null object has some value");
      return false;
    }

    if (smg.isObjectValid(smg.getNullObject())){
      pLogger.log(Level.SEVERE, "SMG inconsistent: null object is not invalid");
      return false;
    }

    if (smg.getNullObject().getSizeInBytes() != 0){
      pLogger.log(Level.SEVERE, "SMG inconsistent: null object does not have zero size");
      return false;
    }

    return true;
  }

  static private boolean verifyInvalidRegionsHaveNoHVEdges(LogManager pLogger, SMG smg){
    for (SMGObject obj : smg.getObjects()){
      if (smg.isObjectValid(obj))
        continue;
      if (smg.getValuesForObject(obj).size() > 0){
        pLogger.log(Level.SEVERE, "SMG inconsistent: invalid object has a HVEdge");
        return false;
      }
    }

    return true;
  }

  static private boolean checkSingleFieldConsistency(LogManager pLogger, SMGObject pObject, SMG smg){
    for (SMGEdgeHasValue hvEdge : smg.getValuesForObject(pObject)){
      if ((hvEdge.getOffset() + hvEdge.getSizeInBytes(smg.getMachineModel())) > pObject.getSizeInBytes()){
        pLogger.log(Level.SEVERE, "SMG inconistent: field exceedes boundary of the object");
        pLogger.log(Level.SEVERE, "Object: ", pObject);
        pLogger.log(Level.SEVERE, "Field: ", hvEdge);
        return false;
      }
    }
    return true;
  }

  static private boolean verifyFieldConsistency(LogManager pLogger, SMG smg){
    for (SMGObject obj : smg.getObjects()){
      if (! checkSingleFieldConsistency(pLogger, obj, smg)){
        return false;
      }
    }

    return true;
  }

  static private boolean verifyHVConsistency(LogManager pLogger, SMG smg){
    ArrayList<SMGEdgeHasValue> to_verify = new ArrayList<>();
    to_verify.addAll(smg.getHVEdges());

    while (to_verify.size() > 0){
      SMGEdgeHasValue edge = to_verify.get(0);
      to_verify.remove(0);
      if (!smg.getObjects().contains(edge.getObject())){
        pLogger.log(Level.SEVERE, "SMG inconsistent: Has Value Edge from a nonexistent object");
        pLogger.log(Level.SEVERE, "Edge :", edge);
        return false;
      }

      if (! smg.getValues().contains(edge.getValue())){
        pLogger.log(Level.SEVERE, "SMG inconsistent: Has Value Edge to a nonexistent value");
        pLogger.log(Level.SEVERE, "Edge :", edge);
        return false;
      }

      for (SMGEdgeHasValue other_edge : to_verify){
        if (! edge.isConsistentWith(other_edge)){
          pLogger.log(Level.SEVERE, "SMG inconsistent: inconsistent Has Value edges");
          pLogger.log(Level.SEVERE, "First edge:  ", edge);
          pLogger.log(Level.SEVERE, "Second edge: ", other_edge);
          return false;
        }
      }
    }
    return true;
  }

  static private boolean verifyPTConsistency(LogManager pLogger, SMG smg){
    ArrayList<SMGEdgePointsTo> to_verify = new ArrayList<>();
    to_verify.addAll(smg.getPTEdges());

    while (to_verify.size() > 0){
      SMGEdgePointsTo edge = to_verify.get(0);
      to_verify.remove(0);

      if (!smg.getValues().contains(edge.getValue())){
        pLogger.log(Level.SEVERE, "SMG inconsistent: Points To edge from a nonexistent value");
        pLogger.log(Level.SEVERE, "Edge : ", edge);
        return false;
      }
      if (!smg.getObjects().contains(edge.getObject())){
        pLogger.log(Level.SEVERE, "SMG inconsistent: Points To edge to a nonexistent object");
        pLogger.log(Level.SEVERE, "Edge : ", edge);
        return false;
      }

      for (SMGEdgePointsTo other_edge : to_verify){
        if (! edge.isConsistentWith(other_edge)){
          pLogger.log(Level.SEVERE, "SMG inconsistent: inconsistent Points To edges");
          pLogger.log(Level.SEVERE, "First edge:  ", edge);
          pLogger.log(Level.SEVERE, "Second edge: ", other_edge);
          return false;
        }
      }
    }

    return true;
  }

  static public boolean verifySMG(LogManager pLogger, SMG smg){
    boolean toReturn = true;
    pLogger.log(Level.FINEST, "Starting constistency check of a SMG");

    toReturn = toReturn && verifySMGProperty(
        verifyNullObject(pLogger, smg),
        pLogger,
        "null object invariants hold");
    toReturn = toReturn && verifySMGProperty(
        verifyInvalidRegionsHaveNoHVEdges(pLogger, smg),
        pLogger,
        "invalid regions have no outgoing edges");
    toReturn = toReturn && verifySMGProperty(
        verifyFieldConsistency(pLogger, smg),
        pLogger,
        "field consistency");
    toReturn = toReturn && verifySMGProperty(
        verifyHVConsistency(pLogger, smg),
        pLogger,
        "Has Value edge consistency");
    toReturn = toReturn && verifySMGProperty(
        verifyPTConsistency(pLogger, smg),
        pLogger,
        "Points To edge consistency");

    pLogger.log(Level.FINEST, "Ending consistency check of a SMG");

    return toReturn;
  }
}
