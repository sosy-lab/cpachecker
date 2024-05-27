// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.c;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

public final class CFunctionEntryNode extends FunctionEntryNode {

  private static final long serialVersionUID = -730687961628864953L;

  public CFunctionEntryNode(
      final FileLocation pFileLocation,
      final CFunctionDeclaration pFunctionDefinition,
      final @Nullable FunctionExitNode pExitNode,
      final Optional<CVariableDeclaration> pReturnVariable) {

    super(pFileLocation, pExitNode, pFunctionDefinition, pReturnVariable);
  }

  /**
   * Constructor for a function entry node for a C program with additional information about the
   * variables in scope at this node for the original program.
   *
   * @param pFileLocation the location in the source file
   * @param pExitNode the corresponding function exit node, or null if the function never returns
   * @param pFunctionDefinition the function definition
   * @param pReturnVariable the variable that stores the return value of the function, if it has one
   * @param pLocalInScopeVariablesForInputProgram the input variables of the function as given in
   *     the function declaration
   * @param pGlobalInScopeVariablesForInputProgram the global variables that are in scope at this
   *     node
   */
  public CFunctionEntryNode(
      final FileLocation pFileLocation,
      final CFunctionDeclaration pFunctionDefinition,
      final @Nullable FunctionExitNode pExitNode,
      final Optional<CVariableDeclaration> pReturnVariable,
      ImmutableSet<CSimpleDeclaration> pLocalInScopeVariablesForInputProgram,
      ImmutableSet<CSimpleDeclaration> pGlobalInScopeVariablesForInputProgram) {
    super(
        pFileLocation,
        pExitNode,
        pFunctionDefinition,
        pReturnVariable,
        pLocalInScopeVariablesForInputProgram,
        pGlobalInScopeVariablesForInputProgram);
  }

  @Override
  public CFunctionDeclaration getFunctionDefinition() {
    return (CFunctionDeclaration) super.getFunctionDefinition();
  }

  @Override
  public List<CParameterDeclaration> getFunctionParameters() {
    return getFunctionDefinition().getParameters();
  }

  @SuppressWarnings("unchecked") // safe because Optional is covariant
  @Override
  public Optional<CVariableDeclaration> getReturnVariable() {
    return (Optional<CVariableDeclaration>) super.getReturnVariable();
  }
}
