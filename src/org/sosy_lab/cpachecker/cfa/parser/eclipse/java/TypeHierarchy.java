// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JConstructorDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.VisibilityModifier;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.EclipseJavaParser.JavaFileAST;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JConstructorType;
import org.sosy_lab.cpachecker.cfa.types.java.JInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JMethodType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.exceptions.JParserException;

final class TypeHierarchy {

  private Map<String, JClassOrInterfaceType> types;

  /*
   * Maps the filename of the file to the type, which it was created from.
   */
  private Map<JClassOrInterfaceType, Path> fileOfTypes;

  private Map<JClassOrInterfaceType, Set<JMethodDeclaration>> methodDeclarationsOfType;

  private Map<JClassOrInterfaceType, ImmutableSet<JFieldDeclaration>> fieldDeclarationsOfType;

  /*
   * This class stores the same type information in mutable Maps.
   * It is used to update the type hierarchy.
   */
  private THTypeTable typeTable;

  private TypeHierarchy(THTypeTable pTypeTable) {

    checkNotNull(pTypeTable);

    typeTable = pTypeTable;
    types = pTypeTable.getTypes();
    fileOfTypes = pTypeTable.getTypeOfFiles();
    methodDeclarationsOfType = pTypeTable.getMethodDeclarationsOfType();
    fieldDeclarationsOfType = pTypeTable.getFieldDeclarationsOfType();
  }

  public Set<JClassOrInterfaceType> getTypes() {
    Set<JClassOrInterfaceType> typeSet = new HashSet<>();

    for (JClassOrInterfaceType type : types.values()) {
      checkArgument(!typeSet.contains(type));
      typeSet.add(type);
    }

    return typeSet;
  }

  public Map<JClassOrInterfaceType, Path> getFileOfTypes() {
    return fileOfTypes;
  }

  public Map<String, JMethodDeclaration> getMethodDeclarations() {
    Map<String, JMethodDeclaration> declarations = new HashMap<>();

    for (Set<JMethodDeclaration> decls : methodDeclarationsOfType.values()) {
      for (JMethodDeclaration decl : decls) {
        String name = decl.getName();
        checkArgument(!declarations.containsKey(name));
        declarations.put(name, decl);
      }
    }

    return declarations;
  }

  public Set<JMethodDeclaration> getMethodDeclarations(JClassOrInterfaceType type) {
    checkArgument(!isExternType(type), "Can't get the declarations of extern types.");
    return methodDeclarationsOfType.get(type);
  }

  public Map<String, JFieldDeclaration> getFieldDeclarations() {
    Map<String, JFieldDeclaration> declarations = new HashMap<>();

    for (Set<JFieldDeclaration> decls : fieldDeclarationsOfType.values()) {
      for (JFieldDeclaration decl : decls) {
        String name = decl.getName();
        checkArgument(!declarations.containsKey(name));
        declarations.put(name, decl);
      }
    }

    return declarations;
  }

  public Set<JFieldDeclaration> getFieldDeclarations(JClassOrInterfaceType type) {
    checkArgument(!isExternType(type), "Can't get the declarations of extern types.");
    return fieldDeclarationsOfType.get(type);
  }

  public static TypeHierarchy createTypeHierachy(LogManager pLogger, List<JavaFileAST> pJavaProgram)
      throws JParserException {

    TypeHierachyCreator creator = new TypeHierachyCreator(pLogger, new THTypeTable());

    creator.createTypeHierachy(pJavaProgram);

    return new TypeHierarchy(creator.getTypeTable());
  }

  public boolean containsType(JClassOrInterfaceType pType) {
    return types.containsKey(pType.getName());
  }

  public boolean containsType(String pFullyQualifiedTypeName) {
    return types.containsKey(pFullyQualifiedTypeName);
  }

  @Nullable
  public JClassOrInterfaceType getType(String pFullyQualifiedTypeName) {
    return types.get(pFullyQualifiedTypeName);
  }

  @Nullable
  public Path getFileOfType(JClassOrInterfaceType pType) {
    return fileOfTypes.get(pType);
  }

  public boolean isExternType(JClassOrInterfaceType pType) {
    return !fileOfTypes.containsKey(pType);
  }

  public boolean containsInterfaceType(String pTypeName) {
    boolean exists = types.containsKey(pTypeName);
    boolean isInterface = types.get(pTypeName) instanceof JInterfaceType;
    return exists && isInterface;
  }

  public JInterfaceType getInterfaceType(String pTypeName) {

    JClassOrInterfaceType type = types.get(pTypeName);

    checkState((type instanceof JInterfaceType), "Interface Type does not exist");

    return (JInterfaceType) types.get(pTypeName);
  }

