// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.exception;

/** Exception class for inconsistent SMGs. */
public class SMGInconsistencyException extends IllegalStateException {

  private static final long serialVersionUID = 3019969078458990250L;

  public SMGInconsistencyException(String msg) {
    super(msg);
  }
}
