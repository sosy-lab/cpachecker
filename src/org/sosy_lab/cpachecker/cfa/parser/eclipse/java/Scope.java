/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * Provides a symbol table that maps variable and methods to their declaration
 * (if a name is visible in the current scope).
 * Additionally, it tracks classes that still need to be parsed.
 */
class Scope {

  // Stores all found class and reference types
  private final Map< String ,JClassOrInterfaceType> types;

  // Track the name of the files which the types were extracted from
  // Key: TypeName Object: fileName
  private final Map<String, String> fileOfType;

  // symbolic tables for Fields
  private final Map<String , LinkedList<JFieldDeclaration>>
    typeFieldDeclarations = new HashMap<>();
  private final Map<String, JFieldDeclaration> fieldDeclarations = new HashMap<>();

  // symbolic table for  Variables and other Declarations
  private final LinkedList<Map<String, JSimpleDeclaration>> varsStack = Lists.newLinkedList();
  private final LinkedList<Map<String, JSimpleDeclaration>> varsList = Lists.newLinkedList();

  // Stores all found methods and constructors
  private final Map<String, JMethodDeclaration> methods = new HashMap<>();

  // Stores current class and method
  private String currentMethodName = null;
  private String currentClassName = null;

  // Stores enclosing classes
  private final Stack<String> classStack = new Stack<>();

  // fully Qualified main Class (not the ast name, but the real name with . instead of _)
  private final String fullyQualifiedMainClassName;
  private final String rootPathofProgram;

  //Track and deliver Classes that need to be parsed
  private final Queue<String> classesToBeParsed = new ConcurrentLinkedQueue<>();
  private final Set<String> registeredClasses = new HashSet<>();

  // Track depth of current Class
  private  int depth = 0;

  private  Map<String, JSimpleDeclaration> programScopeVars;

/**
 * Creates the Scope. It stores Information about the program as well
 * as creating symbolic tables to solve declarations.
 *
 * @param pFullyQualifiedMainClassName Name of the main Class of program. *
 * @param pRootPath Path to the root folder of current program.
 * @param pTypes Type Hierarchy of program created by {@link TypeHierachyCreator}
 * @param pFileOfTypes Maps types to the sourceFile they were extracted from
 */
  public Scope(String pFullyQualifiedMainClassName, String pRootPath,
      Map<String ,JClassOrInterfaceType> pTypes, Map<String, String> pFileOfTypes) {
    fullyQualifiedMainClassName = pFullyQualifiedMainClassName;
    types = pTypes;
    rootPathofProgram = pRootPath;
    enterProgramScope();
    registeredClasses.add(fullyQualifiedMainClassName); // Register Main Class
    fileOfType = pFileOfTypes;

    // initialize Lists in type Map for each type
    for( String classNames : types.keySet()) {
      typeFieldDeclarations.put(classNames, new LinkedList<JFieldDeclaration>());
    }
  }

  private void enterProgramScope() {
    varsStack.addLast(new HashMap<String, JSimpleDeclaration>());
    varsList.addLast(varsStack.getLast());
    programScopeVars = varsStack.getLast();
  }

  /**
   * Returns true, iff Scope is not within a Class or method.
   * In all other cases, returns false.
   *
   * @return true, iff Scope is not within method or Class.
   */
  public boolean isProgramScope() {
    return varsStack.size() == 1 && depth == 0;
  }

  /**
   * Returns true, if Scope is in the top-Level class
   * and not within a method. In all other cases false.
   *
   * @return true, iff Scope is within top-level class.
   */
  public boolean isTopClassScope() {
    return varsStack.size() == 1 && depth == 1;
  }

  /**
   * Is Called to indicate that a Visitor using this Scope
   * enters a method in iterating the JDT AST:
   *
   * @param methodDef indicates method the Visitor enters.
   */
  public void enterMethod(JMethodDeclaration methodDef) {
    currentMethodName = methodDef.getOrigName();
    registerMethodDeclaration(methodDef);
    enterBlock();
  }

  /**
  * Is Called to indicate that a Visitor using this Scope
  * enters a class in iterating the JDT AST:
  *
  * @param enteredClassType indicates the class the Visitor enters.
  */
  public void enterClass(JClassOrInterfaceType enteredClassType) {
      depth++;

      if(!types.containsKey(enteredClassType.getName())) {
        throw new CFAGenerationRuntimeException(
            "Could not find Type for Class" + enteredClassType.getName());
      }

      if(depth > 0) {
        classStack.push(currentClassName);
      }

      currentClassName = enteredClassType.getName();
      assert depth >= 0;
  }

