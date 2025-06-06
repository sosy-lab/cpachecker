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

import com.google.common.collect.ImmutableSet;
import java.io.Serial;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.java.VisibilityModifier;

/**
 * Description of a Java class through its properties.
 *
 * <p>Each description includes:
 *
 * <ul>
 *   <li>the class's name
 *   <li>its visibility and other modifiers
 *   <li>its direct super class
 *   <li>the interfaces it implements and
 *   <li>its enclosing type, if one exists
 * </ul>
 */
public final class JClassType extends JClassOrInterfaceType {

  @Serial private static final long serialVersionUID = 2051770436134716617L;

  private static final String NAME_OF_CLASS_OBJECT = "java.lang.Object";
  private static final String SIMPLE_NAME_OF_CLASS_OBJECT = "Object";

  private final boolean isFinal;
  private final boolean isAbstract;
  private final boolean isStrictFp;

  private final @Nullable JClassType superClass;
  private final Set<JInterfaceType> implementedInterfaces;
  private final Set<JClassType> directSubClasses = new HashSet<>();

  JClassType(
      String fullyQualifiedName,
      String pSimpleName,
      final VisibilityModifier pVisibility,
      final boolean pIsFinal,
      final boolean pIsAbstract,
      final boolean pStrictFp,
      JClassType pSuperClass,
      Set<JInterfaceType> pImplementedInterfaces) {

    super(fullyQualifiedName, pSimpleName, pVisibility);

    checkNotNull(pImplementedInterfaces);
    checkNotNull(pSuperClass);

    checkArgument(!pIsFinal || !pIsAbstract, "Classes can't be abstract and final");
    checkArgument(
        (getVisibility() != VisibilityModifier.PRIVATE)
            && (getVisibility() != VisibilityModifier.PROTECTED),
        "Classes that are not inner classes can't be private or protected");

    isFinal = pIsFinal;
    isAbstract = pIsAbstract;
    isStrictFp = pStrictFp;
    superClass = pSuperClass;
    implementedInterfaces = ImmutableSet.copyOf(pImplementedInterfaces);

    pSuperClass.registerSubType(this);
    notifyImplementedInterfacesOfThisClass();
  }

  JClassType(
      String fullyQualifiedName,
      String pSimpleName,
      final VisibilityModifier pVisibility,
      final boolean pIsFinal,
      final boolean pIsAbstract,
      final boolean pStrictFp,
      JClassType pSuperClass,
      Set<JInterfaceType> pImplementedInterfaces,
      JClassOrInterfaceType pEnclosingType) {

    super(fullyQualifiedName, pSimpleName, pVisibility, pEnclosingType);

    checkNotNull(pImplementedInterfaces);
    checkNotNull(pSuperClass);

    checkArgument(!pIsFinal || !pIsAbstract, "Classes can't be abstract and final");

    isFinal = pIsFinal;
    isAbstract = pIsAbstract;
    isStrictFp = pStrictFp;
    superClass = pSuperClass;
    implementedInterfaces = ImmutableSet.copyOf(pImplementedInterfaces);

    pSuperClass.registerSubType(this);
    notifyImplementedInterfacesOfThisClass();
  }

  // Creates the object describing java.lang.Object
  private JClassType() {
    super(NAME_OF_CLASS_OBJECT, SIMPLE_NAME_OF_CLASS_OBJECT, VisibilityModifier.PUBLIC);

    superClass = null;
    implementedInterfaces = new HashSet<>();

    isFinal = false;
    isAbstract = false;
    isStrictFp = false;
  }

  /**
   * Returns a new <code>JClassType</code> instance that describes the class <code>java.lang.Object
   * </code>. The returned instance will be a fresh Object that is not associated with any
   * subclasses (yet). To get access to the class Object that was initialized with the current
   * verification run's type hierarchy, use TypeHierarchy#getClassTypeOfObject()
   *
   * @return a <code>JClassType</code> instance that describes the class <code>java.lang.Object
   *     </code>
   */
  public static JClassType createObjectType() {
    return new JClassType();
  }

