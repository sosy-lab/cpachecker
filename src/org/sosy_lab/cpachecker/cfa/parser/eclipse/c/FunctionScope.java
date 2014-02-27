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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import static com.google.common.base.Preconditions.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;


/**
 * Implementation of {@link Scope} for the local scope inside functions.
 * Only variables can be declared.
 * Provides the mechanism to have nested scopes (i.e., inside {} blocks).
 */
class FunctionScope implements Scope {

  private final Map<String, CFunctionDeclaration> functions = new HashMap<>();
  private final Deque<Map<String, CComplexTypeDeclaration>> typesStack = new ArrayDeque<>();
  private final Map<String, CTypeDefDeclaration> typedefs = new HashMap<>();
  private final Deque<Map<String, CVariableDeclaration>> labelsStack = new ArrayDeque<>();
  private final Deque<Map<String, CLabelNode>> labelsNodeStack = new ArrayDeque<>();
  private final Deque<Map<String, CSimpleDeclaration>> varsStack = new ArrayDeque<>();
  private final Deque<Map<String, CSimpleDeclaration>> varsList = new ArrayDeque<>();


  private String currentFunctionName = null;
  private String currentFile = null;

  public FunctionScope(ImmutableMap<String, CFunctionDeclaration> pFunctions,
      ImmutableMap<String, CComplexTypeDeclaration> pTypes,
      ImmutableMap<String, CTypeDefDeclaration> pTypedefs,
      ImmutableMap<String, CSimpleDeclaration> pGlobalVars,
      String currentFile) {

    functions.putAll(pFunctions);
    typesStack.addLast(pTypes);
    typedefs.putAll(pTypedefs);
    varsStack.push(pGlobalVars);
    varsList.push(pGlobalVars);
    this.currentFile = currentFile;

    enterBlock();
  }

  public FunctionScope() {
    this(ImmutableMap.<String, CFunctionDeclaration>of(),
         ImmutableMap.<String, CComplexTypeDeclaration>of(),
         ImmutableMap.<String, CTypeDefDeclaration>of(),
         ImmutableMap.<String, CSimpleDeclaration>of(),
         "");
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
    typesStack.addLast(new HashMap<String, CComplexTypeDeclaration>());
    labelsStack.addLast(new HashMap<String, CVariableDeclaration>());
    labelsNodeStack.addLast(new HashMap<String, CLabelNode>());
    varsStack.addLast(new HashMap<String, CSimpleDeclaration>());
    varsList.addLast(varsStack.getLast());
  }

  public void leaveBlock() {
    checkState(varsStack.size() > 2);
    varsStack.removeLast();
    typesStack.removeLast();
    labelsStack.removeLast();
    labelsNodeStack.removeLast();
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
    checkNotNull(name);

    Iterator<Map<String, CComplexTypeDeclaration>> it = typesStack.descendingIterator();
    while (it.hasNext()) {
      Map<String, CComplexTypeDeclaration> types = it.next();

      CComplexTypeDeclaration declaration = types.get(getRenamedTypeName(name));
      if (declaration != null) {
        return declaration.getType();
      } else {
        declaration = types.get(name);
        if (declaration != null) {
          return declaration.getType();
        }
      }
    }
    return null;
  }

  /**
   * Returns the name for the type as it would be if it is renamed.
   */
  private String getRenamedTypeName(String type) {
    return type + "__" + currentFile;
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
  public String createScopedNameOf(String pName) {
    return createQualifiedName(currentFunctionName, pName);
  }

  /**
   * Take a name and return a name that is unconditionally qualified
   * with the given function.
   */
  public static String createQualifiedName(String pFunction, String pName) {
    return (pFunction + "::" + pName).intern();
  }

  @Override
  public void registerDeclaration(CSimpleDeclaration declaration) {
    assert declaration instanceof CVariableDeclaration
        || declaration instanceof CEnumerator
        || declaration instanceof CParameterDeclaration
        : "Tried to register a declaration which does not define a name in the standard namespace: " + declaration;
    assert  !(declaration.getType() instanceof CFunctionTypeWithNames);

    String name = declaration.getOrigName();
    assert name != null;

    Map<String, CSimpleDeclaration> vars = varsStack.getLast();

    // multiple declarations of the same variable are disallowed
    if (vars.containsKey(name)) {
      throw new CFAGenerationRuntimeException("Variable " + name + " already declared", declaration);
    }

    vars.put(name, declaration);
  }

  @Override
  public boolean registerTypeDeclaration(CComplexTypeDeclaration declaration) {
    checkArgument(declaration.getName() == null);

    String typeName = declaration.getType().getQualifiedName();

    if (lookupType(typeName) != null) {
      throw new CFAGenerationRuntimeException("Shadowing types are currently not supported", declaration);
    }

    typesStack.peekLast().put(typeName, declaration);
    return true;
  }

  public CVariableDeclaration lookupLocalLabel(String name) {
    checkNotNull(name);

    Iterator<Map<String, CVariableDeclaration>> it = labelsStack.descendingIterator();
    while (it.hasNext()) {
      Map<String, CVariableDeclaration> labels = it.next();

      CVariableDeclaration label = labels.get(name);
      if (label != null) {
        return label;
      }
    }
    return null;
  }

  public void registerLocalLabel(CVariableDeclaration label) {
    checkNotNull(label.getName());

    String labelName = label.getOrigName();

    if(lookupLocalLabel(labelName) != null) {
      throw new CFAGenerationRuntimeException("Label " + labelName + " already in use");
    }

    labelsStack.peekLast().put(labelName, label);
  }

  public void addLabelCFANode(CLabelNode node) {
    labelsNodeStack.peekLast().put(node.getLabel(), node);
  }

  public CLabelNode lookupLocalLabelNode(String name) {
    Iterator<Map<String, CLabelNode>> it = labelsNodeStack.descendingIterator();
    while(it.hasNext()) {
      Map<String, CLabelNode> nodes = it.next();

      CLabelNode node = nodes.get(name);
      if (node != null) {
        return node;
      }
    }
    return null;
  }

  public void registerLocalFunction(CFunctionDeclaration function) {
    functions.put(function.getName(), function);
  }

  public String getCurrentFunctionName() {
    return currentFunctionName;
  }

  @Override
  public String toString() {
    return "Functions: " + Joiner.on(' ').join(functions.keySet());
  }
}
