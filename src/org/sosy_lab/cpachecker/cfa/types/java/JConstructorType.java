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

/**
 * Description of a constructor method of a Java class.
 */
public class JConstructorType extends JMethodType implements JType {

  private static final long serialVersionUID = -6996173000501454098L;

  private static final JConstructorType UNRESOLVABLE_TYPE =
      new JConstructorType(JClassType.createUnresolvableType(), new ArrayList<>(), false);

  /**
   * Creates a new <code>JConstructorType</code> object with the given attributes.
   *
   * @param pReturnType a {@link JClassOrInterfaceType} describing the class the described
   *    constructor creates
   * @param pParameters the parameters the constructor takes
   * @param pTakesVarArgs if <code>true</code>, the constructor takes a variable amount of
   *        arguments, otherwise not
   */
  public JConstructorType(JClassOrInterfaceType pReturnType, List<JType> pParameters, boolean pTakesVarArgs) {
    super(pReturnType, pParameters, pTakesVarArgs);
  }

  @Override
  public JClassOrInterfaceType getReturnType() {
    return (JClassOrInterfaceType) super.getReturnType();
  }

  /**
   * Returns a <code>JContructorType</code> instance describing an unresolvable constructor.
   *
   * @return a <code>JContructorType</code> instance describing an unresolvable constructor
   */
  public static JConstructorType createUnresolvableConstructorType() {
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
