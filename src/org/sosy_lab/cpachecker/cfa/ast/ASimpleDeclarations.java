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
import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.types.Type;

import com.google.common.base.Strings;


/**
 * This interface represents the core components that occur in each declaration:
 * a type and an (optional) name.
 *
 * This class is only SuperClass of all abstract Classes and their Subclasses.
 * The Interface {@link IASimpleDeclarations} contains all language specific
 * AST Nodes as well.
 */
public abstract class ASimpleDeclarations extends AstNode implements IASimpleDeclaration {

  private  Type type;
  private final String name;
  private final String origName;

  public ASimpleDeclarations(FileLocation pFileLocation, final Type pType, final String pName, final String pOrigName) {
    super(pFileLocation);
    type = pType;
    name = pName;
    origName = pOrigName;
  }

  public ASimpleDeclarations(final FileLocation pFileLocation,
      final Type pType, final String pName) {
    this(pFileLocation, pType, pName, pName);
  }

  @Override
  public String getName() {
    return name;
  }
  @Override
  public String getOrigName() {
    return origName;
  }

  @Override
  public String toASTString() {
    String name = Strings.nullToEmpty(getName());
    return getType().toASTString(name) + ";";
  }

  @Override
  public Type getType() {
    return type;
  }

  protected void setType(Type pType) {
    type = pType;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(type);
    result = prime * result + Objects.hashCode(name);
    result = prime * result + Objects.hashCode(origName);
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

    if (!(obj instanceof ASimpleDeclarations)
        || !super.equals(obj)) {
      return false;
    }

    ASimpleDeclarations other = (ASimpleDeclarations) obj;

    return Objects.equals(other.type, type)
            && Objects.equals(other.name, name)
            && Objects.equals(other.origName, origName);
  }

}