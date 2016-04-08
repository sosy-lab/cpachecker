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
 * Special type for the Java value <code>null</code>.
 */
public class JNullType implements JReferenceType {

  private static final long serialVersionUID = 3755021493601316268L;

  @Override
  public String toASTString(String pDeclarator) {
    return "null";
  }

  @Override
  public int hashCode() {
      final int prime = 31;
      int result = 7;
      return prime * result;
  }

  /**
   * Returns whether a given object equals this object.
   * All <code>JNullType</code> objects equal each other.
   *
   * @param obj the object to compare with this object
   * @return <code>true</code> if the given object equals this object, <code>false</code> otherwise
   */
  @Override
  public boolean equals(Object obj) {
      return obj instanceof JNullType;
  }

  @Override
  public String toString() {
    return toASTString("");
  }
}
