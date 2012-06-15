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
package org.sosy_lab.cpachecker.cpa.seplogic;

import java.util.HashMap;

import org.sosy_lab.cpachecker.cpa.types.Type;
import org.sosy_lab.cpachecker.cpa.types.Type.FunctionType;
import org.sosy_lab.cpachecker.cpa.types.Type.StructType;
import org.sosy_lab.cpachecker.cpa.types.Type.TypeClass;
import org.sosy_lab.cpachecker.cpa.types.TypesElement;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Used by {@link SeplogicTransferRelation} to
 * determine what has changed in a given TypesElement
 * and get various information.
 *
 *@author Philipp Korth
 */
public class SepLogicTypesHandler {


  /**
   * Returns the size of the given type
   * by converting it into {@link Type}.
   * TODO Write something about lists
   * @param expression String representation of the type
   * @return Integer Value
   */
  public int getSizeOfType(String expression) {
    // Create Type from representation
    Type type = parseToType(expression);

    // Pointer?
    if (type.getTypeClass().equals(TypeClass.POINTER)) {

      // what type does it point to?
      String pointsToTypeName = type.getDefinition().replace('*', ' ');

      // Determine its type
      Type pointsToType = parseToType(pointsToTypeName);

      return pointsToType.sizeOf();
    }

    // No Pointer so far
    return type.sizeOf();

  }

  /**
   * Try to convert the given string representation to a
   * {@link}Type.
   * @param expression String representing type name
   * @return Type
   */
  public Type parseToType(String expression) {
    Type type = null;
    String expr = expression.trim();

    // Is it a self defined type?
    if (currentTypedefs.containsKey(expr)) { return currentTypedefs.get(expr); }
    // Maybe a struct?
    if (currentTypedefs.containsKey(structName(expr))) { return parseToType(structName(expr)); }

    // Something else

    // TODO Do something about the primitives

    return type;
  }

  /**
   * Assuming the given struct-type is a linked list,
   * this method calculates the offset between the list nodes
   * @return offset
   */
  public int getListOffset(StructType list) {

    int offset = 0;

    // Look at each field, ignore the pointer to the next list node
    for (String field : list.getMembers()) {
      Type fieldType = list.getMemberType(field);
      if (fieldType.getTypeClass().equals(TypeClass.POINTER)) {
        // Ok this is the pointer, ignore
        // TODO more detail on points to?
      } else {
        offset += fieldType.sizeOf();
      }

    }

    return offset;

  }

  /**
   * Extends the given expression so that it will
   * define a struct in the typedefs keyset
   * @param expression
   *    Expression to expand
   * @return struct expression
   */
  private String structName(String expression) {
    return "struct " + expression.trim();
  }

  // Manage hashmaps to save actual state
  private HashMap<String, Type> currentVariables;
  private HashMap<String, Type> currentTypedefs;
  private HashMap<String, FunctionType> currentFunctions;



  /**
   * Creates new SepLogicTypesHandler
   */
  public SepLogicTypesHandler() {
    currentVariables = new HashMap<String, Type>();
    currentTypedefs = new HashMap<String, Type>();
    currentFunctions = new HashMap<String, FunctionType>();
  }

  /**
   * Determines if a new type definition has been added since
   * last call.
   * @param typesElement TypesElement from {@link SeplogicTransferRelation}
   * @return New Typedef?
   */
  public boolean gotNewTypeDef(TypesElement typesElement) {
    return typesElement.getTypedefs().size() > currentTypedefs.size();
  }

  /**
   * Determines if a new variable has been added since
   * last call.
   * @param typesElement TypesElement from {@link SeplogicTransferRelation}
   * @return New variable declared?
   */
  public boolean gotNewVariable(TypesElement typesElement) {
    return typesElement.getVariableTypes().size() > currentVariables.size();
  }

  /**
   * Determines if a new variable has been added since
   * last call.
   * @param typesElement TypesElement from {@link SeplogicTransferRelation}
   * @return New function?
   */
  public boolean gotNewFunction(TypesElement typesElement) {
    return typesElement.getFunctions().size() > currentFunctions.size();
  }

  /**
  *
  * @param typesElement
  * @return
  */
  public Type getNewTypeDefs(TypesElement typesElement) {

    String newKey = null;
    for (String key : typesElement.getTypedefs().keySet()) {
      if (!currentVariables.containsKey(key)) {
        newKey = key;
        break;
      }
    }
    return typesElement.getTypedefs().get(newKey);
  }

  /**
   *
   * @param typesElement
   * @return
   */
  public Type getNewVariable(TypesElement typesElement) {

    String newKey = null;
    for (String key : typesElement.getVariableTypes().keySet()) {
      if (!currentVariables.containsKey(key)) {
        newKey = key;
        break;
      }
    }

    return typesElement.getVariableTypes().get(newKey);
  }

  /**
   *@deprecated implement me
   * @param typesElement
   * @return
   */
  @Deprecated
  public FunctionType getNewFunction(TypesElement typesElement) {

    throw new NotImplementedException();

  }

  /**
   * Update current definitions by cloning new ones
   * @param typesElement TypesElement with new definitions
   */
  public void update(TypesElement typesElement) {
    currentVariables = new HashMap<String, Type>(typesElement.getVariableTypes());
    currentTypedefs = new HashMap<String, Type>(typesElement.getTypedefs());
    currentFunctions = new HashMap<String, Type.FunctionType>(typesElement.getFunctions());
  }
}
