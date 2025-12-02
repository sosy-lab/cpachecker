// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.antlr;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibLogic;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSimpleParsingDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSmtFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSortDeclaration;

/**
 * This scope represents an uninterpreted scope in SV-LIB, which is used for building tags, for
 * which we don't know in which scope they will be interpreted in if at all.
 */
public class SvLibUninterpretedScope extends SvLibScope {

  private PersistentMap<String, SvLibProcedureDeclaration> procedureDeclarations;

  public SvLibUninterpretedScope() {
    super(new ImmutableSet.Builder<>(), new ImmutableMap.Builder<>(), new ImmutableMap.Builder<>());
    procedureDeclarations = PathCopyingPersistentTreeMap.of();
  }

  private SvLibUninterpretedScope(
      PersistentMap<String, SvLibProcedureDeclaration> pProcedureDeclarations,
      ImmutableSet.Builder<SmtLibLogic> pLogics,
      ImmutableMap.Builder<String, SvLibSortDeclaration> pSortDeclarations,
      ImmutableMap.Builder<String, SvLibSmtFunctionDeclaration> pFunctionDeclarations) {
    super(pLogics, pSortDeclarations, pFunctionDeclarations);
    procedureDeclarations = pProcedureDeclarations;
  }

  @Override
  public SvLibUninterpretedScope copy() {
    return new SvLibUninterpretedScope(
        procedureDeclarations, logics, sortDeclarations, functionDeclarations);
  }

  @Override
  public void enterProcedure(List<SvLibParsingParameterDeclaration> pParameters) {}

  @Override
  public void leaveProcedure() {}

  @Override
  public SvLibSimpleParsingDeclaration getVariable(String pText) {
    return SvLibParsingVariableDeclaration.dummyVariableForName(pText);
  }

  @Override
  public SvLibSimpleParsingDeclaration getVariableForQualifiedName(String pText) {
    return SvLibParsingVariableDeclaration.dummyVariableForName(pText);
  }

  @Override
  public void addVariable(SvLibParsingVariableDeclaration pDeclaration) {}

  @Override
  public void addProcedureDeclaration(SvLibProcedureDeclaration pDeclaration) {
    if (procedureDeclarations.containsKey(pDeclaration.getName())) {
      throw new IllegalArgumentException(
          "Procedure declaration with name "
              + pDeclaration.getName()
              + " already exists in the scope.");
    }
    procedureDeclarations = procedureDeclarations.putAndCopy(pDeclaration.getName(), pDeclaration);
  }

  @Override
  public SvLibProcedureDeclaration getProcedureDeclaration(String pName) {
    return procedureDeclarations.get(pName);
  }
}
