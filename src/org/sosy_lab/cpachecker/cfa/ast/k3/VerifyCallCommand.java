// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class VerifyCallCommand implements K3Command {

  @Serial private static final long serialVersionUID = 3913861771262171193L;
  private final K3ProcedureDeclaration procedureDeclaration;
  private final List<K3Term> terms;
  private final FileLocation fileLocation;

  public VerifyCallCommand(
      K3ProcedureDeclaration pProcedureDeclaration,
      List<K3Term> pTerms,
      FileLocation pFileLocation) {
    procedureDeclaration = pProcedureDeclaration;
    terms = pTerms;
    fileLocation = pFileLocation;
  }

  public K3ProcedureDeclaration getProcedureDeclaration() {
    return procedureDeclaration;
  }

  public List<K3Term> getTerms() {
    return terms;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof VerifyCallCommand other
        && procedureDeclaration.equals(other.procedureDeclaration)
        && terms.equals(other.terms);
  }

  @Override
  public int hashCode() {
    return 31 * procedureDeclaration.hashCode() + terms.hashCode();
  }

  public FileLocation getFileLocation() {
    return fileLocation;
  }
}
