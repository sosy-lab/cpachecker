/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
 */
package LoopAcc;

import java.util.ArrayList;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;

/**
 * This class helps to abstract the loop code so that different cpa's or bounded model checker can
 * easily iterate over it. You have to enable the option, let it run with a cpa. You get a new file
 * with the changed code. After that you have to disable this function and let the cpa run with the
 * newly created file.
 */
@Options(prefix = "loopacc.loopabstractionheader")
public class LoopAbstractionHeader {

  @Option(
      secure = true,
      name = "shouldAbstract",
    values = {"naiv", "advanced", "none"},
      description = "AbstractLoops to be able to process them"
      )
  private String shouldAbstract = "none";

  @Option(
    secure = true,
    name = "pathForFile",
    description = "Use this option to specify a place to save the new c file with the abstracted loops, default is in the cpachecker folder -> abstractLoops"
  )
  private String pathForAbstractLoops = "/home//bensky//cpachecker//abstractLoops//";

  ArrayList<LoopData> loops;
  LoopAbstractionNaiv loopAbstractionN;
  LoopAbstractionAdvanced loopAbstractionAdv;

  /**
   * Constructor that enables the CPAchecker to rewrite the loops in the programs, you can choose
   * between the loops that can't be analyzed by a BMC, all the loops or none
   *
   * @param loopI LoopInformation object that includes all the info needed to abstract a loop
   * @param config config object that enables switching between the 3 modes, default is that none of
   *        the data will be rewritten
   * @throws InvalidConfigurationException
   */
  public LoopAbstractionHeader(
      LoopInformation loopI,
      Configuration config,
      LogManager logger)
      throws InvalidConfigurationException {
    config.inject(this);
    if (shouldAbstract.equals("naiv")) {
    getLoopsToBeAbstracted(loopI);
    loopAbstractionN = new LoopAbstractionNaiv();
    overwriteLoops(loopI, logger, pathForAbstractLoops);
  } else if (shouldAbstract.equals("advanced")) {
    getLoopsToBeAbstracted(loopI);
    loopAbstractionAdv = new LoopAbstractionAdvanced();
    overwriteLoops(loopI, logger, pathForAbstractLoops);
  }

  }

  private void overwriteLoops(LoopInformation loopI, LogManager logger, String pathForNewFile) {
    if (shouldAbstract.equals("naiv")) {
      loopAbstractionN.changeFileToAbstractFile(loopI, logger, pathForNewFile);
    } else if (shouldAbstract.equals("advanced")) {
      loopAbstractionAdv.changeFileToAbstractFile(loopI, logger, pathForNewFile);
    }
  }

  private void getLoopsToBeAbstracted(LoopInformation loopI) {
    loops = new ArrayList<>();
    for (LoopData lD : loopI.getLoopData()) {
      if (lD.getCanBeAccelerated()) {
        loops.add(lD);
      }
    }
  }

}
