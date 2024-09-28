// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

public interface SeqElement {
  // TODO remove this function and create custom string create functions
  //  this way we can specify stuff like addSemicolon, tab, newline, etc.
  //  OR: just override toString method
  //  one option is to include an int tab parameter
  String createString();
}
