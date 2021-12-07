// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.domains;

/*
 * Enum for storing the domain type.
 * Used to assign every aspect to a domain.
 */
public enum DomainType {
  PREFFIX,
  SUFFIX,
  LENGTH,
  CONTAINS,
  STRING_SET,
  CHAR_SET;

  public boolean isLessOrEqual(DomainType obj) {
    return this == obj;
  }
}
