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
package org.sosy_lab.cpachecker.cpa.arg;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.interfaces.PostProcessor;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator;


@Options(prefix="cpa.arg")
public class ARGDumper implements PostProcessor {
  @Option(secure=true, name="dumpARG", description="export final ARG as C program")
  private boolean dumpARG = false;

  @Option(secure=true, name = "addInclude", description = "whether or not add #include <stdio.h> in front of transformed program")
  private boolean addDefaultInclude = true;

  @Option(name="dumpFile", description="export final ARG as .c file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File dumpFile = new File("ARG.c");

  private final ARGCPA cpa;
  private final LogManager logger;

  public ARGDumper(Configuration config, ARGCPA cpa, LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this);
    this.cpa = cpa;
    logger = pLogger;
  }

  @Override
  public void postProcess(ReachedSet pReached) {
    if (dumpARG) {
      Timer t = new Timer();
      t.start();
      //generate source code out of given ARG / ReachedSet
      ARGState argRoot = (ARGState) pReached.getFirstState();
      try {
        Files.writeFile(Paths.get(dumpFile), ARGToCTranslator.translateARG(argRoot, pReached, addDefaultInclude, logger));
        t.stop();
        logger.log(Level.ALL, "ARGToC translation took ", t);
      } catch (IOException e) {
        cpa.getLogger().logUserException(Level.WARNING, e,
            "Could not dump ARG to file");
      }
    }
  }
}
