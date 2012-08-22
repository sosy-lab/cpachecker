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
package org.sosy_lab.cpachecker.cpa.jort;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;


public class JortState implements AbstractState {


  /**
   * the map that keeps the name of ReferenceVariables and their constant  Java Run Time Class Objects
   */
  private final Map<String, String> constantsMap;

  /**
   * this is a convenience map that gives the unique Identification
   * of each Object back
   */
  private final Map<String, String> identificationMap;

  /**
   * this is a convenience map that gives the runTimeClassType
   * of each Object back
   */
  private final Map<String, String> classTypeMap;

  /**
   * this is a convenience set that gives marks every Object which can be erased
   */
 // private final Set<String> toBeErased;



  /**
   * Gives every RunTimeObject its unique Identification
   */
  private final ObjectIdGenerator idGenerator;

  /**
   * Marks the current unique Object Scope this states belons to.
   */
  private String classObjectScope;

  /**
   * Unique Object Scope Stack
   */
  private final Stack<String> classObjectStack;

  public JortState() {
    constantsMap = new HashMap<String, String>();
    identificationMap = new HashMap<String, String>();
    classTypeMap = new HashMap<String, String>();
    idGenerator = new ObjectIdGenerator();
    classObjectStack = new Stack<String>();
    classObjectStack.push("");
    classObjectScope = "";
  //  toBeErased = new HashSet<String>();
  }

  private JortState(Map<String, String> pConstantsMap,
      Map<String, String> pIdentificationMap, Map<String, String> pClassTypeMap,
      ObjectIdGenerator pIdGenerator, String pClassObjectScope, Stack<String> pClassObjectStack) {
    constantsMap = pConstantsMap;
    identificationMap = pIdentificationMap;
    classTypeMap = pClassTypeMap;
    idGenerator = pIdGenerator;
    classObjectStack = pClassObjectStack;
    classObjectScope = pClassObjectScope;
   // toBeErased = pToBeErased;
  }

  /**
   * Assigns a Java Run Time Class Type to the variable and puts it in the map
   * @param variableName name of the variable.
   * @param value value to be assigned.
   */
  void assignObject(String variableName, String object) {


    checkNotNull(variableName);
    checkNotNull(object);

    if(constantsMap.containsValue(object)){
    //  String oldValue = constantsMap.get(variableName);
      constantsMap.put(variableName, object);
    // if(oldValue != null && !oldValue.equals("null") && !constantsMap.containsValue(oldValue)){
       // forgetObject(oldValue);
    //  }
    } else {
      assignNewUniqueObject(variableName, object);
    }
  }

  private void forgetObject(String value) {
    identificationMap.remove(value);
    classTypeMap.remove(value);
   // toBeErased.add(value);
  }

  /**
   * Assigns a Java Run Time Class Type to the variable and puts it in the map
   * @param variableName name of the variable.
   * @param value value to be assigned.
   */
  private void assignNewUniqueObject(String variableName, String javaRunTimeClassName) {
    checkNotNull(variableName);
    checkNotNull(javaRunTimeClassName);



    String iD ;

    if(javaRunTimeClassName.equals("null")){
     iD = "";
    } else {
     iD = idGenerator.getnextId();
    }

    String uniqueObject = javaRunTimeClassName + iD;
    identificationMap.put(uniqueObject, iD);
    classTypeMap.put(uniqueObject, javaRunTimeClassName);
    constantsMap.put(variableName, uniqueObject);
  }

  void forget(String variableName) {
    String oldValue = constantsMap.get(variableName);

    if(oldValue != null && !oldValue.equals("null") && !constantsMap.containsValue(oldValue)){
     forgetObject(oldValue);
    }

    constantsMap.remove(variableName);
  }

  /**
   * This method drops all entries belonging to the stack frame of a function. This method should be called right before leaving a function.
   *
   * @param functionName the name of the function that is about to be left
   */
  void dropFrame(String functionName) {
    List<String> toDropAll = new ArrayList<String>();

    for(String variableName : constantsMap.keySet()) {
      if(variableName.startsWith(functionName + "::")) {
        toDropAll.add(variableName);
      }
    }

    for(String variableNameToDrop : toDropAll) {
      constantsMap.remove(variableNameToDrop);
    }

    classObjectScope = classObjectStack.pop();
  }

   String getUniqueObjectFor(String variableName) {
    return checkNotNull(constantsMap.get(variableName));
  }

   String getRunTimeClassFor(String variableName) {
    checkNotNull(variableName);
    String uniqueObject = getUniqueObjectFor(variableName);
    return classTypeMap.get(uniqueObject);
  }

   String getObjectIDFor(String uniqueObject) {
    checkNotNull(uniqueObject);
    String iD = classTypeMap.get(uniqueObject);
    checkNotNull(iD);
    return iD;
  }

