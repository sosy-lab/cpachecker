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
package org.sosy_lab.cpachecker.cfa.types.java;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.types.AFunctionType;

/**
 * Description of a Java method through its return type and list of (possibly variable) parameters.
 */
public class JMethodType extends AFunctionType implements JType {

  private static final JMethodType UNRESOLVABLE_TYPE = new JMethodType(
     new JSimpleType(JBasicType.UNSPECIFIED), new ArrayList<JType>(), false);

  /**
   * Creates a new <code>JMethodType</code> object that stores the given information.
   *
   * @param pReturnType the return type of the method this object describes
   * @param pParameters the list of parameters the described method takes
   * @param pTakesVarArgs if <code>true</code>, the described method takes a variable amount of
   *        arguments, otherwise not
   */
  public JMethodType(JType pReturnType, List<JType> pParameters, boolean pTakesVarArgs) {
    super(pReturnType, pParameters, pTakesVarArgs);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<JType> getParameters() {
    return (List<JType>) super.getParameters();
  }

  /**
   * Returns a {@link JMethodType} object that describes an unresolvable method.
   *
   * @return a {@link JMethodType} object that describes an unresolvable method
   */
  public static JMethodType createUnresolvableType() {
    return UNRESOLVABLE_TYPE;
  }

  @Override
  public int hashCode() {
      final int prime = 31;
      int result = 7;
      result = prime * result + super.hashCode();
      return result;
  }

  @Override
  public boolean equals(Object obj) {
      return super.equals(obj);
  }
}
