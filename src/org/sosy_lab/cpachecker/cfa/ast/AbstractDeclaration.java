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
package org.sosy_lab.cpachecker.cfa.ast;


import org.sosy_lab.cpachecker.cfa.types.Type;

/**
 * This interface represents all sorts of top-level declarations (i.e., declarations
 * not nested inside another type declaration).
 * This excludes for examples function parameter declarations and struct members.
 * It includes local and global variables and types, as well as functions.
 * This class is only SuperClass of all abstract Classes and their Subclasses.
 * The Interface {@link ADeclaration} contains all language specific
 * AST Nodes as well.
 */
public abstract class AbstractDeclaration extends AbstractSimpleDeclaration implements ADeclaration {

  private final boolean isGlobal;

  public AbstractDeclaration(FileLocation pFileLocation,  boolean pIsGlobal, Type pType, String pName) {
    super(pFileLocation, pType, pName, pName);
    isGlobal = pIsGlobal;
  }

  public AbstractDeclaration(FileLocation pFileLocation,  boolean pIsGlobal, Type pType, String pName, String pOrigName) {
    super(pFileLocation, pType, pName, pOrigName);
    isGlobal = pIsGlobal;
  }

  @Override
  public boolean isGlobal() {
    return isGlobal;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + (isGlobal ? 1231 : 1237);
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

    if (!(obj instanceof AbstractDeclaration)
        || !super.equals(obj)) {
      return false;
    }

    AbstractDeclaration other = (AbstractDeclaration) obj;

    return other.isGlobal == isGlobal;
  }

}