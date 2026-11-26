// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.antlr;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibLogic;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSimpleParsingDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSmtFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSortDeclaration;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

public abstract class SvLibScope {

  protected final ImmutableSet.Builder<SmtLibLogic> logics;

  protected final ImmutableMap.Builder<String, SvLibSortDeclaration> sortDeclarations;

  protected final Map<String, SvLibSmtFunctionDeclaration> functionDeclarations;

  protected SvLibScope(
      ImmutableSet.Builder<SmtLibLogic> pLogics,
      ImmutableMap.Builder<String, SvLibSortDeclaration> pSortDeclarations,
      Map<String, SvLibSmtFunctionDeclaration> pFunctionDeclarations) {
    logics = pLogics;
    sortDeclarations = pSortDeclarations;
    functionDeclarations = pFunctionDeclarations;
  }

  abstract SvLibScope copy();

  abstract void enterProcedure(List<SvLibParsingParameterDeclaration> pParameters);

  abstract void leaveProcedure();

  public abstract SvLibSimpleParsingDeclaration getVariable(String pText);

  public abstract SvLibSimpleParsingDeclaration getVariableForQualifiedName(String pQualifiedName);

  /** Is a variable with the given qualified name declared in this scope? */
  public abstract boolean hasVariableForQualifiedName(String pQualifiedName);

  /** Is a variable with the given name declared in this scope? */
  public abstract boolean hasVariable(String pName);

  public abstract void addVariable(SvLibParsingVariableDeclaration pDeclaration);

  abstract void addProcedureDeclaration(SvLibProcedureDeclaration pDeclaration);

  abstract SvLibProcedureDeclaration getProcedureDeclaration(String pName);

  /** Is a procedure with the given name declared in this scope? */
  abstract boolean hasProcedureDeclaration(String pName);

  void addLogic(SmtLibLogic pLogic) {
    logics.add(pLogic);
  }

  Set<SmtLibLogic> getLogics() {
    return logics.build();
  }

  void addSortDeclaration(SvLibSortDeclaration pDeclaration) {
    sortDeclarations.put(pDeclaration.getName(), pDeclaration);
  }

  /**
   * Get the SvLibType for the given name. First, it tries to parse it as a built-in type. If that
   * fails, it looks for a sort declaration with the given name.
   *
   * @param pName the name of the type
   * @return the SvLibType
   */
  SvLibType getTypeForName(String pName) {
    Optional<SvLibType> builtInType = SvLibType.fromString(pName);
    if (builtInType.isPresent()) {
      return builtInType.orElseThrow();
    }

    return Objects.requireNonNull(
            sortDeclarations.buildOrThrow().get(pName),
            "Sort declaration '" + pName + "' not found.")
        .getType();
  }

  public void addFunctionDeclaration(SvLibSmtFunctionDeclaration pDeclaration) {
    SvLibSmtFunctionDeclaration declared =
        functionDeclarations.putIfAbsent(pDeclaration.getName(), pDeclaration);
    // The same function is declared again whenever it is used again, which is fine, but two
    // functions of one name with different types would give the terms of one of them the type of
    // the other.
    checkArgument(
        declared == null || declared.getType().equals(pDeclaration.getType()),
        "The function %s is declared with the type %s and with the type %s.",
        pDeclaration.getName(),
        declared == null ? null : declared.getType(),
        pDeclaration.getType());
  }

  /** Is a function with the given name declared, i.e. is it an uninterpreted function? */
  public boolean hasFunctionDeclaration(String pName) {
    return functionDeclarations.containsKey(pName);
  }

  public SvLibSmtFunctionDeclaration getFunctionDeclaration(String pName) {
    return Objects.requireNonNull(
        functionDeclarations.get(pName), "Function declaration '" + pName + "' not found.");
  }

  /** All functions that are declared in this scope, in the order in which they were declared. */
  public ImmutableList<SvLibSmtFunctionDeclaration> getFunctionDeclarations() {
    return ImmutableList.copyOf(functionDeclarations.values());
  }
}
