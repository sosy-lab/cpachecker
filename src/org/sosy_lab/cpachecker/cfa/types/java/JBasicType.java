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

/**
 * A basic Java type. This includes mostly primitive types, but also types like <code>null</code>
 * or even a special unspecified type.
 */
public enum JBasicType {


  UNSPECIFIED(""),
  NULL("null"),
  VOID("void"),
  BYTE("byte"),
  SHORT("short"),
  BOOLEAN("boolean"),
  CHAR("char"),
  INT("int"),
  LONG("long"),
  FLOAT("float"),
  DOUBLE("double"),
  ;

  private final String code;

  private JBasicType(String pCode) {
     code = pCode;
  }

  public boolean isFloatingPointType() {
    return this == FLOAT
        || this == DOUBLE;
  }

  public boolean isIntegerType() {
    return this == BYTE
        || this == CHAR
        || this == SHORT
        || this == INT
        || this == LONG;
  }

  /**
   * Returns an unambiguous String representation of this type.
   * from all other <code>JBasicType</code> enum constants.
   *
   * @return a unique String representation of this type
   */
  public String toASTString() {
    return code;
  }

}
