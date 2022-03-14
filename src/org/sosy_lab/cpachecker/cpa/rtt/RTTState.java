// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.rtt;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.common.Appenders.AbstractAppender;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;

public class RTTState extends AbstractAppender implements LatticeAbstractState<RTTState> {

  public static final String KEYWORD_THIS = "this";

  public static final String NULL_REFERENCE = "null";

  /**
   * the map that keeps the name of ReferenceVariables and their constant Java Run Time Class
   * Objects
   */
  private final Map<String, String> constantsMap;

  /** this is a convenience map that gives the unique Identification of each Object back */
  private final Map<String, String> identificationMap;

  /** this is a convenience map that gives the runTimeClassType of each Object back */
  private final Map<String, String> classTypeMap;

  private final Set<String> staticFieldVariables = new HashSet<>();

  private final Set<String> nonStaticFieldVariables = new HashSet<>();

  /** Marks the current unique Object Scope this states belons to. */
  private String classObjectScope;

  /** Unique Object Scope Stack */
  private final Deque<String> classObjectStack;

  public RTTState() {
    constantsMap = new HashMap<>();
    identificationMap = new HashMap<>();
    classTypeMap = new HashMap<>();
    classObjectStack = new ArrayDeque<>();
    classObjectScope = NULL_REFERENCE;
    constantsMap.put(KEYWORD_THIS, NULL_REFERENCE);
  }

  private RTTState(
      Map<String, String> pConstantsMap,
      Map<String, String> pIdentificationMap,
      Map<String, String> pClassTypeMap,
      String pClassObjectScope,
      Deque<String> pClassObjectStack) {
    constantsMap = pConstantsMap;
    identificationMap = pIdentificationMap;
    classTypeMap = pClassTypeMap;
    classObjectStack = pClassObjectStack;
    classObjectScope = pClassObjectScope;
  }

  /**
   * Assigns a Java Run Time Class Type to the variable and puts it in the map
   *
   * @param variableName name of the variable.
   * @param object value to be assigned.
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

  void addFieldVariable(JFieldDeclaration pFieldDeclaration) {
    String name = pFieldDeclaration.getName();

    if (pFieldDeclaration.isStatic()) {
      staticFieldVariables.add(name);
    } else {
      nonStaticFieldVariables.add(name);
    }
  }

  public boolean isKnown(String pFieldName) {
    return staticFieldVariables.contains(pFieldName)
        || nonStaticFieldVariables.contains(pFieldName);
  }

  public boolean isKnownAsStatic(String pFieldName) {
    return staticFieldVariables.contains(pFieldName);
  }

  public boolean isKnownAsDynamic(String pFieldName) {
    return nonStaticFieldVariables.contains(pFieldName);
  }

  /**
   * Assigns a Java Run Time Class Type to the variable and puts it in the map
   *
   * @param variableName name of the variable.
   * @param javaRunTimeClassName value to be assigned.
   */
  private void assignNewUniqueObject(String variableName, String javaRunTimeClassName) {
    checkNotNull(variableName);
    checkNotNull(javaRunTimeClassName);

    String iD;
    String uniqueObject;
    if (javaRunTimeClassName.equals(NULL_REFERENCE)) {
      iD = "";
      uniqueObject = javaRunTimeClassName;

    } else {
      iD = Integer.toString(RTTTransferRelation.nextId());
      uniqueObject = NameProvider.getInstance().getUniqueObjectName(javaRunTimeClassName, iD);
    }

    identificationMap.put(uniqueObject, iD);
    classTypeMap.put(uniqueObject, javaRunTimeClassName);
    constantsMap.put(variableName, uniqueObject);
  }

  void forget(String variableName) {
    String oldValue = constantsMap.get(variableName);

    if (oldValue != null
        && !oldValue.equals(NULL_REFERENCE)
        && !constantsMap.containsValue(oldValue)) {
      forgetObject(oldValue);
    }

    constantsMap.remove(variableName);
  }

  /**
   * This method drops all entries belonging to the stack frame of a function. Additionally, it
   * retrieves the Object scope of the returned to function. This method should be called right
   * before leaving a function.
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
  @Override
  public RTTState join(RTTState other) {
    int size = Math.min(constantsMap.size(), other.constantsMap.size());

    Map<String, String> newConstantsMap = Maps.newHashMapWithExpectedSize(size);
    Map<String, String> newIdentificationMap = new HashMap<>(0);
    Map<String, String> newClassTypeMap = new HashMap<>(0);

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

    // TODO no this for unequal scope (Is it possible)

    return new RTTState(
        newConstantsMap, newIdentificationMap, newClassTypeMap, classObjectScope, classObjectStack);
  }

  /**
   * This method decides if this element is less or equal than the other element, based on the order
   * imposed by the lattice.
   *
   * @param other the other element
   * @return true, if this element is less or equal than the other element, based on the order
   *     imposed by the lattice
   */
  @Override
  public boolean isLessOrEqual(RTTState other) {

    // this element is not less or equal than the other element, if it contains less elements
    if (constantsMap.size() < other.constantsMap.size()) {
      return false;
    }

    // this element is not less or equal than the other element,
    // if any one Java Class Types of the other element differs from the Java Class Type in this
    // element
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
    Deque<String> newClassObjectStack = new ArrayDeque<>(old.classObjectStack);
    // TODO Investigate if this works
    return new RTTState(
        new HashMap<>(old.constantsMap),
        new HashMap<>(old.identificationMap),
        new HashMap<>(old.classTypeMap),
        old.classObjectScope,
        newClassObjectStack);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof RTTState)) {
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
    constantsMap.remove(varName);
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

  Deque<String> getClassObjectStack() {
    return classObjectStack;
  }

  /**
   * Assigns a new Object Scope either from a run Time Type, or a unique Object. In case of
   * RunTimeTyp, a new unique object will be created, In case of unique Object, it will simply be
   * referenced to this and the object scope
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
