// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import java.io.Serial;

public final class K3ProcedureDefinitionCommand implements K3Command {

  @Serial private static final long serialVersionUID = -109324745114809038L;
  private final K3ProcedureDeclaration procedureDeclaration;
  private final K3Statement body;

  public K3ProcedureDefinitionCommand(
      K3ProcedureDeclaration pProcedureDeclaration, K3Statement pBody) {
    procedureDeclaration = pProcedureDeclaration;
    body = pBody;
  }

  public K3ProcedureDeclaration getProcedureDeclaration() {
    return procedureDeclaration;
  }

  public K3Statement getBody() {
    return body;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof K3ProcedureDefinitionCommand other
        && procedureDeclaration.equals(other.procedureDeclaration)
        && body.equals(other.body);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + procedureDeclaration.hashCode();
    result = prime * result + body.hashCode();
    return result;
  }
}
