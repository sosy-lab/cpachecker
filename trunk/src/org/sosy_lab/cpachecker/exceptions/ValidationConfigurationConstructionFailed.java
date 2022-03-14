// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.exceptions;

public class ValidationConfigurationConstructionFailed extends Exception {

  private static final long serialVersionUID = -381269425082457805L;

  public ValidationConfigurationConstructionFailed() {}

  public ValidationConfigurationConstructionFailed(Throwable cause) {
    super(cause);
  }
}