  /**
   * Returns an instance of <code>JClassType</code> that describes a class with the given
   * properties.
   *
   * <p>The fully qualified name, the simple name, possible modifiers, the super class and
   * implemented interfaces of the class to describe have to be provided.
   *
   * <ul>
   *   <li>The fully qualified name consists of the full package name of the class and the name of
   *       the class itself.<br>
   *       Example: <code>java.lang.Object</code>
   *   <li>The simple name only consists of the class's name.<br>
   *       Example: <code>Object</code>
   *   <li>Each class has a super class. If a class does not explicitly extend another class, its
   *       super class is <code>java.lang.Object</code>.
   * </ul>
   *
   * <p>No <code>null</code> values may be provided. In case the class doesn't implement any
   * interfaces, an empty <code>Set</code> has to be provided.
   *
   * @param fullyQualifiedName the fully qualified name of the class to describe, see above
   * @param pSimpleName the simple name of the class to describe, see above
   * @param pVisibility the visibility of the class
   * @param pIsFinal if <code>true</code> the class is final, it's not otherwise
   * @param pIsAbstract if <code>true</code> the class is abstract, it's not otherwise
   * @param pStrictFp if <code>true</code> the class has a strict function pointer, it does not
   *     otherwise
   * @param pSuperClass the <code>JClassType</code> object describing the super class of the class
   *     to describe
   * @param pImplementedInterfaces the <code>JInterfaceType</code> objects describing the interfaces
   *     directly implemented by the class to describe
   * @return an instance of <code>JClassType</code> that describes a class with the given properties
   */
  // TODO check the concrete meaning of pStrictFp
  public static JClassType valueOf(
      String fullyQualifiedName,
      String pSimpleName,
      final VisibilityModifier pVisibility,
      final boolean pIsFinal,
      final boolean pIsAbstract,
      final boolean pStrictFp,
      JClassType pSuperClass,
      Set<JInterfaceType> pImplementedInterfaces) {

    return new JClassType(
        fullyQualifiedName,
        pSimpleName,
        pVisibility,
        pIsFinal,
        pIsAbstract,
        pStrictFp,
        pSuperClass,
        pImplementedInterfaces);
  }

  /**
   * Returns an instance of <code>JClassType</code> that describes a class with the given
   * properties.
   *
   * <p>The fully qualified name, the simple name, possible modifiers, the super class and
   * implemented interfaces of the class to describe have to be provided.
   *
   * <ul>
   *   <li>The fully qualified name consists of the full package name of the class and the name of
   *       the class itself.<br>
   *       Example: <code>java.lang.Object</code>
   *   <li>The simple name only consists of the class's name.<br>
   *       Example: <code>Object</code>
   *   <li>Each class has a super class. If a class does not explicitly extend another class, its
   *       super class is <code>java.lang.Object</code>.
   * </ul>
   *
   * <p>No <code>null</code> values may be provided. In case the class doesn't implement any
   * interfaces, an empty <code>Set</code> has to be provided.
   *
   * @param fullyQualifiedName the fully qualified name of the class to describe, see above
   * @param pSimpleName the simple name of the class to describe, see above
   * @param pVisibility the visibility of the class
   * @param pIsFinal if <code>true</code> the class is final, it's not otherwise
   * @param pIsAbstract if <code>true</code> the class is abstract, it's not otherwise
   * @param pStrictFp if <code>true</code> the class has a strict function pointer, it does not
   *     otherwise
   * @param pSuperClass the <code>JClassType</code> object describing the super class of the class
   *     to describe
   * @param pImplementedInterfaces the <code>JInterfaceType</code> objects describing the interfaces
   *     directly implemented by the class to describe
   * @param pEnclosingType the description of the enclosing type of this class
   * @return an instance of <code>JClassType</code> that describes a class with the given properties
   */
  public static JClassType valueOf(
      String fullyQualifiedName,
      String pSimpleName,
      final VisibilityModifier pVisibility,
      final boolean pIsFinal,
      final boolean pIsAbstract,
      final boolean pStrictFp,
      JClassType pSuperClass,
      Set<JInterfaceType> pImplementedInterfaces,
      JClassOrInterfaceType pEnclosingType) {

    return new JClassType(
        fullyQualifiedName,
        pSimpleName,
        pVisibility,
        pIsFinal,
        pIsAbstract,
        pStrictFp,
        pSuperClass,
        pImplementedInterfaces,
        pEnclosingType);
  }

  private void notifyImplementedInterfacesOfThisClass() {
    for (JInterfaceType implementedInterface : implementedInterfaces) {
      implementedInterface.registerSubType(this);
    }
  }