  public boolean containsClassType(String pTypeName) {
    boolean exists = types.containsKey(pTypeName);
    boolean isClass = types.get(pTypeName) instanceof JClassType;
    return exists && isClass;
  }

  public JClassType getClassType(String pTypeName) {

    JClassOrInterfaceType type = types.get(pTypeName);

    checkState((type instanceof JClassType), "Interface Type does not exist");

    return (JClassType) types.get(pTypeName);
  }

  /** Returns the class type of the Java 'Object' type. */
  public JClassType getClassTypeOfObject() {
    return typeTable.getTypeOfObject();
  }

  /** Returns the class type that identifies unresolvable Java types. */
  public JClassType getUnresolvableClassType() {
    return typeTable.getUnresolvableClassType();
  }

  public JMethodDeclaration getUnresolvableMethodDeclaration() {
    return typeTable.getUnresolvableMethodDeclaration();
  }

  void updateTypeHierarchy(ITypeBinding classOrInterfaceBinding) {

    checkNotNull(classOrInterfaceBinding);
    checkArgument(
        classOrInterfaceBinding.isClass()
            || classOrInterfaceBinding.isEnum()
            || classOrInterfaceBinding.isInterface());

    THTypeConverter converter = new THTypeConverter(typeTable);

    converter.convertClassOrInterfaceType(classOrInterfaceBinding);

    updateFromTypeTable(typeTable);
  }

  void updateTypeHierarchy(JClassType pJClassType) {

    if (typeTable.containsType(pJClassType.getName())) {
      return;
    }
    typeTable.registerType(pJClassType);

    JClassType superClass = pJClassType.getParentClass();

    while (superClass != null && !typeTable.containsType(superClass.getName())) {
      typeTable.registerType(superClass);
      superClass = superClass.getParentClass();
    }

    updateFromTypeTable(typeTable);
  }

  private void updateFromTypeTable(THTypeTable pTypeTable) {
    types = pTypeTable.getTypes();
    fileOfTypes = pTypeTable.getTypeOfFiles();
    methodDeclarationsOfType = pTypeTable.getMethodDeclarationsOfType();
    fieldDeclarationsOfType = pTypeTable.getFieldDeclarationsOfType();

    typeTable = pTypeTable;
  }

  void updateTypeHierarchy(
      AnonymousClassDeclaration pDeclaration, Path pFileName, LogManager pLogger) {

    // this hierarchyCreator has to be initialized after fileOfTypes was updated,
    // so we don't overwrite this update below
    TypeHierachyCreator hierarchyCreator = new TypeHierachyCreator(pLogger, typeTable, pFileName);

    pDeclaration.accept(hierarchyCreator);

    updateFromTypeTable(hierarchyCreator.getTypeTable());
  }

  public JConstructorType getUnresolvableConstructorType() {
    return typeTable.getUnresolvableConstructorType();
  }

  public JConstructorDeclaration getUnresolvableConstructorDeclaration() {
    return typeTable.getUnresolvableConstructorDeclaration();
  }

  static class THTypeTable {

    private final Map<String, JClassOrInterfaceType> types = new HashMap<>();
    private final Map<JClassOrInterfaceType, Path> typeOfFiles = new HashMap<>();

    private final Map<JClassOrInterfaceType, Set<JMethodDeclaration>> methodDeclarationsOfType =
        new HashMap<>();

    private final Map<JClassOrInterfaceType, ImmutableSet<JFieldDeclaration>>
        fieldDeclarationsOfType = new HashMap<>();

    private final JClassType objectType;
    private final JClassType unresolvableClassType;
    private final JMethodDeclaration unresolvableMethodDeclaration;
    private final JConstructorType unresolvableConstructorType;
    private final JConstructorDeclaration unresolvableConstructorDeclaration;

    private THTypeTable() {
      // Create the Object Type.
      objectType = JClassType.createObjectType();
      registerType(objectType);

      unresolvableClassType =
          JClassType.valueOf(
              "_unspecified_",
              "_unspecified_",
              VisibilityModifier.NONE,
              false,
              false,
              false,
              objectType,
              new HashSet<>());

      JMethodType unresolvableMethodType =
          new JMethodType(JSimpleType.getUnspecified(), new ArrayList<>(), false);
      unresolvableMethodDeclaration =
          new JMethodDeclaration(
              FileLocation.DUMMY,
              unresolvableMethodType,
              "__Unresolved__",
              "__Unresolved__",
              new ArrayList<>(),
              VisibilityModifier.NONE,
              false,
              false,
              false,
              false,
              false,
              false,
              unresolvableClassType);
      unresolvableConstructorType =
          new JConstructorType(unresolvableClassType, new ArrayList<>(), false);
      unresolvableConstructorDeclaration =
          new JConstructorDeclaration(
              FileLocation.DUMMY,
              unresolvableConstructorType,
              "__UNRESOLVABLE__",
              "__UNRESOLVABLE__",
              new ArrayList<>(),
              VisibilityModifier.NONE,
              false,
              unresolvableClassType);
    }

