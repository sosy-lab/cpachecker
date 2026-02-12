// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export;

/**
 * An enum for the different types of {@link CMultiSelectionStatementEncoding}, can be used e.g. in
 * options to define a specific encoding.
 */
public enum CMultiSelectionStatementEncoding {
  // NONE may be necessary when some other option defines that there is no multi control statement
  // at all in the exported C code
  NONE,
  BINARY_SEARCH_TREE,
  IF_ELSE_CHAIN,
  SWITCH_CASE;

  // TODO CONDITIONAL_GOTO (cf. Lazy-CSeq)

  public boolean isEnabled() {
    return !this.equals(NONE);
  }
}
