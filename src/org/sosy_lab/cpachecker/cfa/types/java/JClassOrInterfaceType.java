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

/**
 * Description of a Java class or interface.
 *
 * <p>
 * Each <code>JClassOrInterfaceType</code> instance includes:
 *
 * <ul>
 *  <li>the name of the class or interface (both fully qualified and simple)</li>
 *  <li> the visibility of the described class or interface and</li>
 *  <li>the enclosing type of the described class or interface, if one exists</li>
 * </ul>
 * </p>
 */
public abstract class JClassOrInterfaceType implements JReferenceType {

  private final VisibilityModifier visibility;
  private final String name;
  private final String simpleName;

  private final JClassOrInterfaceType enclosingType;
  private final Set<JClassOrInterfaceType> nestedTypes = new HashSet<>();

  /**
   * Creates a new <code>JClassOrInterfaceType</code> object with the given
   * properties.
   *
   * <p>The fully qualified name includes the full package name and the simple name of the class
   * or interface. <br />
   * Example: <code>java.lang.Object</code>, with <code>Object</code> as the simple name.
   * </p>
   *
   * @param fullyQualifiedName the fully qualified name of the class or interface
   * @param pSimpleName the simple name of the class or interface
   * @param pVisibility the visibility of the described class or interface
   */
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

  /**
   * Creates a new <code>JClassOrInterfaceType</code> object with the given
   * properties.
   *
   * <p>The fully qualified name includes the full package name and the simple name of the class
   * or interface. <br />
   * Example: <code>java.lang.Object</code>, with <code>Object</code> as the simple name.
   * </p>
   *
   * @param fullyQualifiedName the fully qualified name of the class or interface
   * @param pSimpleName the simple name of the class or interface
   * @param pVisibility the visibility of the described class or interface
   * @param pEnclosingType the enclosing type of the class or interface
   */
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

    enclosingType.notifyEnclosingTypeOfNestedType(this);
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

  /**
   * Returns the fully qualified name of the described Java type.
   *
   * @return the fully qualified name of the described Java type
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the visibility of the described Java type.
   *
   * @return the visibility of the described Java type
   */
  public VisibilityModifier getVisibility() {
    return visibility;
  }

  /**
   * Returns whether the given object equals this <code>JClassOrInterfaceType</code>.
   *
   * <p>
   * Two <code>JClassOrInterfaceType</code>s equal only if their fully qualified names equal.
   * A <code>JClassOrInterfaceType</code> instance does never equal an object that is not of this
   * type.
   * </p>
   *
   * @return <code>true</code> if the given object equals this object, <code>false</code> otherwise
   */
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

  /**
   * Returns a <code>List</code> containing a <code>JClassOrInterfaceType</code>
   * for each super type (interface or class) of the described class in random order.
   *
   * <p>This includes direct and indirect super types.</p>
   *
   * @return a <code>Set</code> containing a <code>JClassOrInterfaceType</code>
   * for each super type of the described class
   */
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

  /**
   * Returns a <code>Set</code> containing a <code>JClassOrInterfaceType</code> for each sub type
   * that extends the described class or interface.
   *
   * <p>This includes direct and indirect sub types.</p>
   *
   * @return a <code>Set</code> containing a <code>JClassOrInterfaceType</code> for each sub type
   * that extends the described class
   */
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

  /**
   * Returns the directly enclosing type, if one exists.
   *
   * If the class does not have an enclosing type, a <code>NullPointerException</code> is thrown.
   * To check this, {@link #isTopLevel()} can be used.
   *
   * @return the enclosing type of the described class or interface, if one exists
   * @throws NullPointerException if no enclosing type exists
   */
  public JClassOrInterfaceType getEnclosingType() {
    checkNotNull(enclosingType, "Top-level-classes do not have an enclosing type.");
    return enclosingType;
  }

  /**
   * Returns a <code>Set</code> containing one <code>JClassOrInterfaceType</code> for each
   * directly nested class or interface of the class or interface this object describes.
   *
   * <p>
   * If no nested types exist, an empty <code>Set</code> is returned.
   * </p>
   *
   * @return a <code>Set</code> containing descriptions for all directly nested types of the class
   *         or interface this object describes
   */
  public Set<JClassOrInterfaceType> getNestedTypes() {
    return ImmutableSet.copyOf(nestedTypes);
  }

  /**
   * Returns a <code>Set</code> containing one <code>JClassOrInterfaceType</code> for each
   * enclosing type of the class or interface described by this object.
   *
   * <p>
   * This includes directly and indirectly enclosing types.<br />
   * Example:
   * <pre>
   *  public class TopClass {
   *    public class NestedClass {
   *      public interface NestedInterface { }
   *    }
   *  }
   * </pre>
   * For the class hierarchy above and a <code>JClassOrInterfaceType</code> instance describing
   * <code>NestedInterface</code>, a call to this method returns a set containing descriptions for
   * <code>NestedClass</code> and <code>TopClass</code>.
   * </p>
   *
   * @return a <code>Set</code> containing one <code>JClassOrInterfaceType</code> for each
   * enclosing type of the class or interface described by this object
   */
  public final Set<JClassOrInterfaceType> getAllEnclosingTypes() {

    Set<JClassOrInterfaceType> result = new HashSet<>();

    JClassOrInterfaceType nextEnclosingInstance = enclosingType;

    while (!nextEnclosingInstance.isTopLevel()) {
      result.add(nextEnclosingInstance);
      nextEnclosingInstance = nextEnclosingInstance.getEnclosingType();
    }

    return result;
  }

  /**
   * Returns whether the class or interface described by this object is at the top level of a
   * class hierarchy. A class or interface is at the top level of a class hierarchy, if it is not
   * a nested type and does not contain an enclosing type.
   *
   * @return <code>true</code> if the class or interface described by this
   *         <code>JClassOrInterfaceType</code> does not have any enclosing types,
   *         <code>false</code> otherwise
   *
   */
  public boolean isTopLevel() {
    return enclosingType == null;
  }

  private void notifyEnclosingTypeOfNestedType(JClassOrInterfaceType nestedType) {
    checkArgument(!nestedTypes.contains(nestedType));
    nestedTypes.add(nestedType);
  }

  /**
   * Returns the simple name of the described class or interface.
   *
   * @return the simple name of the described class or interface
   */
  public String getSimpleName() {
    return simpleName;
  }
}