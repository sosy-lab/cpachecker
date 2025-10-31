// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3SetOptionCommand implements SMTLibCommand, K3Command {
  @Serial private static final long serialVersionUID = 5643151014736552178L;

  private final String option;
  private final String value;
  private final FileLocation fileLocation;

  public K3SetOptionCommand(String pOption, String pValue, FileLocation pFileLocation) {
    option = pOption;
    value = pValue;
    fileLocation = pFileLocation;
  }

  public String getOption() {
    return option;
  }

  public String getValue() {
    return value;
  }

  public FileLocation getFileLocation() {
    return fileLocation;
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
}
