// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.tests;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.TempFile;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.cwriter.CFAToCTranslator;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.ToCTranslationTest;

/** Tests for {@link CFAToCTranslator}. */
@RunWith(Parameterized.class)
public class CFAToCTranslatorTest extends ToCTranslationTest {

  private Path originalProgram;

  public CFAToCTranslatorTest(
      @SuppressWarnings("unused") String pTestLabel, String pProgram, boolean pVerdict)
      throws InvalidConfigurationException, IOException {
    super(
        /* pTargetProgram = */ TempFile.builder()
            .prefix("residual")
            .suffix(".c")
            .create()
            .toAbsolutePath(),
        /* pVerdict = */ pVerdict,
        /* pCheckerConfig = */ TestDataTools.configurationForTest()
            .loadFromResource(CFAToCTranslatorTest.class, "predicateAnalysis.properties")
            .build());

    originalProgram = Paths.get(TEST_DIR_PATH, pProgram);
  }

  private CFAToCTranslator getTranslator() throws InvalidConfigurationException {
    return new CFAToCTranslator(
        TestDataTools.configurationForTest()
            .setOption("cpa.arg.export.code.header", "false")
            .build());
  }

  private CFA parseProgram(Path pProgram) {
    try {
      final ShutdownNotifier shutdown = ShutdownNotifier.createDummy();
      final Configuration parseConfig;
      parseConfig = Configuration.builder().setOption("analysis.useLoopStructure", "false").build();
      final CFACreator cfaCreator = new CFACreator(parseConfig, logger, shutdown);

      return cfaCreator.parseFileAndCreateCFA(Lists.newArrayList(pProgram.toString()));

    } catch (InvalidConfigurationException
        | InterruptedException
        | IOException
        | ParserException pE) {
      throw new AssertionError(pE);
    }
  }

  @Override
  protected void createProgram(Path pTargetPath) throws Exception {
    CFAToCTranslator translator = getTranslator();

    CFA cfaToTranslate = parseProgram(originalProgram);
    String res = translator.translateCfa(cfaToTranslate);

    Files.write(pTargetPath, res.getBytes("utf-8"));
  }

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return ImmutableList.of(
        directTranslationTest("gotos.c", false),
        directTranslationTest("main2.c", true),
        directTranslationTest("main.c", true),
        directTranslationTest("multipleErrors.c", false),
        directTranslationTest("simple2.c", true),
        directTranslationTest("simple.c", true));
  }

  private static Object[] directTranslationTest(final String pProgram, final boolean pVerdict) {
    final String testLabel = String.format("directTranslationTest(%s is %s)", pProgram, pVerdict);
    return new Object[] {testLabel, pProgram, pVerdict};
  }
}