  /**
   * Indicates that the Visitor using this scope
   * leaves current Class while traversing the JDT AST.
   */
  public void leaveClass() {
    depth--;

    if (depth == 0) {
      currentClassName = null;
    } else {

      if (classStack.size() == 0) { throw new CFAGenerationRuntimeException(
          "Could not find enclosing Class of this nested Class"); }

      currentClassName = classStack.pop();
    }

    assert depth >= 0;
  }

  /**
   * Indicates that the Visitor using this scope
   * leaves current Method while traversing the JDT AST.
   */
  public void leaveMethod() {
    checkState(!isTopClassScope());
    varsStack.removeLast();
    while (varsList.size() > varsStack.size()) {
      varsList.removeLast();
    }
    currentMethodName = null;
  }

  /**
   * Indicates that the Visitor using this scope
   * enters a Block while traversing the JDT AST.
   */
  public void enterBlock() {
    varsStack.addLast(new HashMap<String, JSimpleDeclaration>());
    varsList.addLast(varsStack.getLast());
  }

  /**
   * Indicates that the Visitor using this scope
   * leaves a Block while traversing the JDT AST.
   */
  public void leaveBlock() {
    checkState(varsStack.size() > 2);
    varsStack.removeLast();
  }

  /**
   * Checks if the name given in the parameters has already
   * a declaration to which it is linked.
   *
   * @param name Given name to be checked.
   * @param origName If the name has another Identification, it can also be given
   * with this parameter.
   * @return Returns true, if the name is already in use, else false.
   */
  public boolean variableNameInUse(String name, String origName) {
      checkNotNull(name);
      checkNotNull(origName);

      Iterator<Map<String, JSimpleDeclaration>> it = varsList.descendingIterator();
      while (it.hasNext()) {
        Map<String, JSimpleDeclaration> vars = it.next();

        JSimpleDeclaration binding = vars.get(origName);
        if (binding != null && binding.getName().equals(name)) {
          return true;
        }
        binding = vars.get(name);
        if (binding != null && binding.getName().equals(name)) {
          return true;
        }
      }
      return false;
    }

  /**
   * Returns the variable declaration with the name given as parameter.
   *
   * @param name Name of the Variable which declaration is to be returned.
   * @return declaration of given name, or null, if declaration was not found.
   */
  public JSimpleDeclaration lookupVariable(String name) {
    checkNotNull(name);

    Iterator<Map<String, JSimpleDeclaration>> it = varsStack.descendingIterator();
    while (it.hasNext()) {
      Map<String, JSimpleDeclaration> vars = it.next();

      JSimpleDeclaration binding = vars.get(name);
      if (binding != null) {
        return binding;
      }
    }
    return null;
  }

  /**
   * Returns the method declaration with the name given as parameter.
   *
   * @param name name of the method which declaration is to be returned.
   * @return declaration of given name, or null, if declaration was not found.
   */
  public JMethodDeclaration lookupMethod(String name) {
    return methods.get(checkNotNull(name));
  }

  /**
   * Checks if the method declaration with the name given as parameter
   * is already in use.
   *
   * @param name name of the method.
   * @return true, if found, else false.
   */
  public boolean isMethodRegistered(String name) {
    return methods.containsKey(checkNotNull(name));
  }


  public void registerDeclarationOfThisClass(JSimpleDeclaration declaration) {

    assert declaration instanceof JVariableDeclaration
        || declaration instanceof JParameterDeclaration
        : "Tried to register a declaration which does not define a name in the standard namespace: " + declaration;

    String name = declaration.getOrigName();
    assert name != null;

    Map<String, JSimpleDeclaration> vars = varsStack.getLast();

    if(isProgramScope()) {
      throw new CFAGenerationRuntimeException("Could not find Class for Declaration " + declaration.getName() , declaration);
    }

    // multiple declarations of the same variable are disallowed
    // unless i
    if (vars.containsKey(name)) {
      throw new CFAGenerationRuntimeException("Variable " + name + " already declared", declaration);
    }

    vars.put(name, declaration);

    if(declaration instanceof JFieldDeclaration) {
      registerFieldDeclarationOfThisClass((JFieldDeclaration) declaration);
    }
  }


   public void registerFieldDeclarationOfClass(JFieldDeclaration decl, JType type) {

     String name = decl.getName();

     // multiple declarations of the same variable are disallowed
     if (programScopeVars.containsKey(name)) {
       throw new CFAGenerationRuntimeException("Variable " + name + " already declared", decl);
     }

     programScopeVars.put(name, decl);

     if(type instanceof JClassOrInterfaceType) {
       registerFieldDeclarationOfClass(decl, ((JClassOrInterfaceType) type).getName());
     } else {
       registerFieldDeclarationOfUnspecifiedClass(decl);
     }
   }

