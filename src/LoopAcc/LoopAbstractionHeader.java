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

import java.util.List;
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
  private String shouldAbstract = "naiv";

  @Option(
    secure = true,
    name = "pathForFile",
    description = "Use this option to specify a place to save the new c file with the abstracted loops, default is in the cpachecker folder -> abstractLoops"
  )
  private String pathForAbstractLoops = "../abstractLoops/";

  @Option(
    secure = true,
    name = "onlyAccelerableLoops",
    description = "Change this option only if you want all of the loops to be abstracted.")
  private boolean accLoops = true;

  List<LoopData> loops;
  LoopAbstraction loopAbstraction;

  /**
   * Constructor that enables the CPAchecker to rewrite the loops in the programs, you can choose
   * between the loops that can't be analyzed by a BMC, all the loops or none
   *
   * @param loopI LoopInformation object that includes all the info needed to abstract a loop
   * @param config config object that enables switching between the 3 modes, default is that none of
   *        the data will be rewritten
   * @throws InvalidConfigurationException throws an exception if the configuration doesn't match
   *         the supported options
   */
  public LoopAbstractionHeader(
      LoopInformation loopI,
      boolean automate,
      Configuration config,
      LogManager logger)
      throws InvalidConfigurationException {
    config.inject(this);

    pathForAbstractLoops = loopI.getCFA().getFileNames().get(0).toString();

    loopAbstraction = new LoopAbstraction();
    loopAbstraction
        .changeFileToAbstractFile(
        loopI,
        logger,
        pathForAbstractLoops,
        shouldAbstract,
        automate,
        accLoops);

  }
}
