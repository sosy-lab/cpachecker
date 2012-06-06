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
package org.sosy_lab.cpachecker.cpa.octagon;

import java.util.Map.Entry;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.octagon.NumArray;
import org.sosy_lab.cpachecker.util.octagon.Octagon;
import org.sosy_lab.cpachecker.util.octagon.OctagonManager;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * An element of octagon abstract domain. This element contains an {@link Octagon} which
 * is the concrete representation of the octagon and a map which
 * provides a mapping from variable names to variables.
 * see {@link Variable}.
 *
 */
class OctState implements AbstractState{

  // the octagon representation
  private Octagon octagon;

  // mapping from variable name to its identifier
  private BiMap<String, Integer> variableToIndexMap;

  private OctState previousState;

  // also top element
  public OctState() {
    octagon = OctagonManager.universe(0);
    variableToIndexMap = HashBiMap.create();
    previousState = null;
  }

  public OctState(Octagon oct, BiMap<String, Integer> map, OctState previousElement){
    octagon = oct;
    variableToIndexMap = map;
    this.previousState = previousElement;
  }

  @Override
  public boolean equals(Object pObj) {
    if(!(pObj instanceof OctState)) {
      return false;
    }
    OctState otherOct = (OctState) pObj;
    return this.octagon.equals(otherOct.octagon);
  }

  @Override
  public int hashCode() {
    return octagon.hashCode();
  }

  public void printOctagon() {
    OctagonManager.print(octagon);
  }

  @Override
  public String toString() {
    return variableToIndexMap + " [octagon]: " + octagon;
  }

  public Octagon getOctagon() {
    return octagon;
  }

  public int sizeOfVariables(){
    return variableToIndexMap.size();
  }

  public OctState getPreviousState() {
    return previousState;
  }

  public void setPreviousState(OctState pPreviousElement) {
    this.previousState = pPreviousElement;
  }

  public BiMap<String, Integer> getVariableToIndexMap() {
    return variableToIndexMap;
  }

  public void addVariable(String pVarName, boolean pIsGlobal,
      String pFunctionName) {

    if(sizeOfVariables() == 0){

    }
  }

  @Override
  protected OctState clone() {
    Octagon newOct = OctagonManager.full_copy(octagon);
    BiMap<String, Integer> newMap = HashBiMap.create();

    for(Entry<String, Integer> e: variableToIndexMap.entrySet()){
      newMap.put(e.getKey(), e.getValue());
    }

    return new OctState(newOct, newMap, this.previousState);
  }

  public boolean addVar(String pVarName, String pFunctionName, boolean pIsGlobal) {
    // create variable name
    String varName = pIsGlobal? pVarName : pFunctionName + "::" + pVarName;

    // normally it should not contain
    if(!variableToIndexMap.containsKey(pVarName)){
      int sizeOfMap = variableToIndexMap.size();
      variableToIndexMap.put(varName, sizeOfMap);
    }
    // TODO
    return false;
  }

  public void forget(String pVariableName) {
    OctagonManager.forget(octagon, getVariableIndexFor(pVariableName));
  }

  public void assignConstant(String pVariableName, long pLongValue) {

    if(pVariableName.contains("NONDET") || pVariableName.contains("NONDET")){
      forget(pVariableName);
    }

    else{
      NumArray arr = getArrayForLiteral(pLongValue);
      octagon = OctagonManager.assingVar(octagon, getVariableIndexFor(pVariableName), arr);
      OctagonManager.num_clear_n(arr, size() + 1);
    }
  }

  protected int getVariableIndexFor(String pVariableName){
    return variableToIndexMap.get(pVariableName);
  }

  public void declareVariable(String pVariableName) {
    assert(!variableToIndexMap.containsKey(pVariableName));
    variableToIndexMap.put(pVariableName, size());
    octagon = OctagonManager.addDimensionAndEmbed(octagon, 1);
  }

  private NumArray getArrayForLiteral(long pLongValue){
    NumArray arr = OctagonManager.init_num_t(size() + 1);
    for(int i = 0; i<variableToIndexMap.size(); i++){
      OctagonManager.num_set_int(arr, i, 0);
    }
    OctagonManager.num_set_int(arr, size(), (int)pLongValue);
    return arr;
  }

  public int size(){
    return variableToIndexMap.size();
  }

