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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Implementation of {@link Scope} for the global scope
 * (i.e., outside of functions).
 * Allows to register functions, types and global variables.
 */
class GlobalScope implements Scope {

  private final Map<String, CSimpleDeclaration> globalVars;
  private final Map<String, CFunctionDeclaration> functions;
  private final Map<String, CComplexTypeDeclaration> types;
  private final Map<String, CTypeDefDeclaration> typedefs;
  private final Set<String> alreayTakenTypeNames;

  public GlobalScope(Map<String, CSimpleDeclaration> globalVars,
                     Map<String, CFunctionDeclaration> functions,
                     Map<String, CComplexTypeDeclaration> types,
                     Map<String, CTypeDefDeclaration> typedefs,
                     Set<String> alreadyTakenTypeNames) {
    this.globalVars = globalVars;
    this.functions = functions;
    this.types = types;
    this.typedefs = typedefs;
    this.alreayTakenTypeNames = alreadyTakenTypeNames;
  }

  public GlobalScope() {
    this(new HashMap<String, CSimpleDeclaration>(),
         new HashMap<String, CFunctionDeclaration>(),
         new HashMap<String, CComplexTypeDeclaration>(),
         new HashMap<String, CTypeDefDeclaration>(),
         new HashSet<String>());
  }

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

  @Override
  public CComplexType lookupType(String name) {
    CComplexTypeDeclaration declaration = types.get(checkNotNull(name));
    if (declaration != null) {
      return declaration.getType();
    }

    return null;
  }

  @Override
  public CType lookupTypedef(final String name) {
    checkNotNull(name);

    final CTypeDefDeclaration declaration = typedefs.get(name);
    if (declaration != null) {
      return declaration.getType();
    }

    return null;
  }

  @Override
  public boolean isTypeNameAvailable(String name) {
    return !alreayTakenTypeNames.contains(name);
  }

  @Override
  public String createScopedNameOf(String pName) {
    return pName;
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
    assert  !(declaration.getType() instanceof CFunctionTypeWithNames);

    String name = declaration.getOrigName();
    assert name != null;

    if (functions.containsKey(name)) {
      throw new CFAGenerationRuntimeException("Name of function "
          + name + " from line " + functions.get(name).getFileLocation().getStartingLineNumber()
          + " is reused as identifier in global scope", declaration);
    }

    globalVars.put(name, declaration);
  }

  /**
   * Register a type, e.g., a new struct type.
   *
   * @return True if the type actually needs to be declared, False if the declaration can be omitted because the type is already known.
   */
  @Override
  public boolean registerTypeDeclaration(CComplexTypeDeclaration declaration) {
    CComplexType type = declaration.getType();

    if (type.getName().isEmpty()) {
      // This is an unnamed type like "enum { e }".
      // We ignore it.
      return true;
    }

    String name = type.getQualifiedName();

    if (types.containsKey(name)) {
      CComplexTypeDeclaration oldDeclaration = types.get(name);

      CComplexType oldType = oldDeclaration.getType();

      if (type instanceof CElaboratedType) {
        // the current declaration just re-declares an existing type
        return false;
      }

      if (oldType.getClass() == type.getClass()) {
        // two CCompositeTypes or two CEnumTypes
        // declaring struct twice is not allowed, even with equal signatures

        throw new CFAGenerationRuntimeException("Redeclaring " + name
            + " in line " + declaration.getFileLocation().getStartingLineNumber()
            + ", originally declared in line " + oldDeclaration.getFileLocation().getStartingLineNumber());
      }

      assert oldType instanceof CElaboratedType
          && !(type instanceof CElaboratedType);

      // We now have a real declaration for a type for which we have seen a forward declaration
      // We set a reference to the full type in the old type
      // and update the types map with the full type.
      ((CElaboratedType)oldType).setRealType(type);
    }

    types.put(name, declaration);
    return true;
  }

  /**
   * Register a typedef.
   *
   * @return True if the type actually needs to be declared, False if the declaration can be omitted because the type is already known.
   */
  public boolean registerTypeDeclaration(CTypeDefDeclaration declaration) {
    String name = declaration.getName();

    if (typedefs.containsKey(name)) {
      CTypeDefDeclaration oldDeclaration = typedefs.get(name);
      CType type = declaration.getType();

      CType oldType = oldDeclaration.getType();

      if (!type.getCanonicalType().equals(oldType.getCanonicalType())) {
        throw new CFAGenerationRuntimeException("Redeclaring " + name
            + " in line " + declaration.getFileLocation().getStartingLineNumber()
            + " with type " + type.toASTString("")
            + ", originally declared in line " + oldDeclaration.getFileLocation().getStartingLineNumber()
            + " with type " + oldType.toASTString(""));
      }
      // redundant typedef, ignore it
      return false;
    }
    typedefs.put(name, declaration);
    return true;
  }

  public ImmutableMap<String, CFunctionDeclaration> getFunctions() {
    return ImmutableMap.copyOf(functions);
  }

  public ImmutableMap<String, CComplexTypeDeclaration> getTypes() {
    return ImmutableMap.copyOf(types);
  }

  public ImmutableMap<String, CSimpleDeclaration> getGlobalVars() {
    return ImmutableMap.copyOf(globalVars);
  }

  public ImmutableMap<String, CTypeDefDeclaration> getTypeDefs() {
    return ImmutableMap.copyOf(typedefs);
  }

  public Set<String> getRenamedTypes() {
    return ImmutableSet.copyOf(alreayTakenTypeNames);
  }

  @Override
  public String toString() {
    return "Functions: " + Joiner.on(' ').join(functions.keySet());
  }
}