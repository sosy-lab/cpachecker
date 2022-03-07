// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import java.nio.file.Path;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.parser.Parsers;
import org.sosy_lab.cpachecker.cfa.parser.Parsers.EclipseCParserOptions;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
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

  class FileToParse {
    private final Path fileName;

    public FileToParse(Path pFileName) {
      this.fileName = pFileName;
    }

    public Path getFileName() {
      return fileName;
    }
  }

  class FileContentToParse extends FileToParse {
    private final String fileContent;

    public FileContentToParse(Path pFileName, String pFileContent) {
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
   * @param filename A filename that is the supposed source of this code (for relative lookups).
   * @param code The code to parse.
   * @return The CFA.
   * @throws CParserException If parser or CFA builder cannot handle the code.
   */
  @Override
  default ParseResult parseString(Path filename, String code)
      throws CParserException, InterruptedException {
    return parseString(filename, code, new CSourceOriginMapping(), CProgramScope.empty());
  }

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
      throws CParserException, InterruptedException;

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
      Path pFileName, String pCode, CSourceOriginMapping pSourceOriginMapping, Scope pScope)
      throws CParserException, InterruptedException;

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
  CAstNode parseSingleStatement(String code, Scope scope)
      throws CParserException, InterruptedException;

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
  List<CAstNode> parseStatements(String code, Scope scope)
      throws CParserException, InterruptedException;

  /** Enum for clients of this class to choose the C dialect the parser uses. */
  enum Dialect {
    C99,
    GNUC,
    ;
  }

  @Options(prefix = "parser")
  abstract class ParserOptions {

    @Option(secure=true, description="C dialect for parser")
    private Dialect dialect = Dialect.GNUC;

    @Option(secure = true, description = "Whether to collect ACSL annotations if present")
    private boolean collectACSLAnnotations = false;

    protected ParserOptions() {}

    public Dialect getDialect() {
      return dialect;
    }

    public boolean shouldCollectACSLAnnotations() {
      return collectACSLAnnotations;
    }
  }

  /** Factory that tries to create a parser based on available libraries (e.g. Eclipse CDT). */
  class Factory {

    public static ParserOptions getOptions(Configuration config) throws InvalidConfigurationException {
      ParserOptions result = new EclipseCParserOptions();
      config.recursiveInject(result);
      return result;
    }

    public static ParserOptions getDefaultOptions() {
      return new EclipseCParserOptions();
    }

    public static CParser getParser(
        LogManager logger,
        ParserOptions options,
        MachineModel machine,
        ShutdownNotifier shutdownNotifier) {
      return Parsers.getCParser(logger, (EclipseCParserOptions) options, machine, shutdownNotifier);
    }
  }
}
