// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import org.junit.Test;

public class SMGCPATransferRelationTest {

  /*
   * Declaration tests:
   *   declare variable without value and use afterwards
   *   declare with simple value and use
   *   declare stack array and use
   *   declare stack array with value
   *   declare stack struct
   *   declare stack struct with value
   *   declare String with array of chars
   *   declare String with String ("")
   *   declare String as char * and array of chars
   *   declare String as char * with String
   *
   * Function usage:
   *   malloc
   *   free
   *   ...
   */

  @Test
  public void dummyTestBecauseTheCIComplains() {}
}
