/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa;

import java.io.IOException;
import java.util.List;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.EclipseParsers;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/**
 * Abstraction of a C parser that creates CFAs from C code.
 *
 * A C parser should be state-less and therefore thread-safe as well as reusable.
 *
 * It may offer timing of it's operations. If present, this is not expected to
 * be thread-safe.
 */
public interface CParser extends Parser {

  public static class FileToParse {
    private final String fileName;
    private final String staticVariablePrefix;

    public FileToParse(String pFileName, String pStaticVariablePrefix) {
      this.fileName = pFileName;
      this.staticVariablePrefix = pStaticVariablePrefix;
    }

    public String getFileName() {
      return fileName;
    }

    public String getStaticVariablePrefix() {
      return staticVariablePrefix;
    }
  }

  public static class FileContentToParse extends FileToParse {
    private final String fileContent;

    public FileContentToParse(String pFileName, String pFileContent, String pStaticVariablePrefix) {
      super(pFileName, pStaticVariablePrefix);
      this.fileContent = pFileContent;
    }

    public String getFileContent() {
      return fileContent;
    }
  }

  /**
   * Parse the content of files into a single CFA.
   *
   * @param fileNames  The List of files to parse. The first part of the pair
   *                   should be the filename, the second part should be the
   *                   prefix which will be appended to static variables
   * @return The CFA.
   * @throws IOException If file cannot be read.
   * @throws InterruptedException
   * @throws ParserException If parser or CFA builder cannot handle the C code.
   */
  ParseResult parseFile(List<FileToParse> filenames) throws CParserException, IOException, InvalidConfigurationException, InterruptedException;

  /**
   * Parse the content of Strings into a single CFA.
   *
   * @param code  The List of code fragments to parse. The first part of the pair
   *                   should be the code, the second part should be the
   *                   prefix which will be appended to static variables
   * @return The CFA.
   * @throws ParserException If parser or CFA builder cannot handle the C code.
   */
  ParseResult parseString(List<FileContentToParse> code) throws CParserException, InvalidConfigurationException;

  /**
   * Method for parsing a string that contains exactly one function with exactly
   * one statement. Only the AST for the statement is returned, the function
   * declaration is stripped.
   *
   * Example input:
   * void foo() { bar(); }
   * Example output:
   * AST for "bar();"
   *
   * This method guarantees that the AST does not contain CProblem nodes.
   *
   * @param code The code snippet as described above.
   * @param dialect The parser dialect to use.
   * @return The AST for the statement.
   * @throws ParserException If parsing fails.
   */
  CAstNode parseSingleStatement(String code) throws CParserException, InvalidConfigurationException;

  /**
   * Method for parsing a block of statements that contains exactly one function with exactly
   * one block of statements. Only the List of ASTs for the block of statement is returned, the function
   * declaration is stripped.
   *
   * Example input:
   * void foo() { bar();a = 2; }
   * Example output:
   * AST for "<bar();, a = 2;>"
   *
   * This method guarantees that the AST does not contain CProblem nodes.
   *
   * @param code The code snippet as described above.
   * @param dialect The parser dialect to use.
   * @return The list of ASTs for the statement.
   * @throws ParserException If parsing fails.
   */
  List<CAstNode> parseStatements(String code) throws CParserException, InvalidConfigurationException;

  /**
   * Enum for clients of this class to choose the C dialect the parser uses.
   */
  public static enum Dialect {
    C99,
    GNUC,
    ;
  }

  @Options(prefix="parser")
  public final static class ParserOptions {

    @Option(description="C dialect for parser")
    private Dialect dialect = Dialect.GNUC;

    private ParserOptions() { }
  }

  /**
   * Factory that tries to create a parser based on available libraries
   * (e.g. Eclipse CDT).
   */
  public static class Factory {


    public static ParserOptions getOptions(Configuration config) throws InvalidConfigurationException {
      ParserOptions result = new ParserOptions();
      config.inject(result);
      return result;
    }

    public static ParserOptions getDefaultOptions() {
      return new ParserOptions();
    }

    public static CParser getParser(Configuration config, LogManager logger, ParserOptions options, MachineModel machine) {
      return EclipseParsers.getCParser(config, logger, options.dialect, machine);
    }
  }
}
