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
package org.sosy_lab.cpachecker.cfa.ast.java;

import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JConstructorType;
import org.sosy_lab.cpachecker.cfa.types.java.JMethodType;

import com.google.common.base.Strings;

/**
 *
 * This class and its subclasses represents all methods and constructor declaration.
 *
 * e.g.
 *
 * a.type(x,y);
 * super(a,b);
 * Type v = new Type(a, b);
 *
 */
public  class JMethodDeclaration extends AFunctionDeclaration implements JDeclaration {

  private final boolean isFinal;
  private final boolean isAbstract;
  private final boolean isStatic;
  private final boolean isNative;
  private final boolean isSynchronized;
  private final boolean isStrictfp;
  private final VisibilityModifier visibility;
  private final JClassOrInterfaceType declaringClass;


  public JMethodDeclaration(FileLocation pFileLocation, JMethodType pType, String pName, VisibilityModifier pVisibility, final boolean pIsFinal,
      final boolean pIsAbstract, final boolean pIsStatic, final boolean pIsNative, final boolean pIsSynchronized, final boolean pIsStrictfp, JClassOrInterfaceType pDeclaringClass) {
    super(pFileLocation, pType, pName, pType.getParameterDeclarations());
    visibility = pVisibility;
    isFinal = pIsFinal;
    isAbstract = pIsAbstract;
    isStatic = pIsStatic;
    isNative = pIsNative;
    isSynchronized = pIsSynchronized;
    isStrictfp = pIsStrictfp;
    declaringClass = pDeclaringClass;


    assert (pVisibility != null);
    assert (isAbstract() && !isStatic() && !isNative() && !isFinal() && !isSynchronized() && !isStrictfp() || (!isAbstract()))
    : "Abstract Method may only have one Modifier , either public or protected";

  }

  @Override
  public JMethodType getType() {
    return (JMethodType) super.getType();
  }

  @Override
  public List<JParameterDeclaration> getParameters() {
    return getType().getParameterDeclarations();
  }

  @Override
  public String toASTString() {
    String name = Strings.nullToEmpty(getName());
    StringBuilder modifier = new StringBuilder() ;

    modifier.append(getVisibility().getModifierString() + " ");

    if (isAbstract()) {
      modifier.append("abstract ");
    }
    if (isStatic()) {
      modifier.append("static ");
    }
    if (isFinal()) {
      modifier.append("final ");
    }
    if (isSynchronized()) {
      modifier.append("synchronized ");
    }
    if (isNative()) {
      modifier.append("native ");
    }
    if (isStrictfp()) {
      modifier.append("strictfp ");
    }

    return modifier + getType().toASTString(name) + ";";
  }


  public boolean isFinal() {
    return isFinal;
  }


  public boolean isAbstract() {
    return isAbstract;
  }


  public boolean isStatic() {
    return isStatic;
  }


  public boolean isNative() {
    return isNative;
  }


  public boolean isSynchronized() {
    return isSynchronized;
  }


  public boolean isStrictfp() {
    return isStrictfp;
  }


  public VisibilityModifier getVisibility() {
    return visibility;
  }

  public JClassOrInterfaceType getDeclaringClass() {
    return declaringClass;
  }

  /**
   * This method updates the declaration Type, because it
   * can't be constructed from a Method Binding.
   * DO NOT CALL THIS METHOD OUTSIDE OF ASTCONVERTER.
   *
   * @param type new method Type to be assigned to this declaration.
   */
  public void updateMethodType(JMethodType type) {

    if (this instanceof JConstructorDeclaration) {
      assert type instanceof JConstructorType;
    }
    setType(type);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((declaringClass == null) ? 0 : declaringClass.hashCode());
    result = prime * result + (isAbstract ? 1231 : 1237);
    result = prime * result + (isFinal ? 1231 : 1237);
    result = prime * result + (isNative ? 1231 : 1237);
    result = prime * result + (isStatic ? 1231 : 1237);
    result = prime * result + (isStrictfp ? 1231 : 1237);
    result = prime * result + (isSynchronized ? 1231 : 1237);
    result = prime * result + ((visibility == null) ? 0 : visibility.hashCode());
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
    if (!(obj instanceof JMethodDeclaration)) { return false; }
    JMethodDeclaration other = (JMethodDeclaration) obj;
    if (declaringClass == null) {
      if (other.declaringClass != null) { return false; }
    } else if (!declaringClass.equals(other.declaringClass)) { return false; }
    if (isAbstract != other.isAbstract) { return false; }
    if (isFinal != other.isFinal) { return false; }
    if (isNative != other.isNative) { return false; }
    if (isStatic != other.isStatic) { return false; }
    if (isStrictfp != other.isStrictfp) { return false; }
    if (isSynchronized != other.isSynchronized) { return false; }
    if (visibility != other.visibility) { return false; }

    return super.equals(other);
  }

}
