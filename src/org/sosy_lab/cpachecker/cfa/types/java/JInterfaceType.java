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

import java.util.*;

import org.sosy_lab.cpachecker.cfa.ast.java.VisibilityModifier;

import com.google.common.collect.ImmutableSet;

/**
 * Description of a Java interface.
 *
 * A Java interface is described by its name, visibility and the interfaces
 * it extends.
 */
public final class JInterfaceType extends JClassOrInterfaceType implements JReferenceType {

  private static final long serialVersionUID = 1985477760453866693L;

  private static final JInterfaceType UNRESOLVABLE_TYPE =
      new JInterfaceType(
          "_unspecified_", "_unspecified_", VisibilityModifier.NONE, new HashSet<>());


  private final Set<JClassType> interfaceImplementingClasses = new HashSet<>();
  private final Set<JInterfaceType> superInterfaces;
  private final Set<JInterfaceType> directSubInterfaces = new HashSet<>();

  // Create a JInterface object without an enclosing type
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

  /**
   * Creates a new <code>JInterfaceType</code> object describing an interface with the given
   * attributes.
   *
   * @param pFullyQualifiedName the fully qualified name of the interface
   * @param pSimpleName the simple name of the interface.
   *        This is only the interface name without its package name
   * @param pVisibility the visibility of the describes interface, represented by a
   *        {@link VisibilityModifier} object
   * @param pExtendedInterfaces the <code>JInterfaceType</code>s of the interfaces the described
   *        interface extends
   * @param pEnclosingType the direct enclosing type of this interface
   */
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

  /*
   *  Checks that no interface that is extended by this interface extends this interface.
   *
   *  In a mathematical sense, this method checks that the extends-relation between this
   *  interface and all its extended interfaces is asymmetrical.
   */
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

  /**
   * Returns a <code>Set</code> of {@link JClassType} objects that describe the classes
   * that are known to implement this interface directly.
   *
   * This method only returns the classes that extend this interface directly by specifying it
   * in its class declaration.
   *
   * @return the <code>JClassType</code> objects that describe the classes that are known to
   *         implement this interface directly
   */
  public Set<JClassType> getKnownInterfaceImplementingClasses() {
      return interfaceImplementingClasses;
  }

  /**
   * Returns a <code>Set</code> of {@link JInterfaceType} objects that describe the interfaces
   * this interface extends directly.
   *
   * @return a <code>Set</code> of {@link JInterfaceType} objects that describe the interfaces
   *         this interface extends directly
   */
  public Set<JInterfaceType> getSuperInterfaces() {
    return superInterfaces;
  }

  /**
   * Returns a <code>Set</code> of {@link JInterfaceType} objects that describe the interfaces
   * that directly extend this interface.
   *
   * @return a <code>Set</code> of {@link JInterfaceType} objects that describe the interfaces
   * that directly extend this interface.
   */
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

  /**
   * Returns a <code>Set</code> of {@link JInterfaceType} objects that describe the interfaces
   * that extend this interface.
   *
   * Returns not only <code>JInterfaceType</code>s for the interfaces that directly
   * extend this interface, but all.
   *
   * @return a <code>Set</code> of {@link JInterfaceType} objects that describe all the interfaces
   * that extend this interface directly or indirectly
   */
  public Set<JInterfaceType> getAllSubInterfacesOfInterface() {

    Set<JInterfaceType> result = new HashSet<>();

    result.addAll(directSubInterfaces);

    // Recursion stops, if the Set directSubClasses is empty
    for (JInterfaceType directSubInterface : directSubInterfaces) {
      result.addAll(directSubInterface.getAllSubInterfacesOfInterface());
    }

    return result;
  }

  /**
   * Returns a <code>Set</code> of {@link JInterfaceType} objects that describe all the interfaces
   * this interface extends.
   *
   * Returns not only interfaces that are directly extended, but also ones that are indirectly.
   *
   * @return a <code>Set</code> of {@link JInterfaceType} objects that describe all the interfaces
   * this interface extends
   */
  public Set<JInterfaceType> getAllSuperInterfaces() {

    Set<JInterfaceType> result = new HashSet<>();

    result.addAll(superInterfaces);

    // Recursion stops, if the Set superInterfaces is empty
    for (JInterfaceType directSuperInterface : superInterfaces) {
      result.addAll(directSuperInterface.getAllSuperInterfaces());
    }

    return result;
  }

  /**
   * Returns a <code>Set</code> of {@link JClassType} objects that describe all the classes that
   * are known to implement this interface directly or indirectly.
   *
   * Returns not only types for the classes that directly implement this interface, but also for
   * all classes that implement this interface by implementing a sub interface of this interface.
   *
   * @return a <code>Set</code> of {@link JClassType} objects that describe all the classes that
   * are known to implement this interface directly or indirectly.
   */
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

  /**
   * Returns a <code>List</code> of all sub types of this interface.
   *
   * This includes all sub interfaces and implementing classes of this interface,
   * direct and indirect ones.
   *
   * The returned <code>List</code> contains first all sub interface of this
   * interface in random order, followed by all known implementing classes
   * in random order.
   *
   * @return a <code>List</code> of all sub types of this interface
   */
  public List<JClassOrInterfaceType> getAllSubTypesOfInterfaces() {

    List<JClassOrInterfaceType> result = new LinkedList<>();
    result.addAll(getAllSubInterfacesOfInterface());
    result.addAll(getAllKnownImplementingClassesOfInterface());
    return result;
  }

  /**
   * Returns a <code>JInterfaceType</code> instance describing an interface with the given
   * attributes.
   *
   * @param pFullyQualifiedName the fully qualified name of the interface
   * @param pSimpleName the simple name of the interface.
   *        This is only the interface name without its package name
   * @param pVisibility the visibility of the describes interface, represented by a
   *        {@link VisibilityModifier} object
   * @param pExtendedInterfaces the <code>JInterfaceType</code>s of the interfaces the described
   *        interface extends
   *
   * @return a <code>JInterfaceType</code> object describing an interface with the given attributes
   */
  public static JInterfaceType valueOf(String pFullyQualifiedName, String pSimpleName,
      final VisibilityModifier pVisibility, Set<JInterfaceType> pExtendedInterfaces) {

    return new JInterfaceType(pFullyQualifiedName, pSimpleName,
        pVisibility, pExtendedInterfaces);
  }

  /**
   * Returns a <code>JInterfaceType</code> instance describing an unresolvable interface.
   *
   * @return a <code>JInterfaceType</code> instance describing an unresolvable interface
   */
  public static JInterfaceType createUnresolvableType() {
    return UNRESOLVABLE_TYPE;
  }

  /**
   * Returns a <code>JInterfaceType</code> instance describing an interface with the given
   * attributes.
   *
   * @param pFullyQualifiedName the fully qualified name of the interface
   * @param pSimpleName the simple name of the interface.
   *        This is only the interface name without its package name
   * @param pVisibility the visibility of the describes interface, represented by a
   *        {@link VisibilityModifier} object
   * @param pExtendedInterfaces the <code>JInterfaceType</code>s of the interfaces the described
   *        interface extends
   * @param pEnclosingType the direct enclosing type of this interface
   *
   * @return a <code>JInterfaceType</code> object describing an interface with the given attributes
   */
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