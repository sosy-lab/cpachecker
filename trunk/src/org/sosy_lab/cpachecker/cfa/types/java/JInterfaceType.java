// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.java;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.java.VisibilityModifier;

/**
 * Description of a Java interface.
 *
 * <p>A Java interface is described by its name, visibility and the interfaces it extends.
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
      String pFullyQualifiedName,
      String pSimpleName,
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
   * @param pSimpleName the simple name of the interface. This is only the interface name without
   *     its package name
   * @param pVisibility the visibility of the describes interface, represented by a {@link
   *     VisibilityModifier} object
   * @param pExtendedInterfaces the <code>JInterfaceType</code>s of the interfaces the described
   *     interface extends
   * @param pEnclosingType the direct enclosing type of this interface
   */
  public JInterfaceType(
      String pFullyQualifiedName,
      String pSimpleName,
      VisibilityModifier pVisibility,
      Set<JInterfaceType> pExtendedInterfaces,
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
   * Returns a <code>Set</code> of {@link JClassType} objects that describe the classes that are
   * known to implement this interface directly.
   *
   * <p>This method only returns the classes that extend this interface directly by specifying it in
   * its class declaration.
   *
   * @return the <code>JClassType</code> objects that describe the classes that are known to
   *     implement this interface directly
   */
  public Set<JClassType> getKnownInterfaceImplementingClasses() {
    return interfaceImplementingClasses;
  }

  /**
   * Returns a <code>Set</code> of {@link JInterfaceType} objects that describe the interfaces this
   * interface extends directly.
   *
   * @return a <code>Set</code> of {@link JInterfaceType} objects that describe the interfaces this
   *     interface extends directly
   */
  public Set<JInterfaceType> getSuperInterfaces() {
    return superInterfaces;
  }

  /**
   * Returns a <code>Set</code> of {@link JInterfaceType} objects that describe the interfaces that
   * directly extend this interface.
   *
   * @return a <code>Set</code> of {@link JInterfaceType} objects that describe the interfaces that
   *     directly extend this interface.
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
   * Returns a <code>Set</code> of {@link JInterfaceType} objects that describe the interfaces that
   * extend this interface.
   *
   * <p>Returns not only <code>JInterfaceType</code>s for the interfaces that directly extend this
   * interface, but all.
   *
   * @return a <code>Set</code> of {@link JInterfaceType} objects that describe all the interfaces
   *     that extend this interface directly or indirectly
   */
  public Set<JInterfaceType> getAllSubInterfacesOfInterface() {

    Set<JInterfaceType> result = new HashSet<>(directSubInterfaces);

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
   * <p>Returns not only interfaces that are directly extended, but also ones that are indirectly.
   *
   * @return a <code>Set</code> of {@link JInterfaceType} objects that describe all the interfaces
   *     this interface extends
   */
  public Set<JInterfaceType> getAllSuperInterfaces() {

    Set<JInterfaceType> result = new HashSet<>(superInterfaces);

    // Recursion stops, if the Set superInterfaces is empty
    for (JInterfaceType directSuperInterface : superInterfaces) {
      result.addAll(directSuperInterface.getAllSuperInterfaces());
    }

    return result;
  }

  /**
   * Returns a <code>Set</code> of {@link JClassType} objects that describe all the classes that are
   * known to implement this interface directly or indirectly.
   *
   * <p>Returns not only types for the classes that directly implement this interface, but also for
   * all classes that implement this interface by implementing a sub interface of this interface.
   *
   * @return a <code>Set</code> of {@link JClassType} objects that describe all the classes that are
   *     known to implement this interface directly or indirectly.
   */
  public Set<JClassType> getAllKnownImplementingClassesOfInterface() {

    // first, get all subInterfaces of this interface
    // then, get all Classes of this interface and all subInterfaces.

    Set<JClassType> result = new HashSet<>();

    Set<JInterfaceType> interfaces = getAllSubInterfacesOfInterface();

    interfaces.add(this);

    for (JInterfaceType itInterface : interfaces) {

      result.addAll(itInterface.getKnownInterfaceImplementingClasses());

      for (JClassType implementingClasses : itInterface.getKnownInterfaceImplementingClasses()) {
        result.addAll(implementingClasses.getAllSubTypesOfClass());
      }
    }

    return result;
  }

  /**
   * Returns a <code>List</code> of all sub types of this interface.
   *
   * <p>This includes all sub interfaces and implementing classes of this interface, direct and
   * indirect ones.
   *
   * <p>The returned <code>List</code> contains first all sub interface of this interface in random
   * order, followed by all known implementing classes in random order.
   *
   * @return a <code>List</code> of all sub types of this interface
   */
  public List<JClassOrInterfaceType> getAllSubTypesOfInterfaces() {
    return ImmutableList.<JClassOrInterfaceType>builder()
        .addAll(getAllSubInterfacesOfInterface())
        .addAll(getAllKnownImplementingClassesOfInterface())
        .build();
  }

  /**
   * Returns a <code>JInterfaceType</code> instance describing an interface with the given
   * attributes.
   *
   * @param pFullyQualifiedName the fully qualified name of the interface
   * @param pSimpleName the simple name of the interface. This is only the interface name without
   *     its package name
   * @param pVisibility the visibility of the describes interface, represented by a {@link
   *     VisibilityModifier} object
   * @param pExtendedInterfaces the <code>JInterfaceType</code>s of the interfaces the described
   *     interface extends
   * @return a <code>JInterfaceType</code> object describing an interface with the given attributes
   */
  public static JInterfaceType valueOf(
      String pFullyQualifiedName,
      String pSimpleName,
      final VisibilityModifier pVisibility,
      Set<JInterfaceType> pExtendedInterfaces) {

    return new JInterfaceType(pFullyQualifiedName, pSimpleName, pVisibility, pExtendedInterfaces);
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
   * @param pSimpleName the simple name of the interface. This is only the interface name without
   *     its package name
   * @param pVisibility the visibility of the describes interface, represented by a {@link
   *     VisibilityModifier} object
   * @param pExtendedInterfaces the <code>JInterfaceType</code>s of the interfaces the described
   *     interface extends
   * @param pEnclosingType the direct enclosing type of this interface
   * @return a <code>JInterfaceType</code> object describing an interface with the given attributes
   */
  public static JInterfaceType valueOf(
      String pFullyQualifiedName,
      String pSimpleName,
      VisibilityModifier pVisibility,
      Set<JInterfaceType> pExtendedInterfaces,
      JClassOrInterfaceType pEnclosingType) {

    return new JInterfaceType(
        pFullyQualifiedName, pSimpleName, pVisibility, pExtendedInterfaces, pEnclosingType);
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
