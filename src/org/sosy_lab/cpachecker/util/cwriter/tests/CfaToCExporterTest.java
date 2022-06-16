// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.tests;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
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

  @Test
  public void testProgramsStaySame() throws Exception {
    createProgram(targetProgram);

    // test whether exported C program is equal to the input program
    FileUtils.contentEquals(originalProgram.toFile(), targetProgram.toFile());
  }

  @Override
  protected void createProgram(final Path pTargetPath) throws Exception {
    final CfaToCExporter exporter = getExporter();

    final CFA cfaToExport = parseProgram(originalProgram);
    final String result = exporter.exportCfa(cfaToExport);

    IO.writeFile(pTargetPath, Charset.defaultCharset(), result);
  }

  private CfaToCExporter getExporter() throws InvalidConfigurationException {
    return new CfaToCExporter(
        logger, Configuration.defaultConfiguration(), ShutdownNotifier.createDummy());
  }

  private CFA parseProgram(final Path pProgram)
      throws ParserException, IOException, InterruptedException, InvalidConfigurationException {

    final ShutdownNotifier shutdown = ShutdownNotifier.createDummy();
    final Configuration parseConfig = Configuration.defaultConfiguration();
    final CFACreator cfaCreator = new CFACreator(parseConfig, logger, shutdown);

    return cfaCreator.parseFileAndCreateCFA(ImmutableList.of(pProgram.toString()));
  }

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return ImmutableList.<Object[]>builder()
        .addAll(CFAToCTranslatorTest.data())
        .add(
            directExportTest("declaration.c"),
            directExportTest("declaration_multiple-on-one-line.c"),
            directExportTest("declaration_split.c"),
            directExportTest("declaration_with-side-effect.c"),
            directExportTest("declaration_with-side-effect_multiple-on-one-line.c"),
            directExportTest("for.c"),
            directExportTest("for_condition-negated.c"),
            directExportTest("function-call.c"),
            directExportTest("functions_multiple.c"),
            directExportTest("global-var.c"),
            directExportTest("global-var_multiple.c"),
            directExportTest("goto_label_multiple-per-statement.c"),
            directExportTest("goto_label_on-empty-statement.c"),
            directExportTest("goto_label_on-if.c"),
            directExportTest("goto_with-dead-code.c"),
            directExportTest("if-else.c"),
            directExportTest("if-else_condition-abbreviated.c"),
            directExportTest("if-else_condition-negated.c"),
            directExportTest("if-else_condition-negated_double.c"),
            directExportTest("if-else_condition-true.c"),
            directExportTest("if-else_else-branch-empty.c"),
            directExportTest("if-else_else-branch-missing.c"),
            directExportTest("if-else_multiple-returns.c"),
            directExportTest("if-else_with-goto.c"),
            directExportTest("if-else_with-goto-out-and-back-in.c"),
            directExportTest("if-else_with-goto-out-of-both-branches.c"),
            directExportTest("if-else_with-goto_labeled-return-within.c"),
            directExportTest("if-else_with-goto_labeled-return-within-both-branches.c"),
            directExportTest("mixed.c"),
            directExportTest("switch-case.c"),
            directExportTest("while.c"))
        .build();
  }

  /**
   * Generate test case for the given program with verdict true.
   *
   * <p>The test programs do not include error locations, because the latter do not need to be
   * handled in a special manner in export. Therefore, the verdict is always true. Consequently, a
   * verdict change is highly unlikely and the inherited {@link
   * ToCTranslationTest#testVerdictsStaySame()} is unlikely to fail. The {@link ToCTranslationTest}
   * is extended nonetheless because we are interested in compilability and parsability of the
   * exported program.
   */
  private static Object[] directExportTest(final String pProgram) {
    final boolean verdict = true;
    final String testLabel = String.format("directExportTest(%s)", pProgram);
    return new Object[] {testLabel, EXPORTER_TEST_DIR_PATH + pProgram, verdict};
  }
}
