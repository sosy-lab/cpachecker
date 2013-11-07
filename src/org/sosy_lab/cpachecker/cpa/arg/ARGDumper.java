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

import org.sosy_lab.common.Files;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.PostProcessor;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator;


@Options(prefix="cpa.arg")
public class ARGDumper implements PostProcessor {
  @Option(name="dumpARG", description="export final ART as C program")
  private boolean dumpARG = false;

  @Option(name="dumpFile", description="export final ART as .c file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File dumpFile = new File("ARG.c");

  private final ARGCPA cpa;

  public ARGDumper(Configuration config, ARGCPA cpa) throws InvalidConfigurationException {
    config.inject(this);
    this.cpa = cpa;
  }

  @Override
  public void postProcess(ReachedSet pReached) {
    if (dumpARG) {
      Timer t = new Timer();
      t.start();
      //generate source code out of given ART / ReachedSet
      ARGState artRoot = (ARGState) pReached.getFirstState();
      try {
        Files.writeFile(dumpFile, ARGToCTranslator.translateARG(artRoot, pReached));
        t.stop();
        System.out.println("ARGToC translation took " + t);
      } catch (IOException e) {
        cpa.getLogger().logUserException(Level.WARNING, e,
            "Could not dump ARG to file");
      }
    }
  }
}
