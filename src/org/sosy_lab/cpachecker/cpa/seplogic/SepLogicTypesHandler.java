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
import org.sosy_lab.cpachecker.cpa.types.Type.Primitive;
import org.sosy_lab.cpachecker.cpa.types.Type.TypeClass;
import org.sosy_lab.cpachecker.cpa.types.TypesElement;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Used by {@link SeplogicTransferRelation} to
 * get informationen based on Types
 *
 *@author Philipp Korth
 */
public class SepLogicTypesHandler {

  /**
   * Try to convert the given string representation to a
   * {@link}Type.
   * @param expression String representing type name
   * @return Type
   */
  public Type parseToType(String expression) {
    Type type = null;
    String expr = expression.trim();

    // Self defined type?
    if (currentTypedefs.containsKey(expr)) {
      type = currentTypedefs.get(expr);
      // If its a pointer , look what it points to
      if (type.getTypeClass().equals(TypeClass.POINTER)) {
        type = parseToType(type.getDefinition().replace('*', ' ').trim());
      }
      // Struct ?
    } else if (currentTypedefs.containsKey(structName(expr))) { return parseToType(structName(expr)); }
    return type;
  }

  /**
   * Determines if the given expression is a primitive type
   * @param expression String expression from the code
   * @return Primitive?
   */
  public boolean isPrimitiveType(String expression) {

    // Primitives can be signed or unsigned, remove those parts for now
    String expr = expression.replaceAll("unsigned", "");
    expr = expr.replaceAll("signed", "");
    expr = expr.trim();

    for (Primitive primitive : Primitive.values()) {
      if (primitive.toString().equals(expr))
        return true;
    }
    return false;
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