  private void registerFieldDeclarationOfUnspecifiedClass(JFieldDeclaration declaration) {
    if (fieldDeclarations.containsKey(declaration.getName())) {
      throw new CFAGenerationRuntimeException("Variable " +
        declaration.getName() + " already declared", declaration);
      }

    if (isProgramScope()) {
      throw new CFAGenerationRuntimeException("Field Declaration" +
        "can only be declared within Classes");
      }

    fieldDeclarations.put(declaration.getName(), declaration);

  }

  private void registerFieldDeclarationOfClass(JFieldDeclaration declaration, String className) {

    if (fieldDeclarations.containsKey(declaration.getName())) { throw new CFAGenerationRuntimeException("Variable " +
        declaration.getName() + " already declared", declaration); }

    if (isProgramScope()) { throw new CFAGenerationRuntimeException("Field Declaration" +
        "can only be declared within Classes");
    }

    fieldDeclarations.put(declaration.getName(), declaration);
    typeFieldDeclarations.get(className).add(declaration);

  }

  private void registerFieldDeclarationOfThisClass(JFieldDeclaration declaration) {
    registerFieldDeclarationOfClass(declaration, currentClassName);
  }

  public void registerMethodDeclaration(JMethodDeclaration declaration) {
    checkState(!isProgramScope(), "method was not declared in class");

    String name = declaration.getName();
    assert name != null;

    methods.put(name, declaration);
  }

  public String getCurrentMethodName() {
    return currentMethodName;
  }

  @Override
  public String toString() {
    return "Functions: " + Joiner.on(' ').join(methods.keySet());
  }

  public void registerClass(ITypeBinding classBinding) {
    String className = classBinding.getQualifiedName();
    String topClassName = getTopLevelClass(classBinding);

    Queue<JClassOrInterfaceType> toBeAdded = new LinkedList<>();

    if (!registeredClasses.contains(className)) {

      if (!registeredClasses.contains(topClassName)){
        classesToBeParsed.add(topClassName);
      }

      registeredClasses.add(className);

    } else {
      // If top Class already added, it is unnecessary to search for subTypes
      // unless its the main Class
      if (!fullyQualifiedMainClassName.equals(className))
        return;
    }

    //Sub Classes need to be parsed for dynamic Binding
    JClassOrInterfaceType type = types.get(ASTConverter.getFullyQualifiedClassOrInterfaceName(classBinding));

    toBeAdded.addAll(type.getAllSubTypesOfType());

    for (JClassOrInterfaceType subClassType : toBeAdded) {

      String name = subClassType.getName();

      if (!registeredClasses.contains(name)) {
        registeredClasses.add(name);
        classesToBeParsed.add(name);
      }
    }
  }

  private String getTopLevelClass(ITypeBinding classBinding) {

    ITypeBinding nextClass = classBinding;

    for(ITypeBinding declaringClass = nextClass.getDeclaringClass();
        declaringClass != null; declaringClass = nextClass.getDeclaringClass()) {
      nextClass = declaringClass;
    }

    assert nextClass.isTopLevel();

    return nextClass.getQualifiedName();
  }

  public String getNextClassPath(){
    if(classesToBeParsed.isEmpty()){
      return null;
    } else {
      return classesToBeParsed.poll().replace('.', File.separatorChar) + ".java";
    }
  }

  public String getfullyQualifiedMainClassName() {
    return fullyQualifiedMainClassName;
  }

  public String getCurrentClassName() {
    return currentClassName;
  }

 public Map< String ,JClassOrInterfaceType> getTypeHierachie() {
   return types;
 }

  public String getRootPath() {
    return rootPathofProgram;
  }

  public Map<String, JFieldDeclaration> getFieldDeclarations() {
    return fieldDeclarations;
  }

  public Map<String, LinkedList<JFieldDeclaration>> getTypeFieldDeclarations() {
    return typeFieldDeclarations;
  }

  public Map<String, JFieldDeclaration> getStaticFieldDeclarations() {
    Map<String, JFieldDeclaration> result = new HashMap<>();

    for( JFieldDeclaration declaration : fieldDeclarations.values()) {
      if(declaration.isStatic()) {
        result.put(declaration.getName(), declaration);
      }
    }
    return result;
  }

  public Map<String, JFieldDeclaration> getNonStaticFieldDeclarationOfClass(String className) {
    Map<String, JFieldDeclaration> result = new HashMap<>();

    for( JFieldDeclaration declaration : typeFieldDeclarations.get(className)) {
      if(!declaration.isStatic()) {
        result.put(declaration.getName(), declaration);
      }
    }
    return result;
  }

  public String getFileOfCurrentType() {
    if(fileOfType.containsKey( currentClassName)) {
      return fileOfType.get(currentClassName);
    } else {
      return "";
    }
  }
}