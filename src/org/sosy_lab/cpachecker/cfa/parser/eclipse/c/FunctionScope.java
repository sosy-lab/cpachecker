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

import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.util.VariableClassificationBuilder;

import com.google.common.base.Joiner;
import java.util.Optional;
import com.google.common.collect.ImmutableMap;


/**
 * Implementation of {@link Scope} for the local scope inside functions.
 * Only variables can be declared.
 * Provides the mechanism to have nested scopes (i.e., inside {} blocks).
 */
class FunctionScope extends AbstractScope {

  private final Scope artificialScope;
  private final Map<String, CFunctionDeclaration> localFunctions = new HashMap<>();
  private final Map<String, CFunctionDeclaration> globalFunctions;
  private final Deque<Map<String, CComplexTypeDeclaration>> typesStack = new ArrayDeque<>();
  private final Map<String, CTypeDefDeclaration> typedefs;
  private final Deque<Map<String, CVariableDeclaration>> labelsStack = new ArrayDeque<>();
  private final Deque<Map<String, CLabelNode>> labelsNodeStack = new ArrayDeque<>();
  private final Deque<Map<String, CSimpleDeclaration>> varsStack = new ArrayDeque<>();
  private final Deque<Map<String, CSimpleDeclaration>> varsStackWitNewNames = new ArrayDeque<>();
  private final Deque<Map<String, CSimpleDeclaration>> varsList = new ArrayDeque<>();
  private final Deque<Map<String, CSimpleDeclaration>> varsListWithNewNames = new ArrayDeque<>();


  private CFunctionDeclaration currentFunction = null;
  private Optional<CVariableDeclaration> returnVariable = null;

  public FunctionScope(
      ImmutableMap<String, CFunctionDeclaration> pFunctions,
      ImmutableMap<String, CComplexTypeDeclaration> pTypes,
      ImmutableMap<String, CTypeDefDeclaration> pTypedefs,
      ImmutableMap<String, CSimpleDeclaration> pGlobalVars,
      String currentFile,
      Scope pArtificialScope) {
    super(currentFile);

    globalFunctions = pFunctions;
    typedefs = pTypedefs;

    typesStack.addLast(pTypes);
    varsStack.push(pGlobalVars);
    varsStack.push(pGlobalVars);
    varsList.push(pGlobalVars);
    varsListWithNewNames.push(pGlobalVars);

    this.artificialScope = pArtificialScope;

    enterBlock();
  }

  public FunctionScope() {
    this(
        ImmutableMap.<String, CFunctionDeclaration>of(),
        ImmutableMap.<String, CComplexTypeDeclaration>of(),
        ImmutableMap.<String, CTypeDefDeclaration>of(),
        ImmutableMap.<String, CSimpleDeclaration>of(),
        "",
        CProgramScope.empty());
  }

  @Override
  public boolean isGlobalScope() {
    return false;
  }

  public void enterFunction(CFunctionDeclaration pFuncDef) {
    checkState(currentFunction == null);
    currentFunction = checkNotNull(pFuncDef);
    checkArgument(globalFunctions.containsKey(getCurrentFunctionName()) || localFunctions.containsKey(getCurrentFunctionName()));

    if (currentFunction.getType().getReturnType().getCanonicalType() instanceof CVoidType) {
      returnVariable = Optional.empty();
    } else {
      @SuppressWarnings("deprecation") // As soon as this is the only usage of the deprecated constant, it should be inlined here
      String name = VariableClassificationBuilder.FUNCTION_RETURN_VARIABLE;
      returnVariable = Optional.of(
          new CVariableDeclaration(currentFunction.getFileLocation(), false,
            CStorageClass.AUTO, currentFunction.getType().getReturnType(),
            name, name, createScopedNameOf(name), null));
    }
  }

  public void enterBlock() {
    typesStack.addLast(new HashMap<>());
    labelsStack.addLast(new HashMap<>());
    labelsNodeStack.addLast(new HashMap<>());
    varsStack.addLast(new HashMap<>());
    varsStackWitNewNames.addLast(new HashMap<>());
    varsList.addLast(varsStack.getLast());
    varsListWithNewNames.addLast(varsStackWitNewNames.getLast());
  }

