/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CParser.FileContentToParse;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.CParserException;

@RunWith(Parameterized.class)
public class CParserLocationTest {

  @Parameters(name = "{0}")
  public static List<Object[]> testcases() {
    return ImmutableList.of(
        new Object[] {"test.c", "./test.c"},
        new Object[] {"./test.c", "./test.c"},
        new Object[] {"dir/test.c", "dir/test.c"},
        new Object[] {"./dir/test.c", "./dir/test.c"},
        new Object[] {"/dir/test.c", "/dir/test.c"});
  }

  @Parameter(0)
  public String fileName;

  @Parameter(1)
  public String expectedFileName;

  private CParser parser;

  @Before
  public void createParser() {
    parser =
        CParser.Factory.getParser(
            LogManager.createTestLogManager(),
            CParser.Factory.getDefaultOptions(),
            MachineModel.LINUX32);
  }

  @Test
  public void singleFileTest() throws CParserException {
    String code = "void main() { }";
    ParseResult result = parser.parseString(fileName, code);
    FileLocation mainLoc = result.getFunctions().get("main").getFileLocation();

    assertThat(mainLoc.getFileName()).isEqualTo(expectedFileName);
    assertThat(mainLoc.getNiceFileName()).isEmpty();
    assertThat(mainLoc.getStartingLineNumber()).isEqualTo(1);
    assertThat(mainLoc.getEndingLineNumber()).isEqualTo(1);
    assertThat(mainLoc.getStartingLineInOrigin()).isEqualTo(1);
    assertThat(mainLoc.getEndingLineInOrigin()).isEqualTo(1);
  }

  @Test
  public void singleFileTest_lineDirectiveIgnored()
      throws CParserException, InvalidConfigurationException {
    parser =
        new CParserWithLocationMapper(
            Configuration.defaultConfiguration(), LogManager.createTestLogManager(), parser, false);
    String code = "#line 5 \"foo.c\"\nvoid main() { }";
    ParseResult result = parser.parseString(fileName, code);
    FileLocation mainLoc = result.getFunctions().get("main").getFileLocation();

    assertThat(mainLoc.getFileName()).isEqualTo(expectedFileName);
    assertThat(mainLoc.getNiceFileName()).isEmpty();
    assertThat(mainLoc.getStartingLineNumber()).isEqualTo(2);
    assertThat(mainLoc.getEndingLineNumber()).isEqualTo(2);
    assertThat(mainLoc.getStartingLineInOrigin()).isEqualTo(2);
    assertThat(mainLoc.getEndingLineInOrigin()).isEqualTo(2);
  }

  @Test
  public void singleFileTest_lineDirective()
      throws CParserException, InvalidConfigurationException {
    parser =
        new CParserWithLocationMapper(
            Configuration.defaultConfiguration(), LogManager.createTestLogManager(), parser, true);
    String code = "#line 5\nvoid main() { }";
    ParseResult result = parser.parseString(fileName, code);
    FileLocation mainLoc = result.getFunctions().get("main").getFileLocation();

    assertThat(mainLoc.getFileName()).isEqualTo(expectedFileName);
    assertThat(mainLoc.getNiceFileName()).isEmpty();
    assertThat(mainLoc.getStartingLineNumber()).isEqualTo(2);
    assertThat(mainLoc.getEndingLineNumber()).isEqualTo(2);
    assertThat(mainLoc.getStartingLineInOrigin()).isEqualTo(5);
    assertThat(mainLoc.getEndingLineInOrigin()).isEqualTo(5);
  }

  @Test
  public void singleFileTest_lineDirectiveWithFilename()
      throws CParserException, InvalidConfigurationException {
    parser =
        new CParserWithLocationMapper(
            Configuration.defaultConfiguration(), LogManager.createTestLogManager(), parser, true);
    String code = "#line 5 \"foo.c\"\nvoid main() { }";
    ParseResult result = parser.parseString(fileName, code);
    FileLocation mainLoc = result.getFunctions().get("main").getFileLocation();

    assertThat(mainLoc.getFileName()).isEqualTo("foo.c");
    assertThat(mainLoc.getNiceFileName()).isEqualTo("foo.c");
    assertThat(mainLoc.getStartingLineNumber()).isEqualTo(2);
    assertThat(mainLoc.getEndingLineNumber()).isEqualTo(2);
    assertThat(mainLoc.getStartingLineInOrigin()).isEqualTo(5);
    assertThat(mainLoc.getEndingLineInOrigin()).isEqualTo(5);
  }

  @Test
  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public void multiFileTest() throws CParserException {
    String mainCode = "void main() { }";
    FileContentToParse main = new FileContentToParse(fileName, mainCode);

    String additionalCode = "void foo() { }";
    String additionalFileName = fileName.replace("test", "additional");
    String expectedAdditionalFileName = expectedFileName.replace("test", "additional");
    FileContentToParse additional = new FileContentToParse(additionalFileName, additionalCode);
    ParseResult result =
        parser.parseString(ImmutableList.of(main, additional), new CSourceOriginMapping());

    FileLocation mainLoc = result.getFunctions().get("main").getFileLocation();
    assertThat(mainLoc.getFileName()).isEqualTo(expectedFileName);
    assertThat(mainLoc.getNiceFileName())
        .isEqualTo(Paths.get(expectedFileName).getFileName().toString());
    assertThat(mainLoc.getStartingLineNumber()).isEqualTo(1);
    assertThat(mainLoc.getEndingLineNumber()).isEqualTo(1);
    assertThat(mainLoc.getStartingLineInOrigin()).isEqualTo(1);
    assertThat(mainLoc.getEndingLineInOrigin()).isEqualTo(1);

    FileLocation additionalLoc = result.getFunctions().get("foo").getFileLocation();
    assertThat(additionalLoc.getFileName()).isEqualTo(expectedAdditionalFileName);
    assertThat(additionalLoc.getNiceFileName())
        .isEqualTo(Paths.get(expectedAdditionalFileName).getFileName().toString());
    assertThat(additionalLoc.getStartingLineNumber()).isEqualTo(1);
    assertThat(additionalLoc.getEndingLineNumber()).isEqualTo(1);
    assertThat(additionalLoc.getStartingLineInOrigin()).isEqualTo(1);
    assertThat(additionalLoc.getEndingLineInOrigin()).isEqualTo(1);
  }
}
