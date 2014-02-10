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

import static com.google.common.base.Preconditions.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.java.VisibilityModifier;

import com.google.common.collect.ImmutableSet;


public abstract class  JClassOrInterfaceType implements JReferenceType {

  private final VisibilityModifier visibility;
  private final String name;
  private final String simpleName;

  private final JClassOrInterfaceType enclosingType;
  private final Set<JClassOrInterfaceType> nestedTypes = new HashSet<>();

  protected JClassOrInterfaceType(String fullyQualifiedName, String pSimpleName,
      final VisibilityModifier pVisibility) {
    name = fullyQualifiedName;
    visibility = pVisibility;
    simpleName = pSimpleName;
    enclosingType = null;

    checkNotNull(fullyQualifiedName);
    checkNotNull(pSimpleName);
    //checkArgument(fullyQualifiedName.endsWith(pSimpleName));

    checkArgument((getVisibility() != VisibilityModifier.PRIVATE)
        || (getVisibility() != VisibilityModifier.PROTECTED),
        " Interfaces can't be private or protected");
  }

  protected JClassOrInterfaceType(String fullyQualifiedName, String pSimpleName,
      final VisibilityModifier pVisibility,
      JClassOrInterfaceType pEnclosingType) {
    name = fullyQualifiedName;
    simpleName = pSimpleName;
    visibility = pVisibility;
    enclosingType = pEnclosingType;


    checkNotNull(fullyQualifiedName);
    checkNotNull(pSimpleName);
    checkArgument(fullyQualifiedName.endsWith(pSimpleName));

    checkNotNull(pEnclosingType);
    checkArgument((getVisibility() != VisibilityModifier.PRIVATE)
        || (getVisibility() != VisibilityModifier.PROTECTED),
        " Interfaces can't be private or protected");

    enclosingType.notifyEnclosingTypeOfNestedType(enclosingType);
    checkEnclosingTypeConsistency();
  }

  private void checkEnclosingTypeConsistency() {

    checkArgument(!isTopLevel());

    Set<JClassOrInterfaceType> found = new HashSet<>();

    JClassOrInterfaceType nextEnclosingType = enclosingType;

    found.add(enclosingType);

    while (!nextEnclosingType.isTopLevel()) {
      nextEnclosingType = nextEnclosingType.getEnclosingType();
      checkArgument(!found.contains(this),
          "Class " + getName() + " may not be a nested type of itself.");
      found.add(nextEnclosingType);
    }
  }

  @Override
  public String toASTString(String pDeclarator) {
    return pDeclarator.isEmpty() ? getName() : getName() + " " + pDeclarator;
  }

  public String getName() {
    return name;
  }

  public VisibilityModifier getVisibility() {
    return visibility;
  }

  @Override
  public boolean equals(Object pObj) {

    if (this == pObj) {
      return true;
    }

    if (!(pObj instanceof JClassOrInterfaceType)) {
      return false;
    }

    JClassOrInterfaceType other = (JClassOrInterfaceType) pObj;

    return Objects.equals(name, other.name);
  }

  @Override
  public int hashCode() {
      final int prime = 31;
      int result = 7;
      result = prime * result + Objects.hashCode(name);
      return result;
  }

  @Override
  public String toString() {
    return name;
  }

  public List<JClassOrInterfaceType> getAllSuperTypesOfType() {
    List<JClassOrInterfaceType> result = new LinkedList<>();

    if (this instanceof JClassType) {
      result.addAll(((JClassType) this).getAllSuperTypesOfClass());
    } else if (this instanceof JInterfaceType) {
      result.addAll(((JInterfaceType) this).getAllSuperInterfaces());
      return result;
    }
    return result;
  }

  public List<JClassOrInterfaceType> getAllSubTypesOfType() {

    List<JClassOrInterfaceType> result = new LinkedList<>();

    if (this instanceof JClassType) {
      result.addAll(((JClassType) this).getAllSubTypesOfClass());
    } else if (this instanceof JInterfaceType) {
      result.addAll(((JInterfaceType) this).getAllSuperInterfaces());
      return result;
    }
    return result;
  }

  public JClassOrInterfaceType getEnclosingType() {
    checkNotNull(enclosingType, "Top-level-classes do not have an enclosing type.");
    return enclosingType;
  }

  public Set<JClassOrInterfaceType> getNestedTypes() {
    return ImmutableSet.copyOf(nestedTypes);
  }

  public final Set<JClassOrInterfaceType> getAllEnclosingTypes() {

    Set<JClassOrInterfaceType> result = new HashSet<>();

    JClassOrInterfaceType nextEnclosingInstance = enclosingType;

    while (!nextEnclosingInstance.isTopLevel()) {
      result.add(nextEnclosingInstance);
      nextEnclosingInstance = nextEnclosingInstance.getEnclosingType();
    }

    return result;
  }

  public boolean isTopLevel() {
    return enclosingType == null;
  }

  private void notifyEnclosingTypeOfNestedType(JClassOrInterfaceType nestedType) {
    checkArgument(!nestedTypes.contains(nestedType));
    nestedTypes.add(nestedType);
  }

  public String getSimpleName() {
    return simpleName;
  }
}