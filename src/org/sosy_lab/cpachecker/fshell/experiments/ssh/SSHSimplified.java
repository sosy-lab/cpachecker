/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.fshell.experiments.ssh;

import junit.framework.Assert;

import org.junit.Test;
import org.sosy_lab.cpachecker.fshell.FShell3Result;
import org.sosy_lab.cpachecker.fshell.Main;
import org.sosy_lab.cpachecker.fshell.PredefinedCoverageCriteria;
import org.sosy_lab.cpachecker.fshell.experiments.ExperimentalSeries;

public class SSHSimplified extends ExperimentalSeries {

  @Test
  public void ssh_001() throws Exception {
    String lCFile = "s3_clnt_1_BUG.2.cil.c";

    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/" + lCFile,
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(80, lResult.getNumberOfTestGoals());
    Assert.assertEquals(74, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void ssh_002() throws Exception {
    String lCFile = "s3_clnt_1.cil.c";

    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/" + lCFile,
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(80, lResult.getNumberOfTestGoals());
    Assert.assertEquals(74, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void ssh_003() throws Exception {
    String lCFile = "s3_clnt_2_BUG.cil.c";

    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/" + lCFile,
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(80, lResult.getNumberOfTestGoals());
    Assert.assertEquals(74, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void ssh_004() throws Exception {
    String lCFile = "s3_clnt_2.cil.c";

    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/" + lCFile,
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(80, lResult.getNumberOfTestGoals());
    Assert.assertEquals(74, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void ssh_005() throws Exception {
    String lCFile = "s3_clnt_3_BUG.cil.c";

    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/" + lCFile,
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(80, lResult.getNumberOfTestGoals());
    Assert.assertEquals(74, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void ssh_006() throws Exception {
    String lCFile = "s3_clnt_3.cil_org.c";

    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/" + lCFile,
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(80, lResult.getNumberOfTestGoals());
    Assert.assertEquals(74, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void ssh_007() throws Exception {
    String lCFile = "s3_clnt_3.cil.c";

    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/" + lCFile,
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(80, lResult.getNumberOfTestGoals());
    Assert.assertEquals(74, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void ssh_008() throws Exception {
    String lCFile = "s3_clnt_4_BUG.cil.c";

    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/" + lCFile,
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(80, lResult.getNumberOfTestGoals());
    Assert.assertEquals(74, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void ssh_009() throws Exception {
    String lCFile = "s3_clnt_4.cil.c";

    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/" + lCFile,
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(80, lResult.getNumberOfTestGoals());
    Assert.assertEquals(74, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void ssh_010() throws Exception {
    String lCFile = "s3_srvr_1_BUG.cil.c";

    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/" + lCFile,
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(80, lResult.getNumberOfTestGoals());
    Assert.assertEquals(74, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void ssh_011() throws Exception {
    String lCFile = "s3_srvr_1.cil.c";

    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/" + lCFile,
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(80, lResult.getNumberOfTestGoals());
    Assert.assertEquals(74, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void ssh_012() throws Exception {
    String lCFile = "s3_srvr_2_BUG.cil.c";

    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/" + lCFile,
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(80, lResult.getNumberOfTestGoals());
    Assert.assertEquals(74, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void ssh_013() throws Exception {
    String lCFile = "s3_srvr_2.cil.c";

    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/" + lCFile,
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(80, lResult.getNumberOfTestGoals());
    Assert.assertEquals(74, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void ssh_014() throws Exception {
    String lCFile = "s3_srvr_3.cil.c";

    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/" + lCFile,
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(80, lResult.getNumberOfTestGoals());
    Assert.assertEquals(74, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void ssh_015() throws Exception {
    String lCFile = "s3_srvr_4.cil.c";

    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/" + lCFile,
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(80, lResult.getNumberOfTestGoals());
    Assert.assertEquals(74, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void ssh_016() throws Exception {
    String lCFile = "s3_srvr_6.cil.c";

    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/" + lCFile,
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(80, lResult.getNumberOfTestGoals());
    Assert.assertEquals(74, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void ssh_017() throws Exception {
    String lCFile = "s3_srvr_7.cil.c";

    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/" + lCFile,
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(80, lResult.getNumberOfTestGoals());
    Assert.assertEquals(74, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void ssh_018() throws Exception {
    String lCFile = "s3_srvr_8.cil.c";

    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/ssh-simplified/" + lCFile,
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
