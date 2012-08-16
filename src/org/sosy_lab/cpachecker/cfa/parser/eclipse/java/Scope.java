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
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * Provides a symbol table that maps variable and functions to their declaration
 * (if a name is visible in the current scope).
 */
class Scope {

  private String fullyQualifiedName;

  private final LinkedList<Map<String, IASimpleDeclaration>> varsStack = Lists.newLinkedList();
  private final LinkedList<Map<String, IASimpleDeclaration>> varsList = Lists.newLinkedList();

  //Track and deliver Classes To be Parsed
  private final Queue<String> classesToBeParsed = new ConcurrentLinkedQueue<String>();
  private final Set<String> registeredClasses = new HashSet<String>();


  private final Map<String, IASimpleDeclaration> functions = new HashMap<String, IASimpleDeclaration>();
  private String currentFunctionName = null;



  public Scope() {
    enterBlock(); // enter global scope
  }

  public Scope(String qualifiedName) {
    fullyQualifiedName = qualifiedName;
    registeredClasses.add(qualifiedName);
    enterBlock(); // enter global scope

  }

  public boolean isGlobalScope() {
    return varsStack.size() == 1;
  }

  public void enterFunction(AFunctionDeclaration pFuncDef) {
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
    varsStack.addLast(new HashMap<String, IASimpleDeclaration>());
    varsList.addLast(varsStack.getLast());
  }

  public void leaveBlock() {
    checkState(varsStack.size() > 2);
    varsStack.removeLast();
  }

  public boolean variableNameInUse(String name, String origName) {
      checkNotNull(name);
      checkNotNull(origName);

      Iterator<Map<String, IASimpleDeclaration>> it = varsList.descendingIterator();
      while (it.hasNext()) {
        Map<String, IASimpleDeclaration> vars = it.next();

        IASimpleDeclaration binding = vars.get(origName);
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

  public IASimpleDeclaration lookupVariable(String name) {
    checkNotNull(name);

    Iterator<Map<String, IASimpleDeclaration>> it = varsStack.descendingIterator();
    while (it.hasNext()) {
      Map<String, IASimpleDeclaration> vars = it.next();

      IASimpleDeclaration binding = vars.get(name);
      if (binding != null) {
        return binding;
      }
    }
    return null;
  }

  public IASimpleDeclaration lookupFunction(String name) {
    return functions.get(checkNotNull(name));
  }

  public void registerDeclaration(IASimpleDeclaration declaration) {
    assert declaration instanceof AVariableDeclaration
        || declaration instanceof CEnumerator
        || declaration instanceof AParameterDeclaration
        : "Tried to register a declaration which does not define a name in the standard namespace: " + declaration;
    assert  !(declaration.getType() instanceof CFunctionType);

    String name = declaration.getOrigName();
    assert name != null;

    Map<String, IASimpleDeclaration> vars = varsStack.getLast();

    // multiple declarations of the same variable are disallowed, unless when being in global scope
    if (vars.containsKey(name) && !isGlobalScope()) {
      throw new CFAGenerationRuntimeException("Variable " + name + " already declared", declaration);
    }

    vars.put(name, declaration);
  }

  public void registerFunctionDeclaration(AFunctionDeclaration declaration) {
    checkState(isGlobalScope(), "nested functions not allowed");

    String name = declaration.getName();
    assert name != null;

    if (functions.containsKey(name)) {
      // TODO multiple function declarations are legal, as long as they are equal
      // check this and throw exception if not
      // throw new CFAGenerationRuntimeException("Function " + name + " already declared", declaration);
    }

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
    String name = classBinding.getQualifiedName();
    if(!registeredClasses.contains(classBinding.getQualifiedName())){
      registeredClasses.add(name);
      classesToBeParsed.add(name);
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