   boolean contains(String variableName) {
    return constantsMap.containsKey(variableName);
  }

   int getSize() {
    return constantsMap.size();
  }

  /**
   * This element joins this element with another element.
   *
   * @param other the other element to join with this element
   * @return a new state representing the join of this element and the other element
   */
  JortState join(JortState other) {
    int size = Math.min(constantsMap.size(), other.constantsMap.size());

    Map<String, String> newConstantsMap = new HashMap<String, String>(size);
    Map<String, String> newIdentificationMap = new HashMap<String, String>(size);
    Map<String, String> newClassTypeMap = new HashMap<String, String>(size);


    for (Map.Entry<String, String> otherEntry : other.constantsMap.entrySet()) {
      String key = otherEntry.getKey();

      if (equal(otherEntry.getValue(), constantsMap.get(key))) {
        newConstantsMap.put(key, otherEntry.getValue());
      }
    }

    for (Map.Entry<String, String> otherEntry : other.identificationMap.entrySet()) {
      String key = otherEntry.getKey();

      if (equal(otherEntry.getValue(), identificationMap.get(key))) {
        newConstantsMap.put(key, otherEntry.getValue());
      }
    }

    for (Map.Entry<String, String> otherEntry : other.classTypeMap.entrySet()) {
      String key = otherEntry.getKey();

      if (equal(otherEntry.getValue(), classTypeMap.get(key))) {
        newConstantsMap.put(key, otherEntry.getValue());
      }
    }

    //TODO Exception for unequal Scope

    return new JortState(newConstantsMap, newIdentificationMap, newClassTypeMap, idGenerator, classObjectScope, classObjectStack);
  }

  /**
   * This method decides if this element is less or equal than the other element, based on the order imposed by the lattice.
   *
   * @param other the other element
   * @return true, if this element is less or equal than the other element, based on the order imposed by the lattice
   */
  boolean isLessOrEqual(JortState other) {

    // this element is not less or equal than the other element, if it contains less elements
    if (constantsMap.size() < other.constantsMap.size()) {
      return false;
    }

    // this element is not less or equal than the other element,
    // if any one Java Class Types of the other element differs from the Java Class Type in this element
    for (Map.Entry<String, String> otherEntry : other.constantsMap.entrySet()) {
      String key = otherEntry.getKey();

      if (!otherEntry.getValue().equals(constantsMap.get(key))) {
        return false;
      }
    }

    // this element is not less or equal, if their scope differ
    if(!classObjectScope.equals(other.getClassObjectScope())){
      return false;
    }

    if(!getClassObjectStack().equals(other.getClassObjectStack())){
      return false;
    }


    return true;
  }



  @Override
  public JortState clone() {
      Stack<String> newClassObjectStack = new Stack<String>();
      newClassObjectStack.addAll(classObjectStack);
      //TODO Investigate if this works
    return new JortState(new HashMap<String, String>(constantsMap), new HashMap<String, String>(identificationMap), new HashMap<String, String>(classTypeMap), new ObjectIdGenerator(idGenerator.nextFreeId), new String(classObjectScope), newClassObjectStack);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (other == null) {
      return false;
    }

    if (!getClass().equals(other.getClass())) {
      return false;
    }

    JortState otherElement = (JortState) other;

    return otherElement.constantsMap.equals(constantsMap);
  }

  @Override
  public int hashCode() {
    return constantsMap.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (Map.Entry<String, String> entry : constantsMap.entrySet()) {
      String key = entry.getKey();
      sb.append(" <");
      sb.append(key);
      sb.append(" = ");
      sb.append(entry.getValue());
      sb.append(">\n");
    }

    return sb.append("] size->  ").append(constantsMap.size()).toString();
  }


  public String getCPAName() {
    return "JavaObjectRuntimeTracker";
  }

  void deleteValue(String varName) {
    this.constantsMap.remove(varName);
  }

  Set<String> getTrackedVariableNames() {
    return constantsMap.keySet();
  }

  Map<String, String> getConstantsMap() {
    return constantsMap;
  }

  public String getClassObjectScope() {
    return classObjectScope;
  }

   void setClassObjectScope(String classObjectScope) {
    this.classObjectScope = classObjectScope;
  }

  public Stack<String> getClassObjectStack() {
    return classObjectStack;
  }

  private class ObjectIdGenerator {


    public ObjectIdGenerator() {
    }

    public ObjectIdGenerator(int startWith) {
      nextFreeId = startWith--;
    }

    private int nextFreeId = 0;

    private int nextId(){
      nextFreeId++;
      return nextFreeId;

    }



    String getnextId() {
      int id = nextId();
      return Integer.toString(id);
    }
  }

   void assignObjectScope(String scope) {
    classObjectScope = scope;
    classObjectStack.push(scope);
  }

//  public Set<String> getToBeErased() {
  //  return toBeErased;
 // }
}