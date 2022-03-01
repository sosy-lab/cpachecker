// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
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
            MachineModel.LINUX32,
            ShutdownNotifier.createDummy());
  }

  @Test
  public void singleFileTest() throws CParserException, InterruptedException {
    String code = "void main() { }";
    ParseResult result = parser.parseString(Path.of(fileName), code);
    FileLocation mainLoc = result.getFunctions().get("main").getFileLocation();

    assertThat(mainLoc.getFileName()).isEqualTo(Path.of(expectedFileName));
    assertThat(mainLoc.getNiceFileName()).isEmpty();
    assertThat(mainLoc.getStartingLineNumber()).isEqualTo(1);
    assertThat(mainLoc.getEndingLineNumber()).isEqualTo(1);
    assertThat(mainLoc.getStartingLineInOrigin()).isEqualTo(1);
    assertThat(mainLoc.getEndingLineInOrigin()).isEqualTo(1);
  }

  @Test
  public void singleFileTest_lineDirectiveIgnored()
      throws CParserException, InvalidConfigurationException, InterruptedException {
    parser =
        new CParserWithLocationMapper(
            Configuration.defaultConfiguration(), LogManager.createTestLogManager(), parser, false);
    String code = "#line 5 \"foo.c\"\nvoid main() { }";
    ParseResult result = parser.parseString(Path.of(fileName), code);
    FileLocation mainLoc = result.getFunctions().get("main").getFileLocation();

    assertThat(mainLoc.getFileName()).isEqualTo(Path.of(expectedFileName));
    assertThat(mainLoc.getNiceFileName()).isEmpty();
    assertThat(mainLoc.getStartingLineNumber()).isEqualTo(2);
    assertThat(mainLoc.getEndingLineNumber()).isEqualTo(2);
    assertThat(mainLoc.getStartingLineInOrigin()).isEqualTo(2);
    assertThat(mainLoc.getEndingLineInOrigin()).isEqualTo(2);
  }

  @Test
  public void singleFileTest_lineDirective()
      throws CParserException, InvalidConfigurationException, InterruptedException {
    parser =
        new CParserWithLocationMapper(
            Configuration.defaultConfiguration(), LogManager.createTestLogManager(), parser, true);
    String code = "#line 5\nvoid main() { }";
    ParseResult result = parser.parseString(Path.of(fileName), code);
    FileLocation mainLoc = result.getFunctions().get("main").getFileLocation();

    assertThat(mainLoc.getFileName()).isEqualTo(Path.of(expectedFileName));
    assertThat(mainLoc.getNiceFileName()).isEmpty();
    assertThat(mainLoc.getStartingLineNumber()).isEqualTo(2);
    assertThat(mainLoc.getEndingLineNumber()).isEqualTo(2);
    assertThat(mainLoc.getStartingLineInOrigin()).isEqualTo(5);
    assertThat(mainLoc.getEndingLineInOrigin()).isEqualTo(5);
  }

  @Test
  public void singleFileTest_lineDirectiveWithFilename()
      throws CParserException, InvalidConfigurationException, InterruptedException {
    parser =
        new CParserWithLocationMapper(
            Configuration.defaultConfiguration(), LogManager.createTestLogManager(), parser, true);
    String code = "#line 5 \"foo.c\"\nvoid main() { }";
    ParseResult result = parser.parseString(Path.of(fileName), code);
    FileLocation mainLoc = result.getFunctions().get("main").getFileLocation();

    assertThat(mainLoc.getFileName()).isEqualTo(Path.of("foo.c"));
    assertThat(mainLoc.getNiceFileName()).isEqualTo("foo.c");
    assertThat(mainLoc.getStartingLineNumber()).isEqualTo(2);
    assertThat(mainLoc.getEndingLineNumber()).isEqualTo(2);
    assertThat(mainLoc.getStartingLineInOrigin()).isEqualTo(5);
    assertThat(mainLoc.getEndingLineInOrigin()).isEqualTo(5);
  }

  @Test
  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public void multiFileTest() throws CParserException, InterruptedException {
    String mainCode = "void main() { }";
    FileContentToParse main = new FileContentToParse(Path.of(fileName), mainCode);

    String additionalCode = "void foo() { }";
    String additionalFileName = fileName.replace("test", "additional");
    String expectedAdditionalFileName = expectedFileName.replace("test", "additional");
    FileContentToParse additional =
        new FileContentToParse(Path.of(additionalFileName), additionalCode);
    ParseResult result =
        parser.parseString(ImmutableList.of(main, additional), new CSourceOriginMapping());

    FileLocation mainLoc = result.getFunctions().get("main").getFileLocation();
    assertThat(mainLoc.getFileName()).isEqualTo(Path.of(expectedFileName));
    assertThat(mainLoc.getNiceFileName())
        .isEqualTo(Path.of(expectedFileName).getFileName().toString());
    assertThat(mainLoc.getStartingLineNumber()).isEqualTo(1);
    assertThat(mainLoc.getEndingLineNumber()).isEqualTo(1);
    assertThat(mainLoc.getStartingLineInOrigin()).isEqualTo(1);
    assertThat(mainLoc.getEndingLineInOrigin()).isEqualTo(1);

    FileLocation additionalLoc = result.getFunctions().get("foo").getFileLocation();
    assertThat(additionalLoc.getFileName()).isEqualTo(Path.of(expectedAdditionalFileName));
    assertThat(additionalLoc.getNiceFileName())
        .isEqualTo(Path.of(expectedAdditionalFileName).getFileName().toString());
    assertThat(additionalLoc.getStartingLineNumber()).isEqualTo(1);
    assertThat(additionalLoc.getEndingLineNumber()).isEqualTo(1);
    assertThat(additionalLoc.getStartingLineInOrigin()).isEqualTo(1);
    assertThat(additionalLoc.getEndingLineInOrigin()).isEqualTo(1);
  }
}
