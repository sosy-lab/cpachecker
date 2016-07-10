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

import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Represents a C language stack frame
 */
final public class CLangStackFrame {
  static final String RETVAL_LABEL = "___cpa_temp_result_var_";

  /**
   * Function to which this stack frame belongs
   */
  private final CFunctionDeclaration stack_function;

  /**
   * A mapping from variable names to a set of SMG objects, representing
   * local variables.
   */
  final HashMap <String, SMGRegion> stack_variables = new HashMap<>();

  /**
   * An object to store function return value
   */
  final SMGRegion returnValueObject;

  /**
   * Constructor. Creates an empty frame.
   *
   * @param pDeclaration Function for which the frame is created
   *
   * TODO: [PARAMETERS] Create objects for function parameters
   */
  public CLangStackFrame(CFunctionDeclaration pDeclaration, MachineModel pMachineModel) {
    stack_function = pDeclaration;
    CType returnType = pDeclaration.getType().getReturnType().getCanonicalType();
    if (returnType instanceof CVoidType) {
      // use a plain int as return type for void functions
      returnValueObject = null;
    } else {
      int return_value_size = pMachineModel.getSizeof(returnType);
      returnValueObject = new SMGRegion(return_value_size, CLangStackFrame.RETVAL_LABEL);
    }
  }

  /**
   * Copy constructor.
   *
   * @param pFrame Original frame
   */
  public CLangStackFrame(CLangStackFrame pFrame) {
    stack_function = pFrame.stack_function;
    stack_variables.putAll(pFrame.stack_variables);
    returnValueObject = pFrame.returnValueObject;
  }


  /**
   * Adds a SMG object pObj to a stack frame, representing variable pVariableName
   *
   * Throws {@link IllegalArgumentException} when some object is already
   * present with the name pVariableName
   *
   * @param pVariableName A name of the variable
   * @param pObject An object to put into the stack frame
   */
  public void addStackVariable(String pVariableName, SMGRegion pObject) {
    if (stack_variables.containsKey(pVariableName)) {
      throw new IllegalArgumentException("Stack frame for function '" +
                                       stack_function.toASTString() +
                                       "' already contains a variable '" +
                                       pVariableName + "'");
    }

    stack_variables.put(pVariableName, pObject);
  }

  /* ********************************************* */
  /* Non-modifying functions: getters and the like */
  /* ********************************************* */

  /**
   * @return String representation of the stack frame
   */
  @Override
  public String toString() {
    StringBuilder to_return = new StringBuilder("<");
    for (SMGRegion region : stack_variables.values()) {
      to_return.append(" ").append(region);
    }
    to_return.append(" >");
    return to_return.toString();
  }

  public void removeVariable(String pName) {
    if (RETVAL_LABEL.equals(pName)) {
      // Do nothing for the moment
    } else {
      stack_variables.remove(pName);
    }
  }

  /**
   * Getter for obtaining an object corresponding to a variable name
   *
   * Throws {@link NoSuchElementException} when passed a name not present
   *
   * @param pName Variable name
   * @return SMG object corresponding to pName in the frame
   */
  public SMGRegion getVariable(String pName) {

    if (pName.equals(RETVAL_LABEL) && returnValueObject != null) {
      return returnValueObject;
    }

    SMGRegion to_return = stack_variables.get(pName);

    if (to_return == null) {
      throw new NoSuchElementException("No variable with name '" +
                                       pName + "' in stack frame for function '" +
                                       stack_function.toASTString() + "'");
    }

    return to_return;
  }

  /**
   * @param pName Variable name
   * @return True if variable pName is present, false otherwise
   */
  public boolean containsVariable(String pName) {
    if (pName.equals(RETVAL_LABEL)) {
      return returnValueObject != null;
    } else {
      return stack_variables.containsKey(pName);
    }
  }

  /**
   * @return Declaration of a function corresponding to the frame
   */
  public CFunctionDeclaration getFunctionDeclaration() {
    return stack_function;
  }

  /**
   * @return a mapping from variables name to SMGObjects
   */
  public Map<String, SMGRegion> getVariables() {
    return Collections.unmodifiableMap(stack_variables);
  }

  /**
   * @return a set of all objects: return value object, variables, parameters
   */
  public Set<SMGObject> getAllObjects() {
    HashSet<SMGObject> retset = new HashSet<>();
    retset.addAll(stack_variables.values());
    if (returnValueObject != null) {
      retset.add(returnValueObject);
    }

    return Collections.unmodifiableSet(retset);
  }

  /**
   * @return an {@link SMGObject} reserved for function return value
   */
  public SMGRegion getReturnObject() {
    return returnValueObject;
  }

  /**
   * returns true if stack contains the given variable, else false.
   */
  public boolean hasVariable(String var) {
    return stack_variables.containsKey(var);
  }

  public void clearStackVariables() {
    stack_variables.clear();
  }
}
