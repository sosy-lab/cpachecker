/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.parser.llvm;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.NativeLibraries;
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
  public ParseResult parseFile(final String pFilename) throws ParserException {
    addLlvmLookupDirs();
    try (Context llvmContext = Context.create();
        Module llvmModule = Module.parseIR(pFilename, llvmContext)) {
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

    Path cpacheckerDir = Paths.get(decodedBasePath).getParent();
    if (cpacheckerDir != null) {
      Path runtimeLibDir = Paths.get(cpacheckerDir.toString(), "lib", "java", "runtime");
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

  private ParseResult buildCfa(final Module pModule, final String pFilename) throws LLVMException {
    return cfaBuilder.build(pModule, pFilename);
  }

  @Override
  public ParseResult parseString(final String pFilename, final String pCode) {
    return null;
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