    public Map<String, JClassOrInterfaceType> getTypes() {
      return ImmutableMap.copyOf(types);
    }

    public Map<JClassOrInterfaceType, Path> getTypeOfFiles() {
      return new HashMap<>(typeOfFiles);
    }

    public void registerType(JClassOrInterfaceType pType) {
      if (pType instanceof JClassType classType) {
        checkSuperClassConsistency(classType);
      }
      types.put(pType.getName(), pType);
      methodDeclarationsOfType.put(pType, new HashSet<>());
      fieldDeclarationsOfType.put(pType, ImmutableSet.of());
    }

    private void checkSuperClassConsistency(JClassType pClassType) {
      Deque<JClassType> superClassHierarchy = new ArrayDeque<>();

      JClassType nextSuperClass = pClassType.getParentClass();

      while (nextSuperClass != null) {
        checkArgument(
            !nextSuperClass.equals(pClassType),
            "Class %s may not be a super class of itself.",
            pClassType.getName());
        superClassHierarchy.push(nextSuperClass);
        nextSuperClass = nextSuperClass.getParentClass();
      }

      JClassType upperMostSuperclass = superClassHierarchy.peek();
      checkArgument(
          objectType.equals(upperMostSuperclass),
          "Class %s must be a sub-class of Object",
          pClassType.getName());
    }

    /**
     * Registers the method declaration to the declaring Type.
     *
     * @param pDecl the method declaration to be registered.
     */
    public void registerMethodDeclaration(JMethodDeclaration pDecl) {

      JClassOrInterfaceType declaringClass = pDecl.getDeclaringClass();
      checkArgument(methodDeclarationsOfType.containsKey(declaringClass));
      checkArgument(!methodDeclarationsOfType.get(declaringClass).contains(pDecl));
      methodDeclarationsOfType.get(declaringClass).add(pDecl);
    }

    /**
     * Registers the JDT field declaration to the declaring Type.
     *
     * @param pDecl the method declaration to be registered.
     */
    public void registerFieldDeclaration(
        JFieldDeclaration pDecl, JClassOrInterfaceType declaringClass) {

      checkArgument(fieldDeclarationsOfType.containsKey(declaringClass));
      checkArgument(!fieldDeclarationsOfType.get(declaringClass).contains(pDecl));
      ImmutableSet<JFieldDeclaration> jFieldDeclarations =
          fieldDeclarationsOfType.get(declaringClass);
      jFieldDeclarations =
          new ImmutableSet.Builder<JFieldDeclaration>()
              .addAll(jFieldDeclarations)
              .add(pDecl)
              .build();
      fieldDeclarationsOfType.put(declaringClass, jFieldDeclarations);
    }

    public void registerFileNameOfType(JClassOrInterfaceType type, Path fileName) {
      typeOfFiles.put(type, fileName);
    }

    public boolean containsType(String typeName) {
      return types.containsKey(typeName);
    }

    public JClassOrInterfaceType getType(String typeName) {
      return types.get(typeName);
    }

    public JClassType getTypeOfObject() {
      return objectType;
    }

    public JClassType getUnresolvableClassType() {
      return unresolvableClassType;
    }

    public JMethodDeclaration getUnresolvableMethodDeclaration() {
      return unresolvableMethodDeclaration;
    }

    public JConstructorDeclaration getUnresolvableConstructorDeclaration() {
      return unresolvableConstructorDeclaration;
    }

    public JConstructorType getUnresolvableConstructorType() {
      return unresolvableConstructorType;
    }

    public Map<JClassOrInterfaceType, Set<JMethodDeclaration>> getMethodDeclarationsOfType() {
      return ImmutableMap.copyOf(
          Maps.transformValues(methodDeclarationsOfType, ImmutableSet::copyOf));
    }

    public Map<JClassOrInterfaceType, ImmutableSet<JFieldDeclaration>>
        getFieldDeclarationsOfType() {
      return ImmutableMap.copyOf(
          Maps.transformValues(fieldDeclarationsOfType, ImmutableSet::copyOf));
    }

    public void registerFieldDeclaration(
        Set<JFieldDeclaration> pDecl, JClassOrInterfaceType pDeclaringClass) {

      for (JFieldDeclaration decl : pDecl) {
        registerFieldDeclaration(decl, pDeclaringClass);
      }
    }
  }
}
