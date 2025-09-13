// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3.parser;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VariableDeclaration;

public interface K3Scope {

  K3Scope copy();

  void enterProcedure(List<K3ParameterDeclaration> pParameters);

  void leaveProcedure();

  K3SimpleDeclaration getVariable(String pText);

  void addVariable(K3VariableDeclaration pDeclaration);

  void addProcedureDeclaration(K3ProcedureDeclaration pDeclaration);

  K3ProcedureDeclaration getProcedureDeclaration(String pName);
}