  /**
   * Returns whether the class is final.
   *
   * @return <code>true</code> if the class is final, <code>false</code> otherwise
   */
  public boolean isFinal() {
    return isFinal;
  }

  /**
   * Returns whether the class is abstract.
   *
   * @return <code>true</code> if the class is abstract, <code>false</code> otherwise
   */
  public boolean isAbstract() {
    return isAbstract;
  }

  /**
   * Returns whether the class has a strict function pointer.
   *
   * @return <code>true</code> if the class has a strict function pointer, <code>false</code>
   *     otherwise
   */
  public boolean isStrictFp() {
    return isStrictFp;
  }

  /**
   * Returns the super type of this class type. The Super Type of the class Object is <code>null
   * </code>.
   *
   * @return the super type of this class type
   */
  public @Nullable JClassType getParentClass() {
    return superClass;
  }

  /**
   * Returns a <code>Set</code> containing a <code>JClassType</code> for each subclass that directly
   * extends the described class.
   *
   * @return a <code>Set</code> containing a <code>JClassType</code> for each subclass that directly
   *     extends the described class
   */
  public Set<JClassType> getDirectSubClasses() {
    return ImmutableSet.copyOf(directSubClasses);
  }

  /**
   * Returns a <code>Set</code> containing a {@link JInterfaceType} for each interface directly
   * implemented by the described class.
   *
   * <p>The returned <code>Set</code> does not contain descriptions for interfaces that are extended
   * by implemented interfaces, but solely descriptions for the implemented interfaces.
   *
   * @return a <code>Set</code> containing a <code>JInterfaceType</code> for each interface directly
   *     implemented by the described class
   */
  public Set<JInterfaceType> getImplementedInterfaces() {
    return implementedInterfaces;
  }

  private void registerSubType(JClassType pChild) {
    checkArgument(!directSubClasses.contains(pChild));
    directSubClasses.add(pChild);
  }

  /**
   * Returns a <code>Set</code> containing a <code>JClassType</code> for each super class of the
   * described class.
   *
   * <p>This includes direct and indirect super classes.
   *
   * <p>For <code>JClassType</code> objects describing a consistent class, this method at least
   * returns a <code>Set</code> containing the description for <code>java.lang.Object</code>.
   *
   * @return a <code>Set</code> containing a <code>JClassType</code> for each super class of the
   *     described class
   */
  public Set<JClassType> getAllSuperClasses() {

    Set<JClassType> result = new HashSet<>();

    JClassType nextSuperClass = superClass;

    while (nextSuperClass != null) {

      result.add(nextSuperClass);
      nextSuperClass = nextSuperClass.getParentClass();
    }

    return result;
  }

  /**
   * Returns a <code>Set</code> containing a {@link JInterfaceType} for each interface implemented
   * by the described class.
   *
   * <p>This includes all implemented interfaces, directly and indirectly.
   *
   * @return a <code>Set</code> containing a <code>JInterfaceType</code> for each interface
   *     implemented by the described class
   */
  public Set<JInterfaceType> getAllImplementedInterfaces() {

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

  /**
   * Returns a <code>Set</code> containing a {@link JClassOrInterfaceType} for each super type
   * (interface or class) of the described class.
   *
   * <p>This includes direct and indirect super types.
   *
   * @return a <code>Set</code> containing a <code>JClassOrInterfaceType</code> for each super type
   *     of the described class
   */
  public Set<JClassOrInterfaceType> getAllSuperTypesOfClass() {

    Set<JClassOrInterfaceType> result = new HashSet<>();
    result.addAll(getAllSuperClasses());
    result.addAll(getAllImplementedInterfaces());
    return result;
  }

  /**
   * Returns a <code>Set</code> containing a <code>JClassType</code> for each subclass that extends
   * the described class.
   *
   * <p>This includes direct and indirect subclasses.
   *
   * @return a <code>Set</code> containing a <code>JClassType</code> for each subclass that extends
   *     the described class
   */
  public Set<JClassType> getAllSubTypesOfClass() {

    Set<JClassType> result = new HashSet<>(directSubClasses);

    // Recursion stops, if the Set directSubClasses is empty
    for (JClassType directSubClass : directSubClasses) {
      result.addAll(directSubClass.getAllSubTypesOfClass());
    }

    return result;
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
