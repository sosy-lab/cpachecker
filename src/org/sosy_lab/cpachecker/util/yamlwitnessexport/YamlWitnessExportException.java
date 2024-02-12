// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

public class YamlWitnessExportException extends Exception {
  private static final long serialVersionUID = -5647551194742587246L;

  public YamlWitnessExportException(String pReason) {
    super(pReason);
  }
}
