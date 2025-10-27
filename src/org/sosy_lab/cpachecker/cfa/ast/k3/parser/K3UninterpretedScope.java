// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3.parser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.SmtLibLogic;

/**
 * This scope represents an uninterpreted scope in K3, which is used for building tags, for which we
 * don't know in which scope they will be interpreted in if at all.
 */
public class K3UninterpretedScope implements K3Scope {

  private PersistentMap<String, K3ProcedureDeclaration> procedureDeclarations;

  private Set<SmtLibLogic> logics = new HashSet<>();

  public K3UninterpretedScope() {
    procedureDeclarations = PathCopyingPersistentTreeMap.of();
  }

  private K3UninterpretedScope(
      PersistentMap<String, K3ProcedureDeclaration> pProcedureDeclarations) {
    procedureDeclarations = pProcedureDeclarations;
  }

  @Override
  public K3UninterpretedScope copy() {
    return new K3UninterpretedScope(procedureDeclarations);
  }

  @Override
  public void enterProcedure(List<K3ParameterDeclaration> pParameters) {}

  @Override
  public void leaveProcedure() {}

  @Override
  public K3SimpleDeclaration getVariable(String pText) {
    return K3VariableDeclaration.dummyVariableForName(pText);
  }

  @Override
  public void addVariable(K3VariableDeclaration pDeclaration) {}

  @Override
  public void addProcedureDeclaration(K3ProcedureDeclaration pDeclaration) {
    if (procedureDeclarations.containsKey(pDeclaration.getName())) {
      throw new IllegalArgumentException(
          "Procedure declaration with name "
              + pDeclaration.getName()
              + " already exists in the scope.");
    }
    procedureDeclarations = procedureDeclarations.putAndCopy(pDeclaration.getName(), pDeclaration);
  }

  @Override
  public K3ProcedureDeclaration getProcedureDeclaration(String pName) {
    return procedureDeclarations.get(pName);
  }

  @Override
  public void addLogic(SmtLibLogic pLogic) {
    logics.add(pLogic);
  }

  @Override
  public Set<SmtLibLogic> getLogics() {
    return logics;
  }
}
