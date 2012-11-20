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
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * Provides a symbol table that maps variable and functions to their declaration
 * (if a name is visible in the current scope).
 */
class Scope {

  private String fullyQualifiedName;

  private final LinkedList<Map<String, JSimpleDeclaration>> varsStack = Lists.newLinkedList();
  private final LinkedList<Map<String, JSimpleDeclaration>> varsList = Lists.newLinkedList();

  //Track and deliver Classes To be Parsed
  private final Queue<String> classesToBeParsed = new ConcurrentLinkedQueue<String>();
  private final Set<String> registeredClasses = new HashSet<String>();

  private final Map< String ,JClassOrInterfaceType> types ;

  private final Map<String, JMethodDeclaration> functions = new HashMap<String, JMethodDeclaration>();
  private String currentFunctionName = null;



  public Scope(Map< String ,JClassOrInterfaceType> pTypes) {
    types = pTypes;
    enterBlock(); // enter global scope
  }

  public Scope(String qualifiedName, Map<String ,JClassOrInterfaceType> pTypes) {
    fullyQualifiedName = qualifiedName;
    registeredClasses.add(qualifiedName);
    types = pTypes;
    enterBlock(); // enter global scope

  }

  public boolean isGlobalScope() {
    return varsStack.size() == 1;
  }

  public void enterFunction(JMethodDeclaration pFuncDef) {
    currentFunctionName = pFuncDef.getOrigName();
    registerFunctionDeclaration(pFuncDef);

    enterBlock();
  }

  public void leaveFunction() {
    checkState(!isGlobalScope());
    varsStack.removeLast();
    while (varsList.size() > varsStack.size()) {
      varsList.removeLast();
    }
    currentFunctionName = null;
  }

  public void enterBlock() {
    varsStack.addLast(new HashMap<String, JSimpleDeclaration>());
    varsList.addLast(varsStack.getLast());
  }

  public void leaveBlock() {
    checkState(varsStack.size() > 2);
    varsStack.removeLast();
  }

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

  public JMethodDeclaration lookupFunction(String name) {
    return functions.get(checkNotNull(name));
  }

  public void registerDeclaration(JSimpleDeclaration declaration) {
    assert declaration instanceof JVariableDeclaration
        || declaration instanceof JParameterDeclaration
        : "Tried to register a declaration which does not define a name in the standard namespace: " + declaration;
    assert  !(declaration.getType() instanceof CFunctionType);

    String name = declaration.getOrigName();
    assert name != null;

    Map<String, JSimpleDeclaration> vars = varsStack.getLast();

    // multiple declarations of the same variable are disallowed, unless when being in global scope
    if (vars.containsKey(name) && !isGlobalScope()) {
      throw new CFAGenerationRuntimeException("Variable " + name + " already declared", declaration);
    }

    vars.put(name, declaration);
  }

  public void registerFunctionDeclaration(JMethodDeclaration declaration) {
    checkState(isGlobalScope(), "nested functions not allowed");

    String name = declaration.getName();
    assert name != null;

    functions.put(name, declaration);
  }



  public String getCurrentFunctionName() {
    return currentFunctionName;
  }

  @Override
  public String toString() {
    return "Functions: " + Joiner.on(' ').join(functions.keySet());
  }



  public void registerClasses(ITypeBinding classBinding){
    String topClassName = classBinding.getQualifiedName();
    Queue<JClassOrInterfaceType> toBeAdded = new LinkedList<JClassOrInterfaceType>();

    if(!registeredClasses.contains(classBinding.getQualifiedName())){
      registeredClasses.add(topClassName);
      classesToBeParsed.add(topClassName);


    } else {
      // If top Class already added, it is unnecessary to search for subTypes
      // unless its the main Class
      if(!fullyQualifiedName.equals(topClassName))
      return;
    }


    //Sub Classes need to be parsed for dynamic Binding

    JClassOrInterfaceType type = types.get(ASTConverter.getFullyQualifiedClassOrInterfaceName( classBinding));

    toBeAdded.addAll(type.getAllSubTypesOfType());


    for(JClassOrInterfaceType subClassType : toBeAdded){

      String name = ASTConverter.getFullyQualifiedBindingNameFromType(subClassType);

      if(!registeredClasses.contains(name)){
        registeredClasses.add(name);
        classesToBeParsed.add(name);
      }
    }
  }

  public String getNextClassPath(){
    if(classesToBeParsed.isEmpty()){
      return null;
    } else {
      return classesToBeParsed.poll().replace('.', File.separatorChar) + ".java";
    }
  }

  public String getFullyQualifiedName() {
    return fullyQualifiedName;
  }
}