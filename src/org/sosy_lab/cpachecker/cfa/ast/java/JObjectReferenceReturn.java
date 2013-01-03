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
package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;


public class JObjectReferenceReturn extends JReturnStatement {

  private final JClassType classReference;

  public JObjectReferenceReturn(FileLocation pFileLocation, JClassType pClassReference ) {
    super(pFileLocation, new JThisExpression(pFileLocation, pClassReference));
    classReference = pClassReference;
  }

  @Override
  public JThisExpression getReturnValue() {
    return (JThisExpression) super.getReturnValue();
  }

  public JClassType getReturnClassType(){
    return classReference;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((classReference == null) ? 0 : classReference.hashCode());
    result = prime * result + super.hashCode();
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (!super.equals(obj)) { return false; }
    if (!(obj instanceof JObjectReferenceReturn)) { return false; }
    JObjectReferenceReturn other = (JObjectReferenceReturn) obj;
    if (classReference == null) {
      if (other.classReference != null) { return false; }
    } else if (!classReference.equals(other.classReference)) { return false; }

    return super.equals(other);
  }

}