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
package org.sosy_lab.cpachecker.cpa.rtt;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

import org.sosy_lab.common.Appenders.AbstractAppender;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import com.google.common.base.Joiner;


public class RTTState extends AbstractAppender implements AbstractState {


  public static final String KEYWORD_THIS = "this";


  public static final String NULL_REFERENCE = "null";

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
   * Marks the current unique Object Scope this states belons to.
   */
  private String classObjectScope;

  /**
   * Unique Object Scope Stack
   */
  private final Stack<String> classObjectStack;

  public RTTState() {
    constantsMap = new HashMap<>();
    identificationMap = new HashMap<>();
    classTypeMap = new HashMap<>();
    classObjectStack = new Stack<>();
    classObjectScope = NULL_REFERENCE;
    constantsMap.put(KEYWORD_THIS, NULL_REFERENCE);
  }

  private RTTState(Map<String, String> pConstantsMap,
      Map<String, String> pIdentificationMap, Map<String, String> pClassTypeMap,
      String pClassObjectScope, Stack<String> pClassObjectStack) {
    constantsMap = pConstantsMap;
    identificationMap = pIdentificationMap;
    classTypeMap = pClassTypeMap;
    classObjectStack = pClassObjectStack;
    classObjectScope = pClassObjectScope;
  }

  /**
   * Assigns a Java Run Time Class Type to the variable and puts it in the map
   * @param variableName name of the variable.
   * @param value value to be assigned.
   */
  void assignObject(String variableName, String object) {


    checkNotNull(variableName);
    checkNotNull(object);

    if (constantsMap.containsValue(object)) {
      constantsMap.put(variableName, object);
    } else {
      assignNewUniqueObject(variableName, object);
    }
  }

  private void forgetObject(String value) {
    identificationMap.remove(value);
    classTypeMap.remove(value);
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

    if (javaRunTimeClassName.equals(NULL_REFERENCE)) {
     iD = "";
    } else {
     iD = Integer.toString(RTTTransferRelation.nextId());
    }

    String uniqueObject = javaRunTimeClassName + iD;
    identificationMap.put(uniqueObject, iD);
    classTypeMap.put(uniqueObject, javaRunTimeClassName);
    constantsMap.put(variableName, uniqueObject);
  }

  void forget(String variableName) {
    String oldValue = constantsMap.get(variableName);

    if (oldValue != null && !oldValue.equals(NULL_REFERENCE) && !constantsMap.containsValue(oldValue)) {
     forgetObject(oldValue);
    }

    constantsMap.remove(variableName);
  }

  /**
   * This method drops all entries belonging to the stack frame of a function.
   * Additionally, it retrieves the Object scope of the returned to function.
   * This method should be called right before leaving a function.
   *
   * @param functionName the name of the function that is about to be left
   */
  void dropFrame(String functionName) {
    List<String> toDropAll = new ArrayList<>();

    for (String variableName : constantsMap.keySet()) {
      if (variableName.startsWith(functionName + "::")) {
        toDropAll.add(variableName);
      }
    }

    for (String variableNameToDrop : toDropAll) {
      constantsMap.remove(variableNameToDrop);
    }

    retrieveObjectScope();
  }

   private void retrieveObjectScope() {
    // TODO Own Exception for Stack is empty
    classObjectScope = classObjectStack.pop();
    constantsMap.put(KEYWORD_THIS, classObjectScope);
  }

  public String getUniqueObjectFor(String variableName) {
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

  public boolean contains(String variableName) {
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
  RTTState join(RTTState other) {
    int size = Math.min(constantsMap.size(), other.constantsMap.size());

    Map<String, String> newConstantsMap = new HashMap<>(size);
    Map<String, String> newIdentificationMap = new HashMap<>(size);
    Map<String, String> newClassTypeMap = new HashMap<>(size);


    for (Map.Entry<String, String> otherEntry : other.constantsMap.entrySet()) {
      String key = otherEntry.getKey();

      if (Objects.equals(otherEntry.getValue(), constantsMap.get(key))) {
        newConstantsMap.put(key, otherEntry.getValue());
      }
    }

    for (Map.Entry<String, String> otherEntry : other.identificationMap.entrySet()) {
      String key = otherEntry.getKey();

      if (Objects.equals(otherEntry.getValue(), identificationMap.get(key))) {
        newConstantsMap.put(key, otherEntry.getValue());
      }
    }

    for (Map.Entry<String, String> otherEntry : other.classTypeMap.entrySet()) {
      String key = otherEntry.getKey();

      if (Objects.equals(otherEntry.getValue(), classTypeMap.get(key))) {
        newConstantsMap.put(key, otherEntry.getValue());
      }
    }

    //TODO no this for unequal scope (Is it possible)

    return new RTTState(newConstantsMap, newIdentificationMap, newClassTypeMap, classObjectScope, classObjectStack);
  }

  /**
   * This method decides if this element is less or equal than the other element, based on the order imposed by the lattice.
   *
   * @param other the other element
   * @return true, if this element is less or equal than the other element, based on the order imposed by the lattice
   */
  boolean isLessOrEqual(RTTState other) {

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
    if (!classObjectScope.equals(other.getClassObjectScope())) {
      return false;
    }

    // Is this neccessary?
    if (!getClassObjectStack().equals(other.getClassObjectStack())) {
      return false;
    }


    return true;
  }



  public static RTTState copyOf(RTTState old) {
      Stack<String> newClassObjectStack = new Stack<>();
      newClassObjectStack.addAll(old.classObjectStack);
      //TODO Investigate if this works
    return new RTTState(new HashMap<>(old.constantsMap), new HashMap<>(old.identificationMap), new HashMap<>(old.classTypeMap), old.classObjectScope, newClassObjectStack);
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

    RTTState otherElement = (RTTState) other;

    return otherElement.constantsMap.equals(constantsMap);
  }

  @Override
  public int hashCode() {
    return constantsMap.hashCode();
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

  public Map<String, String> getConstantsMap() {
    return constantsMap;
  }

  String getClassObjectScope() {
    return classObjectScope;
  }

   void setClassObjectScope(String classObjectScope) {
    this.classObjectScope = classObjectScope;
  }

  Stack<String> getClassObjectStack() {
    return classObjectStack;
  }

  /**
   * Assigns a new Object Scope either from a run Time Type, or a unique Object.
   * In case of RunTimeTyp, a new unique object will be created,
   * In case of unique Object, it will simply be referenced to this and the object scope
   *
   * @param scope
   */
   void assignThisAndNewObjectScope(String scope) {
    classObjectStack.push(classObjectScope);

    assignObject(KEYWORD_THIS, scope);
    classObjectScope = getKeywordThisUniqueObject();

  }

   public String getKeywordThisUniqueObject() {
     return getUniqueObjectFor(KEYWORD_THIS);
   }

  public String getRunTimeClassOfUniqueObject(String uniqueObject) {
    return classTypeMap.get(uniqueObject);
  }

  @Override
  public void appendTo(Appendable a) throws IOException {
    a.append("[");
    Joiner.on(", ").withKeyValueSeparator("=").appendTo(a, constantsMap);
    a.append("]");
  }

 void assignAssumptionType(String pReferenz, JClassOrInterfaceType pAssignableType) {
    assignNewUniqueObject(pReferenz, pAssignableType.getName());
  }

}