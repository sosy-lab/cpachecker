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
package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;


public class CFieldDesignator extends CADesignator {

  private final String         name;
  private final CIADesignator owner;
  private final boolean        isPointerDereference;

  public CFieldDesignator(final FileLocation pFileLocation,
                            final String pName,
                            final CIADesignator pOwner,
                            final boolean pIsPointerDereference) {
    super(pFileLocation);
    name = pName;
    owner = pOwner;
    isPointerDereference = pIsPointerDereference;
  }


  public String getFieldName() {
    return name;
  }

  public CIADesignator getFieldOwner() {
    return owner;
  }

  public boolean isPointerDereference() {
    return isPointerDereference;
  }

  @Override
  public String toASTString() {
    String op = isPointerDereference ? "->" : ".";
    return owner.toASTString() + op  + name;
  }

  @Override
  public String toParenthesizedASTString() {
    return toASTString();
  }

  @Override
  public <R, X extends Exception> R accept(CDesignatorVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }


  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (isPointerDereference ? 1231 : 1237);
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((owner == null) ? 0 : owner.hashCode());
    return result;
  }


  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (obj == null) { return false; }
    if (!(obj instanceof CFieldDesignator)) { return false; }
    CFieldDesignator other = (CFieldDesignator) obj;
    if (isPointerDereference != other.isPointerDereference) { return false; }
    if (name == null) {
      if (other.name != null) { return false; }
    } else if (!name.equals(other.name)) { return false; }
    if (owner == null) {
      if (other.owner != null) { return false; }
    } else if (!owner.equals(other.owner)) { return false; }
    return true;
  }


}
