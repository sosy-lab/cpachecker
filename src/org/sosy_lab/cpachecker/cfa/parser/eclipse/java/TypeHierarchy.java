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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import static com.google.common.base.Preconditions.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.EclipseJavaParser.JavaFileAST;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JInterfaceType;
import org.sosy_lab.cpachecker.exceptions.JParserException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;


public final class TypeHierarchy {

  private final LogManager logger;

  private Map<String, JClassOrInterfaceType> types;

  /*
   * Maps the filename of the file to the type, which it was created from.
   */
  private Map<JClassOrInterfaceType, String> fileOfTypes;

  private Map<JClassOrInterfaceType, Set<JMethodDeclaration>> methodDeclarationsOfType;

  private Map<JClassOrInterfaceType, Set<JFieldDeclaration>> fieldDeclarationsOfType;

  /*
   * This class stores the same type information in mutable Maps.
   * It is used to update the type hierarchy.
   */
  private final THTypeTable typeTable;

  private TypeHierarchy(THTypeTable pTypeTable, LogManager pLogger) {

    checkNotNull(pTypeTable);
    checkNotNull(pLogger);

    logger = pLogger;
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

  public Map<JClassOrInterfaceType, String> getFileOfTypes() {
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

  public static TypeHierarchy createTypeHierachy
      (LogManager pLogger, List<JavaFileAST> pJavaProgram) throws JParserException {

    TypeHierachyCreator creator = new TypeHierachyCreator(pLogger, new THTypeTable());

    creator.createTypeHierachy(pJavaProgram);

    return new TypeHierarchy(creator.getTypeTable(), pLogger);
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
  public String getFileOfType(JClassOrInterfaceType pType) {
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

    JClassOrInterfaceType type =  types.get(pTypeName);

    if (type == null || !(type instanceof JInterfaceType)) {
      throw new IllegalStateException("Interface Type does not exist");
    }

    return (JInterfaceType) types.get(pTypeName);
  }

  public boolean containsClassType(String pTypeName) {
    boolean exists = types.containsKey(pTypeName);
    boolean isClass = types.get(pTypeName) instanceof JClassType;
    return exists && isClass;
  }

  public JClassType getClassType(String pTypeName) {

    JClassOrInterfaceType type =  types.get(pTypeName);

    if (type == null || !(type instanceof JClassType)) {
      throw new IllegalStateException("Interface Type does not exist");
    }

    return (JClassType) types.get(pTypeName);
  }

  void updateTypeHierarchy(ITypeBinding classOrInterfaceBinding) {

    checkNotNull(classOrInterfaceBinding);
    checkArgument(classOrInterfaceBinding.isClass() || classOrInterfaceBinding.isEnum()
        || classOrInterfaceBinding.isInterface());

    THTypeConverter converter = new THTypeConverter(logger, typeTable);

    converter.convertClassOrInterfaceType(classOrInterfaceBinding);

    types = typeTable.getTypes();
    fileOfTypes = typeTable.getTypeOfFiles();
    methodDeclarationsOfType = typeTable.getMethodDeclarationsOfType();
  }

  static class THTypeTable {

    private final Map<String, JClassOrInterfaceType> types = new HashMap<>();
    private final Map<JClassOrInterfaceType, String> typeOfFiles = new HashMap<>();

    private final Map<JClassOrInterfaceType, Set<JMethodDeclaration>>
                                                    methodDeclarationsOfType = new HashMap<>();

    private final Map<JClassOrInterfaceType, Set<JFieldDeclaration>> fieldDeclarationsOfType
                                                                                = new HashMap<>();

    private THTypeTable() {
      // Create the Object Type.
      JClassType objectType = JClassType.getTypeOfObject();
      registerType(objectType);
    }

    public Map<String, JClassOrInterfaceType> getTypes() {
      return ImmutableMap.copyOf(types);
    }

    public Map<JClassOrInterfaceType, String> getTypeOfFiles() {
      return ImmutableMap.copyOf(typeOfFiles);
    }

    public void registerType(JClassOrInterfaceType pType) {
      types.put(pType.getName(), pType);
      methodDeclarationsOfType.put(pType, new HashSet<JMethodDeclaration>());
      fieldDeclarationsOfType.put(pType, new HashSet<JFieldDeclaration>());
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
    public void registerFieldDeclaration(JFieldDeclaration pDecl, JClassOrInterfaceType declaringClass) {

      checkArgument(fieldDeclarationsOfType.containsKey(declaringClass));
      checkArgument(!fieldDeclarationsOfType.get(declaringClass).contains(pDecl));
      fieldDeclarationsOfType.get(declaringClass).add(pDecl);
    }

    public void registerFileNameOfType(JClassOrInterfaceType type, String fileName) {
      typeOfFiles.put(type, fileName);
    }

    public boolean containsType(String typeName) {
      return types.containsKey(typeName);
    }

    public JClassOrInterfaceType getType(String typeName) {
      return types.get(typeName);
    }

    public Map<JClassOrInterfaceType, Set<JMethodDeclaration>> getMethodDeclarationsOfType() {

      Map<JClassOrInterfaceType, Set<JMethodDeclaration>> tmp = new HashMap<>();

      for (JClassOrInterfaceType type : methodDeclarationsOfType.keySet()) {
        Set<JMethodDeclaration> set = methodDeclarationsOfType.get(type);
        tmp.put(type, ImmutableSet.copyOf(set));
      }

      return ImmutableMap.copyOf(tmp);
    }

    public Map<JClassOrInterfaceType, Set<JFieldDeclaration>> getFieldDeclarationsOfType() {
      Map<JClassOrInterfaceType, Set<JFieldDeclaration>> tmp = new HashMap<>();

      for (JClassOrInterfaceType type : fieldDeclarationsOfType.keySet()) {
        Set<JFieldDeclaration> set = fieldDeclarationsOfType.get(type);
        tmp.put(type, ImmutableSet.copyOf(set));
      }

      return ImmutableMap.copyOf(tmp);
    }

    public void registerFieldDeclaration(Set<JFieldDeclaration> pDecl,
        JClassOrInterfaceType pDeclaringClass) {

      for (JFieldDeclaration decl : pDecl) {
        registerFieldDeclaration(decl, pDeclaringClass);
      }
    }

  }
}