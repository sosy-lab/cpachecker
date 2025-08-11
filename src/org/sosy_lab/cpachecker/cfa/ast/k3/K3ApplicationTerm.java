// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import com.google.common.base.Objects;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public abstract sealed class K3ApplicationTerm implements K3Term permits K3SymbolApplicationTerm {
  @Serial private static final long serialVersionUID = -2851628533508538234L;
  private final List<K3Term> terms;
  private final FileLocation fileLocation;

  protected K3ApplicationTerm(List<K3Term> pTerms, FileLocation pFileLocation) {
    terms = pTerms;
    fileLocation = pFileLocation;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "";
  }

  public List<K3Term> getTerms() {
    return terms;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof K3ApplicationTerm other && Objects.equal(terms, other.terms);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(terms);
  }
}
