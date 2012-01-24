/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.fshell.experiments.systemc;

import junit.framework.Assert;

import org.junit.Test;
import org.sosy_lab.cpachecker.fshell.FShell3Result;
import org.sosy_lab.cpachecker.fshell.Main;
import org.sosy_lab.cpachecker.fshell.experiments.ExperimentalSeries;

public class SystemC040_BB2_Test extends ExperimentalSeries {

  @Test
  public void systemc_001() throws Exception {
    String lCFile = "transmitter.12.BUG.cil.c";

    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/systemc/" + lCFile,
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(80, lResult.getNumberOfTestGoals());
    Assert.assertEquals(74, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

}
