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
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.java.VisibilityModifier;

import com.google.common.collect.ImmutableSet;

public final class JInterfaceType extends JClassOrInterfaceType implements JReferenceType {

  private static final JInterfaceType UNRESOLVABLE_TYPE =
      new JInterfaceType("_unspecified_", "_unspecified_", VisibilityModifier.NONE,
          new HashSet<JInterfaceType>());


  private final Set<JClassType> interfaceImplementingClasses = new HashSet<>();
  private final Set<JInterfaceType> superInterfaces;
  private final Set<JInterfaceType> directSubInterfaces = new HashSet<>();

  private JInterfaceType(
      String pFullyQualifiedName, String pSimpleName,
      final VisibilityModifier pVisibility,
      Set<JInterfaceType> pExtendedInterfaces) {
    super(pFullyQualifiedName, pSimpleName, pVisibility);

    checkNotNull(pExtendedInterfaces);
    superInterfaces = ImmutableSet.copyOf(pExtendedInterfaces);

    notifySuperTypes();
    checkInterfaceConsistency();
  }

  public JInterfaceType(
      String pFullyQualifiedName, String pSimpleName,
      VisibilityModifier pVisibility, Set<JInterfaceType> pExtendedInterfaces,
      JClassOrInterfaceType pEnclosingType) {

    super(pFullyQualifiedName, pSimpleName, pVisibility, pEnclosingType);
    checkNotNull(pExtendedInterfaces);
    superInterfaces = ImmutableSet.copyOf(pExtendedInterfaces);

    notifySuperTypes();
    checkInterfaceConsistency();

  }

  private void checkInterfaceConsistency() {
    checkInterfaceConsistencyRec(this);
  }

  private void checkInterfaceConsistencyRec(JInterfaceType basisType) {

    checkArgument(!superInterfaces.contains(basisType));

    // Recursion stops, if the Set superInterfaces is empty
    for (JInterfaceType directSuperInterface : superInterfaces) {
      directSuperInterface.checkInterfaceConsistencyRec(basisType);
    }
  }

  private void notifySuperTypes() {

    for (JInterfaceType superInterface : superInterfaces) {
      // link this interface with all superInterfaces
      superInterface.registerSubType(this);
    }
  }

  public Set<JClassType> getKnownInterfaceImplementingClasses() {
      return interfaceImplementingClasses;
  }

  public Set<JInterfaceType> getSuperInterfaces() {
    return superInterfaces;
  }

  public Set<JInterfaceType> getDirectSubInterfaces() {
    return directSubInterfaces;
  }

  void registerSubType(JClassOrInterfaceType subType) {

    if (subType instanceof JInterfaceType) {

      checkArgument(!directSubInterfaces.contains(subType));
      directSubInterfaces.add((JInterfaceType) subType);
    } else {

      checkArgument(!interfaceImplementingClasses.contains(subType));
      interfaceImplementingClasses.add((JClassType) subType);
    }
  }

  public Set<JInterfaceType> getAllSubInterfacesOfInterface() {

    Set<JInterfaceType> result = new HashSet<>();

    result.addAll(directSubInterfaces);

    // Recursion stops, if the Set directSubClasses is empty
    for (JInterfaceType directSubInterface : directSubInterfaces) {
      result.addAll(directSubInterface.getAllSubInterfacesOfInterface());
    }

    return result;
  }

  public Set<JInterfaceType> getAllSuperInterfaces() {

    Set<JInterfaceType> result = new HashSet<>();

    result.addAll(superInterfaces);

    // Recursion stops, if the Set superInterfaces is empty
    for (JInterfaceType directSuperInterface : superInterfaces) {
      result.addAll(directSuperInterface.getAllSuperInterfaces());
    }

    return result;
  }

  public Set<JClassType>  getAllKnownImplementingClassesOfInterface() {

    // first, get all subInterfaces of this interface
    // then, get all Classes of this interface and all subInterfaces.

      Set<JClassType> result = new HashSet<>();

      Set<JInterfaceType> interfaces = getAllSubInterfacesOfInterface();

      interfaces.add(this);

      for (JInterfaceType itInterface : interfaces) {

        result.addAll(itInterface.getKnownInterfaceImplementingClasses());

        for (JClassType implementingClasses :
          itInterface.getKnownInterfaceImplementingClasses()) {
          result.addAll(implementingClasses.getAllSubTypesOfClass());
        }
      }

      return result;
  }

  public List<JClassOrInterfaceType> getAllSubTypesOfInterfaces() {

    List<JClassOrInterfaceType> result = new LinkedList<>();
    result.addAll(getAllSubInterfacesOfInterface());
    result.addAll(getAllKnownImplementingClassesOfInterface());
    return result;
  }

  public static JInterfaceType valueOf(String pFullyQualifiedName, String pSimpleName,
      final VisibilityModifier pVisibility, Set<JInterfaceType> pExtendedInterfaces) {

    return new JInterfaceType(pFullyQualifiedName, pSimpleName,
        pVisibility, pExtendedInterfaces);
  }

  public static JInterfaceType createUnresolvableType() {
    return UNRESOLVABLE_TYPE;
  }

  public static JInterfaceType valueOf(
      String pFullyQualifiedName, String pSimpleName,
      VisibilityModifier pVisibility,
      Set<JInterfaceType> pExtendedInterfaces,
      JClassOrInterfaceType pEnclosingType) {

    return new JInterfaceType(pFullyQualifiedName, pSimpleName,
        pVisibility, pExtendedInterfaces, pEnclosingType);
  }

  @Override
  public int hashCode() {
      final int prime = 31;
      int result = 7;
      result = prime * result + super.hashCode();
      return result;
  }

  @Override
  public boolean equals(Object obj) {
     return this == obj || super.equals(obj);
  }
}