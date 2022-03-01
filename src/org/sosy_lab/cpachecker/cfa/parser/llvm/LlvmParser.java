// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.llvm;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.NativeLibraries;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.Parser;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.LLVMParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.llvm_j.Context;
import org.sosy_lab.llvm_j.LLVMException;
import org.sosy_lab.llvm_j.Module;

/**
 * Parser for the LLVM intermediate language to a CFA. LLVM IR is a typed, assembler-like language
 * that uses the SSA form by default. Because of this, parsing is quite simple: there is no need for
 * scoping and expression trees are always flat.
 */
public class LlvmParser implements Parser {

  private final LogManager logger;
  private final CFABuilder cfaBuilder;

  private final Timer parseTimer = new Timer();
  private final Timer cfaCreationTimer = new Timer();

  public LlvmParser(final LogManager pLogger, final MachineModel pMachineModel) {
    logger = pLogger;
    cfaBuilder = new CFABuilder(logger, pMachineModel);
  }

  @Override
  public ParseResult parseFiles(final List<String> pFilenames)
      throws ParserException, InterruptedException, InvalidConfigurationException {

    if (pFilenames.size() > 1) {
      throw new InvalidConfigurationException(
          "Multiple program files not supported when using LLVM frontend.");
    }
    return parseFile(Path.of(pFilenames.get(0)));
  }

  protected ParseResult parseFile(final Path pFilename) throws LLVMParserException {
    addLlvmLookupDirs();
    try (Context llvmContext = Context.create();
        Module llvmModule = Module.parseIR(pFilename.toString(), llvmContext)) {
      parseTimer.start();
      return buildCfa(llvmModule, pFilename);

    } catch (LLVMException pE) {
      throw new LLVMParserException(pE);
    }
  }

  private void addLlvmLookupDirs() {
    List<Path> libDirs = new ArrayList<>(3);
    Path nativeDir = NativeLibraries.getNativeLibraryPath();
    libDirs.add(nativeDir);

    // If cpachecker.jar is used, decodedBasePath will look similar to CPACHECKER/cpachecker.jar .
    // If the compiled class files are used outside of a jar, decodedBasePath will look similar to
    // CPACHECKER/bin .
    // In both cases, we strip the last part to get the CPAchecker base directory.
    String encodedBasePath =
        LlvmParser.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    String decodedBasePath = URLDecoder.decode(encodedBasePath, StandardCharsets.UTF_8);

    Path cpacheckerDir = Path.of(decodedBasePath).getParent();
    if (cpacheckerDir != null) {
      Path runtimeLibDir = Path.of(cpacheckerDir.toString(), "lib", "java", "runtime");
      libDirs.add(runtimeLibDir);
    } else {
      logger.logf(
          Level.INFO,
          "Base path %s of CPAchecker seems to have no parent directory",
          decodedBasePath);
    }

    for (Path p : libDirs) {
      logger.logf(Level.FINE, "Adding llvm shared library lookup dir: %s", p);
    }
    Module.addLibraryLookupPaths(libDirs);
  }

  private ParseResult buildCfa(final Module pModule, final Path pFilename) throws LLVMException {
    return cfaBuilder.build(pModule, pFilename);
  }

  @Override
  public ParseResult parseString(final Path pFilename, final String pCode) {
    // TODO
    throw new UnsupportedOperationException();
  }

  @Override
  public Timer getParseTime() {
    return parseTimer;
  }

  @Override
  public Timer getCFAConstructionTime() {
    return cfaCreationTimer;
  }
}
