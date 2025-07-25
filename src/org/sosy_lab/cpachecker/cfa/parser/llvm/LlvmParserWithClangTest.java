// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.llvm;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

/** Unit tests for {@link LlvmParserWithClang}. */
@RunWith(Parameterized.class)
@Ignore // cf. https://gitlab.com/sosy-lab/software/cpachecker/-/issues/1356
public class LlvmParserWithClangTest {

  @Parameters(name = "{0} with file name {1}")
  public static List<Object[]> testcases() {
    List<Path> testFiles =
        ImmutableList.of(
            Path.of("test/programs/llvm/switch-case.c"),
            Path.of("test/programs/simple/globalVariableInitialValue-1.c"));
    List<Path> fileNames = ImmutableList.of(Path.of("test.c"), Path.of("test"));
    List<List<Path>> testcases = Lists.cartesianProduct(testFiles, fileNames);
    return testcases.stream().map(List::toArray).collect(ImmutableList.toImmutableList());
  }

  @Parameter(0)
  public Path testFile;

  @Parameter(1)
  public Path fileName;

  // two parsers needed, as otherwise there is a problem with the already running parseTimer
  private LlvmParserWithClang fileParser;
  private LlvmParserWithClang stringParser;

  @Before
  public void createParsers() throws InvalidConfigurationException {
    Configuration config = TestDataTools.configurationForTest().build();
    LogManager logger = LogManager.createTestLogManager();
    stringParser = new LlvmParserWithClang(config, logger, MachineModel.LINUX32);
    fileParser = new LlvmParserWithClang(config, logger, MachineModel.LINUX32);
  }

  @Test
  public void testStringParseResultEqualsFileParseResult()
      throws ParserException, InterruptedException, InvalidConfigurationException, IOException {
    String code = Files.readString(testFile, Charset.defaultCharset());

    ParseResult fileResult;
    try {
      fileResult = fileParser.parseFiles(ImmutableList.of(testFile.toString()));
    } catch (ClangParserException e) {
      assume()
          .that(e)
          .hasMessageThat()
          .doesNotContain(
              "Clang failed: Cannot run program \"clang-"
                  + LlvmUtils.extractVersionNumberFromLlvmJ()
                  + "\": error=2");
      throw e;
    }
    ParseResult stringResult = stringParser.parseString(fileName, code);

    assertThat(stringResult.isEmpty()).isEqualTo(fileResult.isEmpty());
    assertThat(stringResult.cfaNodes()).hasSize(fileResult.cfaNodes().size());
    assertThat(stringResult.functions().keySet())
        .containsExactlyElementsIn(fileResult.functions().keySet());
    for (String function : stringResult.functions().keySet()) {
      assertThat(stringResult.cfaNodes().get(function))
          .hasSize(fileResult.cfaNodes().get(function).size());
    }
    assertThat(stringResult.globalDeclarations()).isEqualTo(fileResult.globalDeclarations());
  }
}
