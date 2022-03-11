// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// Copyright (C) 2007-2017  Dirk Beyer
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.TempFile;
import org.sosy_lab.common.io.TempFile.DeleteOnCloseDir;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.parser.llvm.LlvmParser;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.ClangParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/**
 * Parser for the LLVM intermediate language to a CFA. LLVM IR is a typed, assembler-like language
 * that uses the SSA form by default. Because of this, parsing is quite simple: there is no need for
 * scoping and expression trees are always flat.
 */
public class LlvmParserWithClang extends LlvmParser {

  private final ClangPreprocessor preprocessor;

  public LlvmParserWithClang(
      final ClangPreprocessor pPreprocessor,
      final LogManager pLogger,
      final MachineModel pMachineModel) {
    super(pLogger, pMachineModel);
    preprocessor = pPreprocessor;
  }

  @Override
  public ParseResult parseFiles(final List<String> pFilenames)
      throws ParserException, InterruptedException, InvalidConfigurationException {

    if (pFilenames.size() > 1) {
      throw new InvalidConfigurationException(
          "Multiple program files not supported when using LLVM frontend.");
    }
    Path filename = Path.of(pFilenames.get(0));

    return parseSingleFile(filename);
  }

  @Override
  public ParseResult parseString(final Path pFilename, final String pCode)
      throws ParserException, InterruptedException {
    Objects.requireNonNull(pFilename);
    // The input is written to a file in a temporary directory so that clang can preprocess it.
    // This temp dir will be automatically deleted when the try block terminates.
    try (DeleteOnCloseDir tempDir = TempFile.createDeleteOnCloseDir("input-for-llvm-with-clang")) {
      Path tempFile = tempDir.toPath().resolve(pFilename).normalize();
      IO.writeFile(tempFile, Charset.defaultCharset(), pCode);
      return parseSingleFile(tempFile);
    } catch (IOException e) {
      throw new ClangParserException("Could not write clang input to file " + e.getMessage(), e);
    }
  }

  private ParseResult parseSingleFile(final Path pFilename)
      throws ParserException, InterruptedException {

    if (preprocessor.getDumpDirectory() != null) {
      // Writing to the output directory is possible.
      return parse0(pFilename, preprocessor.getDumpDirectory());
    }

    try (DeleteOnCloseDir tempDir = TempFile.createDeleteOnCloseDir("clang-results")) {
      return parse0(pFilename, tempDir.toPath());
    } catch (IOException e) {
      throw new ClangParserException("Could not write clang output to file " + e.getMessage(), e);
    }
  }

  private ParseResult parse0(final Path pFilename, final Path pDumpDirectory)
      throws ParserException, InterruptedException {
    Path dumpedFile = preprocessor.preprocessAndGetDumpedFile(pFilename, pDumpDirectory);
    return super.parseFile(dumpedFile);
  }

  static class Factory {
    public static LlvmParserWithClang getParser(
        ClangPreprocessor processor, LogManager logger, MachineModel machine) {
      return new LlvmParserWithClang(processor, logger, machine);
    }
  }
}
