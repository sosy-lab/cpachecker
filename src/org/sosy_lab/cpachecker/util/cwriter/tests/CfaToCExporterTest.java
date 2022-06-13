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

    originalProgram = Path.of(EXPORTER_TEST_DIR_PATH + pProgram);
  }

  @Override
  protected void createProgram(Path pTargetPath) throws Exception {
    CFA cfaToExport = parseProgram(originalProgram);
    String result =
        new CfaToCExporter(
                logger, Configuration.defaultConfiguration(), ShutdownNotifier.createDummy())
            .exportCfa(cfaToExport);

    IO.writeFile(pTargetPath, Charset.defaultCharset(), result);
  }

  private CFA parseProgram(Path pProgram)
      throws ParserException, IOException, InterruptedException, InvalidConfigurationException {

    final ShutdownNotifier shutdown = ShutdownNotifier.createDummy();
    final Configuration parseConfig = Configuration.defaultConfiguration();
    final CFACreator cfaCreator = new CFACreator(parseConfig, logger, shutdown);
    return cfaCreator.parseFileAndCreateCFA(ImmutableList.of(pProgram.toString()));
  }

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return ImmutableList.of(
        directTranslationTest("declaration.c", true),
        directTranslationTest("declaration_multiple-on-one-line.c", true),
        directTranslationTest("declaration_split.c", true),
        directTranslationTest("declaration_with-side-effect.c", true),
        directTranslationTest("declaration_with-side-effect_multiple-on-one-line.c", true),
        directTranslationTest("for.c", true),
        directTranslationTest("for_condition-negated.c", true),
        directTranslationTest("function-call.c", true),
        directTranslationTest("functions_multiple.c", true),
        directTranslationTest("global-var.c", true),
        directTranslationTest("global-var_multiple.c", true),
        directTranslationTest("goto_label_multiple-per-statement.c", true),
        directTranslationTest("goto_label_on-empty-statement.c", true),
        directTranslationTest("goto_label_on-if.c", true),
        directTranslationTest("goto_with-dead-code.c", true),
        directTranslationTest("if-else.c", true),
        directTranslationTest("if-else_condition-abbreviated.c", true),
        directTranslationTest("if-else_condition-negated.c", true),
        directTranslationTest("if-else_condition-negated_double.c", true),
        directTranslationTest("if-else_condition-true.c", true),
        directTranslationTest("if-else_else-branch-empty.c", true),
        directTranslationTest("if-else_else-branch-missing.c", true),
        directTranslationTest("if-else_multiple-returns.c", true),
        directTranslationTest("if-else_with-goto.c", true),
        directTranslationTest("if-else_with-goto-out-and-back-in.c", true),
        directTranslationTest("if-else_with-goto-out-of-both-branches.c", true),
        directTranslationTest("if-else_with-goto_labeled-return-within.c", true),
        directTranslationTest("if-else_with-goto_labeled-return-within-both-branches.c", true),
        directTranslationTest("mixed.c", true),
        directTranslationTest("switch-case.c", true),
        directTranslationTest("while.c", true));
  }
}
