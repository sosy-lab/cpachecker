// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.tests;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.IO;
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

  private final Path originalProgram;

  public CFAToCTranslatorTest(
      @SuppressWarnings("unused") final String pTestLabel,
      final String pProgram,
      final boolean pVerdict)
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

    originalProgram = Path.of(pProgram);
  }

  @Override
  protected void createProgram(final Path pTargetPath) throws Exception {
    final CFAToCTranslator translator = getTranslator();

    final CFA cfaToTranslate = parseProgram(originalProgram);
    final String result = translator.translateCfa(cfaToTranslate);

    IO.writeFile(pTargetPath, Charset.defaultCharset(), result);
  }

  private CFAToCTranslator getTranslator() throws InvalidConfigurationException {
    return new CFAToCTranslator(
        TestDataTools.configurationForTest()
            .setOption("cpa.arg.export.code.header", "false")
            .build());
  }

  private CFA parseProgram(final Path pProgram)
      throws ParserException, IOException, InterruptedException, InvalidConfigurationException {

    final ShutdownNotifier shutdown = ShutdownNotifier.createDummy();
    final Configuration parseConfig =
        Configuration.builder().setOption("analysis.useLoopStructure", "false").build();
    final CFACreator cfaCreator = new CFACreator(parseConfig, logger, shutdown);

    return cfaCreator.parseFileAndCreateCFA(ImmutableList.of(pProgram.toString()));
  }

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return ImmutableList.of(
        directTranslationTest("functionreturn.c", true),
        directTranslationTest("gotos.c", false),
        directTranslationTest("main.c", true),
        directTranslationTest("main2.c", true),
        directTranslationTest("multipleErrors.c", false),
        directTranslationTest("multipleLoops.c", false),
        directTranslationTest("no_overflow.c", true),
        // overflow.c has verdict true because the configuration does not check for overflows
        directTranslationTest("overflow.c", true),
        directTranslationTest("simple.c", true),
        directTranslationTest("simple2.c", true));
  }

  private static Object[] directTranslationTest(final String pProgram, final boolean pVerdict) {
    final String testLabel = String.format("directTranslationTest(%s is %s)", pProgram, pVerdict);
    return new Object[] {testLabel, TEST_DIR_PATH + pProgram, pVerdict};
  }
}