  public void addConstraint(int pType, int pLVarIdx, int pRVarIdx, int pConstant) {
    NumArray arr = OctagonManager.init_num_t(4);
    OctagonManager.num_set_int(arr, 0, pType);
    OctagonManager.num_set_int(arr, 1, pLVarIdx);
    OctagonManager.num_set_int(arr, 2, pRVarIdx);
    OctagonManager.num_set_int(arr, 3, pConstant);
    octagon = OctagonManager.addBinConstraint(octagon, 1, arr);
    OctagonManager.num_clear_n(arr, 4);
  }

  public void assignVariable(String pLeftVarName, String pRightVarName, int coef) {

    if(pLeftVarName.contains("NONDET") || pRightVarName.contains("NONDET")){
      forget(pLeftVarName);
    }

    else{
      NumArray arr = getArrayForVariable(getVariableIndexFor(pRightVarName), coef);
      octagon = OctagonManager.assingVar(octagon, getVariableIndexFor(pLeftVarName), arr);
      OctagonManager.num_clear_n(arr, size() + 1);
    }
  }

  private NumArray getArrayForVariable(int pVariableIndexFor, int coef) {
    NumArray arr = OctagonManager.init_num_t(size() + 1);
    for(int i = 0; i<variableToIndexMap.size(); i++){
      if(i == pVariableIndexFor){
        OctagonManager.num_set_int(arr, i, coef);
      }
      else{
        OctagonManager.num_set_int(arr, i, 0);
      }
    }
    OctagonManager.num_set_int(arr, size(), 0);
    return arr;
  }

  public void assignmentOfBinaryExp(String pAssignedVar, String pLeftVarName,
      int pLeftVarCoef, String pRightVarName, int pRightVarCoef, int pConstVal) {
    int leftVarIdx;
    int rightVarIdx;
    int leftVarCoef;
    int rightVarCoef;

    if(pAssignedVar.contains("NONDET") ||
        (pLeftVarName != null && pLeftVarName.contains("NONDET")) ||
        (pRightVarName != null && pRightVarName.contains("NONDET"))){
      forget(pAssignedVar);
    }
    else{
      if(pLeftVarName == null){
        leftVarIdx = -1;
        leftVarCoef = 0;
      }
      else{
        leftVarIdx = getVariableIndexFor(pLeftVarName);
        leftVarCoef = pLeftVarCoef;
      }

      if(pRightVarName == null){
        rightVarIdx = -1;
        rightVarCoef = 0;
      }
      else{
        rightVarIdx = getVariableIndexFor(pRightVarName);
        rightVarCoef = pRightVarCoef;
      }

      int idxForAssignedVar = getVariableIndexFor(pAssignedVar);

      NumArray arr = getArrayForVariableAndConstant(
          leftVarIdx, leftVarCoef, rightVarIdx, rightVarCoef, pConstVal);

      octagon = OctagonManager.assingVar(octagon, idxForAssignedVar, arr);
      OctagonManager.num_clear_n(arr, size() + 1);
    }
  }

  private NumArray getArrayForVariableAndConstant(int pLeftVarIdx, int pLeftVarCoef, int pRightVarIdx, int pRightVarCoef, int pConstVal) {
    NumArray arr = OctagonManager.init_num_t(size() + 1);
    for(int i = 0; i<variableToIndexMap.size(); i++){
      if(i == pLeftVarIdx){
        OctagonManager.num_set_int(arr, i, pLeftVarCoef);
      }
      else if(i == pRightVarIdx){
        OctagonManager.num_set_int(arr, i, pRightVarCoef);
      }
      else{
        OctagonManager.num_set_int(arr, i, 0);
      }
    }
    OctagonManager.num_set_int(arr, size(), pConstVal);
    return arr;
  }

  public boolean isEmpty() {
    return OctagonManager.isEmpty(octagon);
  }

  // keep pSizeOfpreviousElem dimensions at the beginning and remove the rest
  public void removeLocalVariables(OctState pPreviousElem, int noOfGlobalVars) {
    int noOfLocalVars = (size()- pPreviousElem.size());

    for(int i = size(); i>pPreviousElem.size(); i--){
      String s = variableToIndexMap.inverse().get(i-1);
      variableToIndexMap.remove(s);
    }

    octagon = OctagonManager.removeDimension(octagon, noOfLocalVars);
    assert(OctagonManager.dimension(octagon) == size());
  }

}