  public void leaveBlock() {
    checkState(varsStack.size() > 2);
    varsStack.removeLast();
    varsStackWitNewNames.removeLast();
    typesStack.removeLast();
    labelsStack.removeLast();
    labelsNodeStack.removeLast();
  }

  @Override
  public boolean variableNameInUse(String name) {
    checkNotNull(name);

    Iterator<Map<String, CSimpleDeclaration>> it = varsListWithNewNames.descendingIterator();
    while (it.hasNext()) {
      Map<String, CSimpleDeclaration> vars = it.next();

      if (vars.get(name) != null) {
        return true;
      }
    }
    return artificialScope.variableNameInUse(name);
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

    return artificialScope.lookupVariable(name);
  }

  @Override
  public CFunctionDeclaration lookupFunction(String name) {
    checkNotNull(name);

    // we look at first if the function is available in the local functions
    CFunctionDeclaration returnDecl = localFunctions.get(name);
    if (returnDecl != null) {
      return returnDecl;
    }

    returnDecl = globalFunctions.get(name);
    if (returnDecl != null) {
      return returnDecl;
    }

    return artificialScope.lookupFunction(name);
  }

  @Override
  public CComplexType lookupType(String name) {
    checkNotNull(name);

    Iterator<Map<String, CComplexTypeDeclaration>> it = typesStack.descendingIterator();
    while (it.hasNext()) {
      Map<String, CComplexTypeDeclaration> types = it.next();

      CComplexTypeDeclaration declaration = types.get(getFileSpecificTypeName(name));
      if (declaration != null) {
        return declaration.getType();
      } else {
        declaration = types.get(name);
        if (declaration != null) {
          return declaration.getType();
        }
      }
    }

    return artificialScope.lookupType(name);
  }

  @Override
  public CType lookupTypedef(final String name) {
    checkNotNull(name);

    final CTypeDefDeclaration declaration = typedefs.get(name);
    if (declaration != null) {
      return declaration.getType();
    }

    return artificialScope.lookupTypedef(name);
  }

  @Override
  public String createScopedNameOf(String pName) {
    if (!artificialScope.isGlobalScope()) {
      return artificialScope.createScopedNameOf(pName);
    }
    return createQualifiedName(getCurrentFunctionName(), pName);
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
    Map<String, CSimpleDeclaration> varsWithNewNames = varsStackWitNewNames.getLast();

    // multiple declarations of the same variable are disallowed
    if (vars.containsKey(name)) {
      throw new CFAGenerationRuntimeException("Variable " + name + " already declared", declaration);
    }

    vars.put(name, declaration);
    varsWithNewNames.put(declaration.getName(), declaration);
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

    if (containsLocalLabel(labelName)) {
      throw new CFAGenerationRuntimeException("Label " + labelName + " already in use");
    }

    labelsStack.peekLast().put(labelName, label);
  }

  public boolean containsLocalLabel(String labelName) {
    return labelsStack.peekLast().containsKey(labelName);
  }

  public boolean containsLabelCFANode(CLabelNode node) {
    return labelsNodeStack.peekLast().containsKey(node.getLabel());
  }

  public void addLabelCFANode(CLabelNode node) {
    labelsNodeStack.peekLast().put(node.getLabel(), node);
  }

  public CLabelNode lookupLocalLabelNode(String name) {
    Iterator<Map<String, CLabelNode>> it = labelsNodeStack.descendingIterator();
    while (it.hasNext()) {
      Map<String, CLabelNode> nodes = it.next();

      CLabelNode node = nodes.get(name);
      if (node != null) {
        return node;
      }
    }
    return null;
  }

  public void registerLocalFunction(CFunctionDeclaration function) {
    localFunctions.put(function.getName(), function);
  }

  public String getCurrentFunctionName() {
    checkState(currentFunction != null);
    return currentFunction.getOrigName();
  }

  public Optional<CVariableDeclaration> getReturnVariable() {
    checkState(returnVariable != null);
    return returnVariable;
  }

  @Override
  public String toString() {
    return "Functions: " + Joiner.on(' ').join(globalFunctions.keySet()) + Joiner.on(' ').join(localFunctions.keySet());
  }
}
