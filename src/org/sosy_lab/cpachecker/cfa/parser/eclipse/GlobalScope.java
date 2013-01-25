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
package org.sosy_lab.cpachecker.cfa.parser.eclipse;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

/**
 * Implementation of {@link Scope} for the global scope
 * (i.e., outside of functions).
 * Allows to register functions, types and global variables.
 */
public class GlobalScope implements Scope {

  private final Map<String, CSimpleDeclaration> globalVars = new HashMap<>();
  private final Map<String, CFunctionDeclaration> functions = new HashMap<>();

  @Override
  public boolean isGlobalScope() {
    return true;
  }

  @Override
  public boolean variableNameInUse(String name, String origName) {
      checkNotNull(name);
      checkNotNull(origName);

      CSimpleDeclaration binding = globalVars.get(origName);
      if (binding != null && binding.getName().equals(name)) {
        return true;
      }
      binding = globalVars.get(name);
      if (binding != null && binding.getName().equals(name)) {
        return true;
      }
      return false;
    }

  @Override
  public CSimpleDeclaration lookupVariable(String name) {
    checkNotNull(name);

    CSimpleDeclaration binding = globalVars.get(name);
    if (binding != null) {
      return binding;
    }

    return null;
  }

  @Override
  public CFunctionDeclaration lookupFunction(String name) {
    return functions.get(checkNotNull(name));
  }

  public void registerFunctionDeclaration(CFunctionDeclaration declaration) {
    String name = declaration.getName();
    assert name != null;

    if (functions.containsKey(name)) {
      // TODO multiple function declarations are legal, as long as they are equal
      // check this and throw exception if not
//        throw new CFAGenerationRuntimeException("Function " + name + " already declared", declaration);
    }

    if (globalVars.containsKey(name)) {
      throw new CFAGenerationRuntimeException("Name of global variable "
          + name + " from line " + globalVars.get(name).getFileLocation().getStartingLineNumber()
          + " is reused as function declaration", declaration);
    }

    functions.put(name, declaration);
  }

  @Override
  public void registerDeclaration(CSimpleDeclaration declaration) {
    assert declaration instanceof CVariableDeclaration
        || declaration instanceof CEnumerator
        : "Tried to register a declaration which does not define a name in the standard namespace: " + declaration;
    assert  !(declaration.getType() instanceof CFunctionType);

    String name = declaration.getOrigName();
    assert name != null;

    if (functions.containsKey(name)) {
      throw new CFAGenerationRuntimeException("Name of function "
          + name + " from line " + functions.get(name).getFileLocation().getStartingLineNumber()
          + " is reused as identifier in global scope", declaration);
    }

    globalVars.put(name, declaration);
  }

  public ImmutableMap<String, CFunctionDeclaration> getFunctions() {
    return ImmutableMap.copyOf(functions);
  }

  public ImmutableMap<String, CSimpleDeclaration> getGlobalVars() {
    return ImmutableMap.copyOf(globalVars);
  }

  @Override
  public String toString() {
    return "Functions: " + Joiner.on(' ').join(functions.keySet());
  }
}