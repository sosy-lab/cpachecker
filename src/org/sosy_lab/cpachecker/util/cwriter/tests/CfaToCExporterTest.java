// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.tests;

import static org.sosy_lab.cpachecker.util.cwriter.tests.CFAToCTranslatorTest.directTranslationTest;

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
import org.sosy_lab.cpachecker.util.cwriter.CfaToCExporter;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.ToCTranslationTest;

/** Tests for {@link org.sosy_lab.cpachecker.util.cwriter.CfaToCExporter}. */
@RunWith(Parameterized.class)
public class CfaToCExporterTest extends ToCTranslationTest {

  public static final String EXPORTER_TEST_DIR_PATH = "test/programs/cfa_to_c_export/";

  private final Path originalProgram;

  public CfaToCExporterTest(
      @SuppressWarnings("unused") String pTestLabel, String pProgram, boolean pVerdict)
      throws InvalidConfigurationException, IOException {
    super(
        TempFile.builder().prefix("residual").suffix(".c").create().toAbsolutePath(),
        pVerdict,
        TestDataTools.configurationForTest()
            .loadFromResource(CFAToCTranslatorTest.class, "predicateAnalysis.properties")
            .build());

    originalProgram = Path.of(pProgram);
  }

  @Override
  protected void createProgram(Path pTargetPath) throws Exception {
    CFA cfaToExport = parseProgram(originalProgram);
    String result = CfaToCExporter.exportCfa(cfaToExport);

    IO.writeFile(pTargetPath, Charset.defaultCharset(), result);
  }

  private CFA parseProgram(Path pProgram) {
    final ShutdownNotifier shutdown = ShutdownNotifier.createDummy();
    final Configuration parseConfig = Configuration.defaultConfiguration();

    try {
      final CFACreator cfaCreator = new CFACreator(parseConfig, logger, shutdown);

      return cfaCreator.parseFileAndCreateCFA(ImmutableList.of(pProgram.toString()));

    } catch (InvalidConfigurationException
        | ParserException
        | IOException
        | InterruptedException pE) {
      throw new AssertionError(pE);
    }
  }

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return ImmutableList.of(
        directTranslationTest(EXPORTER_TEST_DIR_PATH + "declaration.c", true),
        directTranslationTest(EXPORTER_TEST_DIR_PATH + "declaration_multiple-on-one-line.c", true),
        directTranslationTest(EXPORTER_TEST_DIR_PATH + "declaration_split.c", true),
        directTranslationTest(EXPORTER_TEST_DIR_PATH + "declaration_with-side-effect.c", true),
        directTranslationTest(
            EXPORTER_TEST_DIR_PATH + "declaration_with-side-effect_multiple-on-one-line.c", true),
        directTranslationTest(EXPORTER_TEST_DIR_PATH + "functions_multiple.c", true),
        directTranslationTest(EXPORTER_TEST_DIR_PATH + "global-var.c", true),
        directTranslationTest(EXPORTER_TEST_DIR_PATH + "global-var_multiple.c", true),
        directTranslationTest(EXPORTER_TEST_DIR_PATH + "goto_with-dead-code.c", true),
        directTranslationTest(EXPORTER_TEST_DIR_PATH + "goto_with-double-label.c", true),
        directTranslationTest(
            EXPORTER_TEST_DIR_PATH + "goto_with-label-with-empty-statement.c", true),
        directTranslationTest(EXPORTER_TEST_DIR_PATH + "if-else.c", true),
        directTranslationTest(EXPORTER_TEST_DIR_PATH + "if-else_multiple-returns.c", true),
        directTranslationTest(EXPORTER_TEST_DIR_PATH + "if-else_negated.c", true),
        directTranslationTest(EXPORTER_TEST_DIR_PATH + "if-else_negated_double.c", true),
        directTranslationTest(EXPORTER_TEST_DIR_PATH + "if-else_with-empty-else-branch.c", true));
  }
}
