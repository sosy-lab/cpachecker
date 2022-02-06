// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.MoreFiles;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
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
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

/** Unit tests for {@link LlvmParserWithClang}. */
@RunWith(Parameterized.class)
public class LlvmParserWithClangTest {

  @Parameters(name = "{0} with file name {1}")
  public static List<Object[]> testcases() {
    List<String> testFiles =
        ImmutableList.of(
            "test/programs/llvm/switch-case.c",
            "test/programs/simple/globalVariableInitialValue-1.c");
    List<String> fileNames = ImmutableList.of("test.c", "test", "");
    List<List<String>> testcases = Lists.cartesianProduct(testFiles, fileNames);
    return testcases.stream().map(List::toArray).collect(ImmutableList.toImmutableList());
  }

  @Parameter(0)
  public String testFile;

  @Parameter(1)
  public String fileName;

  // two parsers needed, as otherwise there is a problem with the already running parseTimer
  private LlvmParserWithClang fileParser;
  private LlvmParserWithClang stringParser;

  @Before
  public void createParsers() throws InvalidConfigurationException {
    Configuration config = TestDataTools.configurationForTest().build();
    LogManager logger = LogManager.createTestLogManager();
    ClangPreprocessor clangPreprocessor = new ClangPreprocessor(config, logger);
    stringParser =
        LlvmParserWithClang.Factory.getParser(clangPreprocessor, logger, MachineModel.LINUX32);
    fileParser =
        LlvmParserWithClang.Factory.getParser(clangPreprocessor, logger, MachineModel.LINUX32);
  }

  @Test
  public void compareStringParsingAndFileParsing()
      throws ParserException, InterruptedException, InvalidConfigurationException, IOException {
    ParseResult fileResult = fileParser.parseFiles(ImmutableList.of(testFile));
    String code = MoreFiles.asCharSource(Path.of(testFile), Charset.defaultCharset()).read();
    ParseResult stringResult = stringParser.parseString(fileName, code);
    assertThat(stringResult.isEmpty()).isEqualTo(fileResult.isEmpty());
    assertThat(stringResult.getCFANodes()).hasSize(fileResult.getCFANodes().size());
    assertThat(stringResult.getFunctions().keySet())
        .containsExactlyElementsIn(fileResult.getFunctions().keySet());
    for (String function : stringResult.getFunctions().keySet()) {
      assertThat(stringResult.getCFANodes().get(function))
          .hasSize(fileResult.getCFANodes().get(function).size());
    }
    assertThat(stringResult.getGlobalDeclarations()).isEqualTo(fileResult.getGlobalDeclarations());
  }
}
