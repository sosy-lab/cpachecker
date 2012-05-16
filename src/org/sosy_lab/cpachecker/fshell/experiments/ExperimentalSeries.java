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
package org.sosy_lab.cpachecker.fshell.experiments;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.sosy_lab.common.TimeAccumulator;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.fshell.FShell3Result;
import org.sosy_lab.cpachecker.fshell.Main;

import com.google.common.base.Preconditions;

public abstract class ExperimentalSeries {

  private static Experiment mExperiment = null;

  @BeforeClass
  public static void createLogFile() {
    if (mExperiment != null) {
      throw new RuntimeException();
    }

    SimpleDateFormat lDateFormat = new SimpleDateFormat("'log.test_locks.'yyyy-MM-dd'.'HH-mm-ss'.csv'");
    String lFileName = "test" + File.separator + "output" + File.separator + lDateFormat.format(new Date());

    mExperiment = new Experiment(lFileName);
  }

  @AfterClass
  public static void closeLogFile() {
    mExperiment.close();

    mExperiment = null;
  }

  public FShell3Result execute(String[] pArguments) throws IOException, InvalidConfigurationException {
    Preconditions.checkNotNull(pArguments);
    //Preconditions.checkArgument(pArguments.length == 3 || pArguments.length == 4);

    TimeAccumulator lTime = new TimeAccumulator();

    lTime.proceed();

    FShell3Result lResult = Main.run(pArguments);

    lTime.pause();

    boolean lCilPreprocessing = false;

    for (int lIndex = 3; lIndex < pArguments.length; lIndex++) {
      String lOption = pArguments[lIndex].trim();

      if (lOption.equals("--withoutCilPreprocessing")) {
        lCilPreprocessing = true;
      }
    }

    mExperiment.addExperiment(pArguments[0], pArguments[1], pArguments[2], lCilPreprocessing, lResult, lTime.getSeconds());

    return lResult;
  }

}
