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

import com.google.common.base.Strings;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.Type;

/**
 * This interface represents the core components that occur in each declaration:
 * a type and an (optional) name.
 *
 * This class is only SuperClass of all abstract Classes and their Subclasses.
 * The Interface {@link ASimpleDeclaration} contains all language specific
 * AST Nodes as well.
 */
public abstract class AbstractSimpleDeclaration extends AbstractAstNode implements ASimpleDeclaration {

  private static final long serialVersionUID = 1078153969461542233L;
  private  Type type;

  /**
   * The name of the declared item as it should be used by analyses in CPAchecker. This is a
   * (possibly unique) name that might, but does not have to match the {@link #origName}.
   *
   * We should use this name in all cases where conflicting names could cause problems in
   * CPAchecker, e.g., when decoding variables in an abstract state.
   */
  private final String name;

  /**
   * The name of the declared item as written in the source code of the analyzed task.
   *
   * <p>
   * We should use this name in all cases where data is imported or exported, e.g., for automaton
   * transition matching and counterexample export.
   */
  private final String origName;

  public AbstractSimpleDeclaration(FileLocation pFileLocation, final Type pType, final String pName, final String pOrigName) {
    super(pFileLocation);
    type = pType;
    name = pName;
    origName = pOrigName;
  }

  public AbstractSimpleDeclaration(final FileLocation pFileLocation,
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
  public String toASTString(boolean pQualified) {
    String nameAsString;
    if (pQualified) {
      nameAsString = Strings.nullToEmpty(getQualifiedName()).replace("::", "__");
    } else {
      nameAsString = Strings.nullToEmpty(getName());
    }
    return getType().toASTString(nameAsString) + ";";
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
    return 31 * Objects.hash(type, name, origName) + super.hashCode();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof AbstractSimpleDeclaration)
        || !super.equals(obj)) {
      return false;
    }

    AbstractSimpleDeclaration other = (AbstractSimpleDeclaration) obj;

    return Objects.equals(other.type, type)
            && Objects.equals(other.name, name)
            && Objects.equals(other.origName, origName);
  }

}