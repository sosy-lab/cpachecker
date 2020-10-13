// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.java;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.types.AFunctionType;

/**
 * Description of a Java method through its return type and list of (possibly variable) parameters.
 */
public class JMethodType extends AFunctionType implements JType {

  private static final long serialVersionUID = 1324108617808888102L;

  private static final JMethodType UNRESOLVABLE_TYPE =
      new JMethodType(JSimpleType.getUnspecified(), new ArrayList<>(), false);

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

  @Override
  public JType getReturnType() {
    return (JType) super.getReturnType();
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
