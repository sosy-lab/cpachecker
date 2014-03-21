/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value.refiner.utils;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.StringBuildingLogHandler;

import com.google.common.collect.ImmutableMap;

/**
 *
 */
public class SympyTest {

  private static LogManager logger;

  /**
   * Init dummy logger.
   *
   * @throws InvalidConfigurationException
   */
  @BeforeClass
  public static void initalizeLogger() throws InvalidConfigurationException {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.value.ValueAnalysisCPA",
        "specification", "config/specification/default.spc",
        "cpa.value.variableBlacklist", "somethingElse"
        );
    Configuration config = Configuration.builder()
        .addConverter(FileOption.class, new FileTypeConverter(Configuration.defaultConfiguration()))
        .setOptions(prop).build();
    StringBuildingLogHandler stringLogHandler = new StringBuildingLogHandler();
    logger = new BasicLogManager(config, stringLogHandler);
  }

  @Test
  public void testSimpy0() {
    String exp = "(4 * a) + a + b + c + d";
    String correct = "5*a + b + c + d";
    String result = SympyHandler.simplifyExpression(exp, logger);
    System.out.println("Correct " + correct + ", is: " + result);
    assertEquals(correct, result);
  }

  @Test
  public void testSimpy1() {
    String exp = "(7 * b) + (3 * b)";
    String correct = "10*b";
    String result = SympyHandler.simplifyExpression(exp, logger);
    System.out.println("Correct " + correct + ", is: " + result);
    assertEquals(correct, result);
  }


}
