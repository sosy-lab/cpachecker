/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import static com.google.common.base.Preconditions.*;

import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;


/**
 * Implementation of {@link Scope} for the local scope inside functions.
 * Only variables can be declared.
 * Provides the mechanism to have nested scopes (i.e., inside {} blocks).
 */
public class FunctionScope implements Scope {

  private final ImmutableMap<String, CFunctionDeclaration> functions;
  private final ImmutableMap<String, CComplexTypeDeclaration> types;
  private final Deque<Map<String, CSimpleDeclaration>> varsStack = Lists.newLinkedList();
  private final Deque<Map<String, CSimpleDeclaration>> varsList = Lists.newLinkedList();

  private String currentFunctionName = null;

  public FunctionScope(ImmutableMap<String, CFunctionDeclaration> pFunctions,
      ImmutableMap<String, CComplexTypeDeclaration> pTypes,
      ImmutableMap<String, CSimpleDeclaration> pGlobalVars) {

    functions = pFunctions;
    types = pTypes;
    varsStack.push(pGlobalVars);
    varsList.push(pGlobalVars);

    enterBlock();
  }

  public FunctionScope() {
    this(ImmutableMap.<String, CFunctionDeclaration>of(),
         ImmutableMap.<String, CComplexTypeDeclaration>of(),
         ImmutableMap.<String, CSimpleDeclaration>of());
  }

  @Override
  public boolean isGlobalScope() {
    return false;
  }

  public void enterFunction(CFunctionDeclaration pFuncDef) {
    checkState(currentFunctionName == null);
    currentFunctionName = pFuncDef.getOrigName();
    checkArgument(functions.containsKey(currentFunctionName));
  }

  public void enterBlock() {
    varsStack.addLast(new HashMap<String, CSimpleDeclaration>());
    varsList.addLast(varsStack.getLast());
  }

  public void leaveBlock() {
    checkState(varsStack.size() > 2);
    varsStack.removeLast();
  }

  @Override
  public boolean variableNameInUse(String name, String origName) {
      checkNotNull(name);
      checkNotNull(origName);

      Iterator<Map<String, CSimpleDeclaration>> it = varsList.descendingIterator();
      while (it.hasNext()) {
        Map<String, CSimpleDeclaration> vars = it.next();

        CSimpleDeclaration binding = vars.get(origName);
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

  @Override
  public CSimpleDeclaration lookupVariable(String name) {
    checkNotNull(name);

    Iterator<Map<String, CSimpleDeclaration>> it = varsStack.descendingIterator();
    while (it.hasNext()) {
      Map<String, CSimpleDeclaration> vars = it.next();

      CSimpleDeclaration binding = vars.get(name);
      if (binding != null) {
        return binding;
      }
    }
    return null;
  }

  @Override
  public CFunctionDeclaration lookupFunction(String name) {
    return functions.get(checkNotNull(name));
  }

  @Override
  public CComplexType lookupType(String name) {
    CComplexTypeDeclaration declaration = types.get(checkNotNull(name));
    if (declaration != null) {
      return declaration.getType();
    }
    return null;
  }

  @Override
  public void registerDeclaration(CSimpleDeclaration declaration) {
    assert declaration instanceof CVariableDeclaration
        || declaration instanceof CEnumerator
        || declaration instanceof CParameterDeclaration
        : "Tried to register a declaration which does not define a name in the standard namespace: " + declaration;
    assert  !(declaration.getType() instanceof CFunctionType);

    String name = declaration.getOrigName();
    assert name != null;

    Map<String, CSimpleDeclaration> vars = varsStack.getLast();

    // multiple declarations of the same variable are disallowed
    if (vars.containsKey(name)) {
      throw new CFAGenerationRuntimeException("Variable " + name + " already declared", declaration);
    }

    vars.put(name, declaration);
  }

  public String getCurrentFunctionName() {
    return currentFunctionName;
  }

  @Override
  public String toString() {
    return "Functions: " + Joiner.on(' ').join(functions.keySet());
  }
}
