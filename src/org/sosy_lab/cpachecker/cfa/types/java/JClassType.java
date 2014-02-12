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
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.ast.java.VisibilityModifier;

import com.google.common.collect.ImmutableSet;

public class JClassType extends JClassOrInterfaceType implements JReferenceType {

  private static final String NAME_OF_CLASS_OBJECT = "java.lang.Object";
  private static final String SIMPLE_NAME_OF_CLASS_OBJECT = "Object";

  private static final JClassType typeOfObject = new JClassType();

  private static final JClassType UNRESOLVABLE_TYPE =
      new JClassType("_unspecified_", "_unspecified_",
          VisibilityModifier.NONE, false, false, false,
          JClassType.getTypeOfObject(), new HashSet<JInterfaceType>());

  private final boolean isFinal;
  private final boolean isAbstract;
  private final boolean isStrictFp;

  private final  JClassType superClass;
  private final  Set<JInterfaceType> implementedInterfaces;
  private final  Set<JClassType> directSubClasses = new HashSet<>();

  JClassType(
      String fullyQualifiedName, String pSimpleName, final VisibilityModifier pVisibility,
      final boolean pIsFinal, final boolean pIsAbstract, final boolean pStrictFp,
      JClassType pSuperClass, Set<JInterfaceType> pImplementedInterfaces) {

    super(fullyQualifiedName, pSimpleName, pVisibility);

    checkNotNull(pImplementedInterfaces);
    checkNotNull(pSuperClass);

    checkArgument(!pIsFinal || !pIsAbstract ,
        "Classes can't be abstract and final");
    checkArgument((getVisibility() != VisibilityModifier.PRIVATE)
        || (getVisibility() != VisibilityModifier.PROTECTED),
        " Classes can't be private or protected");

    isFinal = pIsFinal;
    isAbstract = pIsAbstract;
    isStrictFp = pStrictFp;
    superClass = pSuperClass;
    implementedInterfaces = ImmutableSet.copyOf(pImplementedInterfaces);

    pSuperClass.registerSubType(this);
    notifyImplementedInterfacesOfThisClass();
    checkSuperClassConsistency();
  }

  JClassType(
      String fullyQualifiedName, String pSimpleName, final VisibilityModifier pVisibility,
      final boolean pIsFinal, final boolean pIsAbstract, final boolean pStrictFp,
      JClassType pSuperClass, Set<JInterfaceType> pImplementedInterfaces,
      JClassOrInterfaceType pEnclosingType) {

    super(fullyQualifiedName, pSimpleName, pVisibility, pEnclosingType);

    checkNotNull(pImplementedInterfaces);
    checkNotNull(pSuperClass);

    checkArgument(!pIsFinal || !pIsAbstract,
        "Classes can't be abstract and final");
    checkArgument((getVisibility() != VisibilityModifier.PRIVATE)
        || (getVisibility() != VisibilityModifier.PROTECTED),
        " Classes can't be private or protected");

    isFinal = pIsFinal;
    isAbstract = pIsAbstract;
    isStrictFp = pStrictFp;
    superClass = pSuperClass;
    implementedInterfaces = ImmutableSet.copyOf(pImplementedInterfaces);

    pSuperClass.registerSubType(this);
    notifyImplementedInterfacesOfThisClass();
    checkSuperClassConsistency();
  }

  private void checkSuperClassConsistency() {
    Set<JClassType> found = new HashSet<>();

    JClassType nextSuperClass = superClass;

    while (nextSuperClass != null) {
      found.add(nextSuperClass);
      nextSuperClass = nextSuperClass.getParentClass();
      checkArgument(!found.contains(this),
          "Class " + getName() + " may not be a super class of itself." );
    }

    checkArgument(found.contains(typeOfObject),
        "Class " + getName() + " must be a super class of Object");
  }

