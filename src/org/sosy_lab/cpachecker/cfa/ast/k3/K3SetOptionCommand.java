// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import com.google.common.base.Preconditions;
import java.io.Serial;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3SetOptionCommand implements SMTLibCommand, K3Command {
  @Serial private static final long serialVersionUID = 5643151014736552178L;

  private final String option;
  private final String value;
  private final FileLocation fileLocation;

  // Some constants to identify common options
  public static final String OPTION_WITNESS_OUTPUT_CHANNEL = ":witness-output-channel";

  public K3SetOptionCommand(String pOption, String pValue, FileLocation pFileLocation) {
    Preconditions.checkArgument(pOption != null);
    Preconditions.checkArgument(pValue != null);
    option = pOption;
    value = pValue;
    fileLocation = pFileLocation;
  }

  @NonNull
  public String getOption() {
    return option;
  }

  @NonNull
  public String getValue() {
    return value;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(set-option " + option + " " + value + ")";
  }

  @Override
  public int hashCode() {
    return option.hashCode() * 31 + value.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof K3SetOptionCommand other
        && option.equals(other.option)
        && value.equals(other.value);
  }

  @Override
  public <R, X extends Exception> R accept(K3CommandVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}
