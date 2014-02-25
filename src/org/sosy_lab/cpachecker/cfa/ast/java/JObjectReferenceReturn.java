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
package org.sosy_lab.cpachecker.cfa.ast.java;

import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;

/**
 * This class makes the return of an object reference to the caller of
 * an constructor explicit. Semantically, it is the equivalent of return this;
 * It may however only occur at the end of an constructor in the cfa.
 *
 * The returnClassType only provides the compile time type, i. e. the class,
 * which declared the constructor. This may not always be the case,
 * i.e. super constructor invocation.
 *
 *
 */
public class JObjectReferenceReturn extends JReturnStatement {

  private final JClassType classReference;

  public JObjectReferenceReturn(FileLocation pFileLocation, JClassType pClassReference) {
    super(pFileLocation, new JThisExpression(pFileLocation, pClassReference));
    classReference = pClassReference;
  }

  @Override
  public JThisExpression getReturnValue() {
    return (JThisExpression) super.getReturnValue();
  }

  public JClassType getReturnClassType() {
    return classReference;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(classReference);
    result = prime * result + super.hashCode();
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JObjectReferenceReturn)
        || !super.equals(obj)) {
      return false;
    }

    JObjectReferenceReturn other = (JObjectReferenceReturn) obj;

    return Objects.equals(other.classReference, classReference);
  }

}