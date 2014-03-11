/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.testgen.old;

import java.io.IOException;
import java.util.Collections;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.StringBuildingLogHandler;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.ParserException;

import com.google.common.collect.ImmutableMap;


public class TestgenTestHelper {

  public static  Pair<UnmodifiableReachedSet, Triple<CFA,Configuration,LogManager>> createReachedSetFromFile(String pSourceCodeFilePath)
      throws InvalidConfigurationException {

    ImmutableMap<String, String> prop =
        new ImmutableMap.Builder<String, String>()
            .put("cpa", "cpa.arg.ARGCPA")
            .put("ARGCPA.cpa", "cpa.composite.CompositeCPA")
            .put("CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.explicit.ExplicitCPA")
            .put("specification", "config/specification/default.spc")
            .put("cpa.composite.precAdjust", "COMPONENT")
            .put("log.consoleLevel", "FINER")
            .build();

    Configuration config = Configuration.builder()
        .addConverter(FileOption.class, new FileTypeConverter(Configuration.defaultConfiguration()))
        .setOptions(prop).build();
    StringBuildingLogHandler stringLogHandler = new StringBuildingLogHandler();
    LogManager logger = new BasicLogManager(config, stringLogHandler);
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.create();
    CPAchecker cpaChecker = new CPAchecker(config, logger, shutdownNotifier);
    CFACreator cfaCreator = new CFACreator(config, logger, shutdownNotifier);
    CFA cfa;
    CPAcheckerResult results;
    try {
      cfa = cfaCreator.parseFileAndCreateCFA(Collections.singletonList(pSourceCodeFilePath));
      results = cpaChecker.run(pSourceCodeFilePath);
    } catch (IOException | ParserException | InterruptedException e) {
      e.printStackTrace();
      return Pair.of(null, null) ;
    }
    return Pair.of(results.getReached(), Triple.of(cfa, config, logger)) ;

  }

}
