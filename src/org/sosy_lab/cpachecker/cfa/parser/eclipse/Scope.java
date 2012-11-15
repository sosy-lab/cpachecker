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
package org.sosy_lab.cpachecker.cfa.parser.eclipse;

import static com.google.common.base.Preconditions.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Provides a symbol table that maps variable and functions to their declaration
 * (if a name is visible in the current scope).
 */
class Scope {

  /** This stack contains all vars, that are declared at a specific level.
   * The first level is "global", the second level is "function-scope",
   * each further level is "block-scope". */
  private final LinkedList<Map<String, CSimpleDeclaration>> varsStack = Lists.newLinkedList();
  private final Set<String> usedNames = Sets.newHashSet();

  private final Map<String, CSimpleDeclaration> functions = new HashMap<String, CSimpleDeclaration>();
  private String currentFunctionName = null;

  public Scope() {
    enterBlock(); // enter global scope
  }

  public boolean isGlobalScope() {
    return varsStack.size() == 1;
  }

  public void enterFunction(CFunctionDeclaration pFuncDef) {
    checkState(isGlobalScope());
    currentFunctionName = pFuncDef.getOrigName();
    registerFunctionDeclaration(pFuncDef);
    enterBlock();
  }

  public void leaveFunction() {
    varsStack.removeLast();
    currentFunctionName = null;
    checkState(isGlobalScope());
  }

  public void enterBlock() {
    varsStack.addLast(new HashMap<String, CSimpleDeclaration>());
  }

  public void leaveBlock() {
    checkState(varsStack.size() > 2);
    varsStack.removeLast();
  }

  /** returns a unique name for a original variable-name.
   * If the original name hides another (equal) name,
   * the returned name has an unique index. */
  public String getUniqueName(final String origName) {
    checkNotNull(origName);

    if (!usedNames.contains(origName)) {
      usedNames.add(origName);
      return origName;
    }

    int i = 0;
    String uniqueName;
    do {
      uniqueName = origName + "__" + (i++);
    } while (usedNames.contains(uniqueName));

    usedNames.add(uniqueName);
    return uniqueName;
  }

  /** returns the declaration for this name.
   * If the name hides another name from higher scope,
   * the declaration from lowest scope is returned. */
  public CSimpleDeclaration lookupVariable(String origName) {
    checkNotNull(origName);

    for (Map<String, CSimpleDeclaration> vars : Lists.reverse(varsStack)){
      CSimpleDeclaration binding = vars.get(origName);
      if (binding != null) {
        return binding;
      }
    }
    return null;
  }

  public CSimpleDeclaration lookupFunction(String name) {
    return functions.get(checkNotNull(name));
  }

  public void registerDeclaration(CSimpleDeclaration declaration) {
    assert declaration instanceof CVariableDeclaration
        || declaration instanceof CEnumerator
        || declaration instanceof CParameterDeclaration
        : "Tried to register a declaration which does not define a name in the standard namespace: " + declaration;
    assert  !(declaration.getType() instanceof CFunctionType);

    String name = declaration.getOrigName();
    assert name != null;

    Map<String, CSimpleDeclaration> vars = varsStack.getLast();

    // multiple declarations of the same variable are disallowed, unless when being in global scope
    if (vars.containsKey(name) && !isGlobalScope()) {
      throw new CFAGenerationRuntimeException("Variable " + name + " already declared", declaration);
    }

    vars.put(name, declaration);
    usedNames.add(name);
  }

  public void registerFunctionDeclaration(CFunctionDeclaration declaration) {
    checkState(isGlobalScope(), "nested functions not allowed");

    String name = declaration.getName();
    assert name != null;

    if (functions.containsKey(name)) {
      // TODO multiple function declarations are legal, as long as they are equal
      // check this and throw exception if not
//        throw new CFAGenerationRuntimeException("Function " + name + " already declared", declaration);
    }

    functions.put(name, declaration);
    usedNames.add(name);
  }

  public String getCurrentFunctionName() {
    return currentFunctionName;
  }

  @Override
  public String toString() {
    return "Functions: " + Joiner.on(' ').join(functions.keySet());
  }
}