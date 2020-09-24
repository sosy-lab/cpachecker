// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import java.nio.file.Path;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator.BlockTreatmentAtFunctionEnd;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator.TargetTreatment;

@Options(prefix = "cpa.arg.export.code")
public class TranslatorConfig {

  @Option(secure = true, name = "header", description = "write include directives")
  private boolean includeHeader = true;

  @Option(
      secure = true,
      name = "blockAtFunctionEnd",
      description =
          "Only enable CLOSEFUNCTIONBLOCK if you are sure that the ARG merges different flows"
              + " through a function at the end of the function.")
  private BlockTreatmentAtFunctionEnd handleCompoundStatementAtEndOfFunction =
      BlockTreatmentAtFunctionEnd.KEEPBLOCK;

  @Option(
      secure = true,
      name = "handleTargetStates",
      description = "How to deal with target states during code generation")
  private TargetTreatment targetStrategy = TargetTreatment.NONE;

  @Option(
      secure = true,
      description =
          "Enable the integration of __VERIFIER_assume statements for non-true assumption in"
              + " states. Disable if you want to create residual programs.")
  private boolean addAssumptions = true;

  @Option(
      secure = true,
      description =
          "If specified, metadata about the produced C program will be exported to this file")
  @FileOption(Type.OUTPUT_FILE)
  private Path metadataOutput = null;

  public TranslatorConfig(final Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  private TranslatorConfig() {
    // do nothing
  }

  public static TranslatorConfig getDefault() {
    return new TranslatorConfig();
  }

  public boolean doIncludeHeader() {
    return includeHeader;
  }

  public BlockTreatmentAtFunctionEnd doHandleCompoundStatementAtEndOfFunction() {
    return handleCompoundStatementAtEndOfFunction;
  }

  public TargetTreatment getTargetStrategy() {
    return targetStrategy;
  }

  public boolean doAddAssumptions() {
    return addAssumptions;
  }

  public Path getMetadataOutput() {
    return metadataOutput;
  }
}
