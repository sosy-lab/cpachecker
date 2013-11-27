/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.math.BigDecimal;

import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;


/**
 * Container that stores a number and its inital data type. The type must be
 * from <code>NumerTypes</code>.
 */
public class NumberContainer {

  private CSimpleType type;

  private NumberTypes ntype;

  private BigDecimal number;

  /**
   * Creates a new <code>NumberContainer</code>, given the type and a
   * <code>BigDecimal</code>.
   * @param pType the inital type of the number. Must be from
   *        <code>NumberTypes</code>.
   * @param pNumber the value of the number (must be a <code>BigDecimal</code>)
   */
  public NumberContainer(NumberTypes pType, BigDecimal pNumber) {
    ntype = pType;
    number = pNumber;
  }

  /**
   * Creates a new <code>NumberContainer</code>, given the type and a
   * <code>BigDecimal</code>.
   * @param pType the inital type of the number.
   * @param pNumber the value of the number (must be a <code>BigDecimal</code>)
   */
  public NumberContainer(CSimpleType pType, BigDecimal pNumber) {
    type = pType;
    number = pNumber;
  }

  /**
   * Creates a new <code>NumberContainer</code>, given a type and a
   * <code>String</code> used to create the <code>BigDecimal</code>.
   *
   * @param pType the inital type of the number. Must be from
   *        <code>NumberTypes</code>.
   * @param pNumber the value of the number
   */
  public NumberContainer(NumberTypes pType, String pNumber) {
    ntype = pType;
    number = new BigDecimal(pNumber);
  }

  /**
   * Creates a new <code>NumberContainer</code>, given a type and a
   * <code>String</code> used to create the <code>BigDecimal</code>.
   *
   * @param pType the inital type of the number.
   * @param pNumber the value of the number
   */
  public NumberContainer(CSimpleType pType, String pNumber) {
    type = pType;
    number = new BigDecimal(pNumber);
  }

  /**
   * Returns the C type of the number stored in the container.
   *
   * @return The type of the number stored in the container. Must be a CSimpleType
   *         which represents a numeric type.
   */
  public CSimpleType getType() {
    return type;
  }

  //  /**
  //   * Returns the inital type of the number stored in the container.
  //   *
  //   * @return the type of the number stored in the container.
  //   */
  //  public CSimpleType getType() {
  //    return type;
  //  }


  /**
   * Sets the type of the number stored in the container.
   *
   * @param pType the type of the number. Must be from <code>NumberTypes</code>.
   */
  public void setType(NumberTypes pType) {
    ntype = pType;
  }

  /**
   * Sets the type of the number stored in the container.
   *
   * @param pType the type of the number.
   */
  public void setType(CSimpleType pType) {
    type = pType;
  }

  /**
   * Returns the number stored in the container.
   *
   * @return the number stored in the container
   */
  public BigDecimal getNumber() {
    return number;
  }


  /**
   * Sets the number stored in the container.
   *
   * @param pNumber the number to store in the container
   */
  public void setNumber(BigDecimal pNumber) {
    number = pNumber;
  }


  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "NumberContainer [type=" + type.toString() + ", number=" + number + "]";
  }

}