  private JClassType() {
    super(NAME_OF_CLASS_OBJECT, SIMPLE_NAME_OF_CLASS_OBJECT, VisibilityModifier.PUBLIC);

    superClass = null;
    implementedInterfaces = new HashSet<>();

    isFinal = false;
    isAbstract = false;
    isStrictFp = false;
  }

  public static final JClassType getTypeOfObject() {
    return typeOfObject;
  }

  public static final JClassType valueOf(
      String fullyQualifiedName, String pSimpleName,
      final VisibilityModifier pVisibility, final boolean pIsFinal,
      final boolean pIsAbstract, final boolean pStrictFp,
      JClassType pSuperClass, Set<JInterfaceType> pImplementedInterfaces) {

    return new JClassType(fullyQualifiedName, pSimpleName, pVisibility,
        pIsFinal, pIsAbstract, pStrictFp, pSuperClass, pImplementedInterfaces);
  }

  public static final JClassType valueOf(
      String fullyQualifiedName, String pSimpleName,
      final VisibilityModifier pVisibility, final boolean pIsFinal,
      final boolean pIsAbstract, final boolean pStrictFp,
      JClassType pSuperClass, Set<JInterfaceType> pImplementedInterfaces,
      JClassOrInterfaceType pEnclosingType) {

    return new JClassType(fullyQualifiedName, pSimpleName, pVisibility, pIsFinal,
        pIsAbstract, pStrictFp, pSuperClass, pImplementedInterfaces, pEnclosingType);
  }

  private final void notifyImplementedInterfacesOfThisClass() {
    for (JInterfaceType implementedInterface : implementedInterfaces) {
      implementedInterface.registerSubType(this);
    }
  }

  public final boolean isFinal() {
    return isFinal;
  }

  public final boolean isAbstract() {
    return isAbstract;
  }

  public final boolean isStrictFp() {
    return isStrictFp;
  }

  @Nullable
  /**
   * Returns the super type of this class type.
   * The Super Type of the class Object is null.
   *
   * @return the super Type of this class type.
   */
  public final JClassType getParentClass() {
    return superClass;
  }

  public final Set<JClassType> getDirectSubClasses() {
     return ImmutableSet.copyOf(directSubClasses);
  }

  public final Set<JInterfaceType> getImplementedInterfaces() {
    return implementedInterfaces;
  }

  private final void registerSubType(JClassType pChild) {
      checkArgument(!directSubClasses.contains(pChild));
      directSubClasses.add(pChild);
  }

  public final Set<JClassType> getAllSuperClasses() {

    Set<JClassType> result = new HashSet<>();

    JClassType nextSuperClass = superClass;

    while (nextSuperClass != null) {

      result.add(nextSuperClass);
      nextSuperClass = nextSuperClass.getParentClass();
    }

    return result;
  }

  public final Set<JInterfaceType> getAllImplementedInterfaces() {

    // First, get all super classes of this class,
    // then, get all Implementing Interfaces and superInterfaces

    Set<JInterfaceType> result = new HashSet<>();

    Set<JClassType> classes = getAllSuperClasses();

    classes.add(this);

    for (JClassType iClass : classes) {

      result.addAll(iClass.getImplementedInterfaces());

      for (JInterfaceType implementedInterface : iClass.getImplementedInterfaces()) {
        result.addAll(implementedInterface.getAllSuperInterfaces());
      }
    }

    return result;
  }

  public final Set<JClassOrInterfaceType> getAllSuperTypesOfClass() {

    Set<JClassOrInterfaceType> result = new HashSet<>();
    result.addAll(getAllSuperClasses());
    result.addAll(getAllImplementedInterfaces());
    return result;
  }

  public final Set<JClassType> getAllSubTypesOfClass() {

    Set<JClassType> result = new HashSet<>();

    result.addAll(directSubClasses);

    // Recursion stops, if the Set directSubClasses is empty
    for (JClassType directSubClass : directSubClasses) {
      result.addAll(directSubClass.getAllSubTypesOfClass());
    }

    return result;
  }

  public static JClassType createUnresolvableType() {
    return UNRESOLVABLE_TYPE;
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