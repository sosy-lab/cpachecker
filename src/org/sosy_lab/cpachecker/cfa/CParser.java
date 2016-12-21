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
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.EclipseParsers;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.EclipseParsers.EclipseCParserOptions;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.CParserException;

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

    public FileToParse(String pFileName) {
      this.fileName = pFileName;
    }

    public String getFileName() {
      return fileName;
    }
  }

  public static class FileContentToParse extends FileToParse {
    private final String fileContent;

    public FileContentToParse(String pFileName, String pFileContent) {
      super(pFileName);
      this.fileContent = pFileContent;
    }

    public String getFileContent() {
      return fileContent;
    }
  }

  /**
   * Parse the content of a String into a CFA.
   *
   * @param code The code to parse.
   * @return The CFA.
   * @throws CParserException If parser or CFA builder cannot handle the code.
   */
  @Override
  default ParseResult parseString(String filename, String code) throws CParserException {
    return parseString(filename, code, new CSourceOriginMapping(), CProgramScope.empty());
  }

  /**
   * Parse the content of files into a single CFA.
   *
   * @param filenames The List of files to parse. The first part of the pair should be the filename,
   *     the second part should be the prefix which will be appended to static variables
   * @return The CFA.
   * @throws IOException If file cannot be read.
   * @throws CParserException If parser or CFA builder cannot handle the C code.
   */
  ParseResult parseFile(List<String> filenames)
      throws CParserException, IOException, InterruptedException;

  /**
   * Parse the content of Strings into a single CFA.
   *
   * @param code The List of code fragments to parse. The first part of the pair should be the code,
   *     the second part should be the prefix which will be appended to static variables
   * @param sourceOriginMapping A mapping from real input file locations to original file locations
   *     (before pre-processing).
   * @return The CFA.
   * @throws CParserException If parser or CFA builder cannot handle the C code.
   */
  ParseResult parseString(List<FileContentToParse> code, CSourceOriginMapping sourceOriginMapping)
      throws CParserException;

  /**
   * Parse the content of a String into a CFA.
   *
   * @param pFileName the file name.
   * @param pCode the code to parse.
   * @param pSourceOriginMapping a mapping from real input file locations to original file locations
   *     (before pre-processing).
   * @param pScope an optional external scope to be used.
   * @return the parse result.
   * @throws CParserException if the parser cannot handle the C code.
   */
  ParseResult parseString(
      String pFileName, String pCode, CSourceOriginMapping pSourceOriginMapping, Scope pScope)
      throws CParserException;

  /**
   * Method for parsing a string that contains exactly one function with exactly one statement. Only
   * the AST for the statement is returned, the function declaration is stripped.
   *
   * <p>Example input: <code>
   * void foo() {
   *   bar();
   * }
   * </code> Example output: AST for "bar();"
   *
   * <p>This method guarantees that the AST does not contain CProblem nodes.
   *
   * @param code The code snippet as described above.
   * @param scope The scope is needed to resolve the type bindings in the statement.
   * @return The AST for the statement.
   * @throws CParserException If parsing fails.
   */
  CAstNode parseSingleStatement(String code, Scope scope) throws CParserException;

  /**
   * Method for parsing a block of statements that contains exactly one function with exactly one
   * block of statements. Only the List of ASTs for the block of statement is returned, the function
   * declaration is stripped.
   *
   * <p>Example input: <code>
   * void foo() {
   *   bar();
   *   a = 2;
   *   }
   * </code> Example output: AST for "<bar();, a = 2;>"
   *
   * <p>This method guarantees that the AST does not contain CProblem nodes.
   *
   * @param code The code snippet as described above.
   * @param scope The scope is needed to resolve the type bindings in the statement.
   * @return The list of ASTs for the statement.
   * @throws CParserException If parsing fails.
   */
  List<CAstNode> parseStatements(String code, Scope scope) throws CParserException;

  /**
   * Enum for clients of this class to choose the C dialect the parser uses.
   */
  public static enum Dialect {
    C99,
    GNUC,
    ;
  }

  @Options(prefix = "parser")
  public abstract static class ParserOptions {

    @Option(secure=true, description="C dialect for parser")
    private Dialect dialect = Dialect.GNUC;

    protected ParserOptions() {}

    public Dialect getDialect() {
      return dialect;
    }
  }

  /**
   * Factory that tries to create a parser based on available libraries
   * (e.g. Eclipse CDT).
   */
  public static class Factory {


    public static ParserOptions getOptions(Configuration config) throws InvalidConfigurationException {
      ParserOptions result = new EclipseCParserOptions();
      config.recursiveInject(result);
      return result;
    }

    public static ParserOptions getDefaultOptions() {
      return new EclipseCParserOptions();
    }

    public static CParser getParser(
        LogManager logger, ParserOptions options, MachineModel machine) {
      return EclipseParsers.getCParser(logger, (EclipseCParserOptions) options, machine);
    }
  }
}
