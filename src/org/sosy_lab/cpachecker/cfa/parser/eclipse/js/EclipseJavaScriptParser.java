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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.js;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.Parser;
import org.sosy_lab.cpachecker.exceptions.JSParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/** Wrapper for Eclipse JSDT Parser. */
class EclipseJavaScriptParser implements Parser {
  //  @Option(secure=true, name ="java.encoding",
  //      description="use the following encoding for java files")
  private Charset encoding = StandardCharsets.UTF_8;

  private final ASTParser parser = ASTParser.newParser(AST.JLS3);

  private final LogManager logger;

  private final Timer parseTimer = new Timer();
  private final Timer cfaTimer = new Timer();

  public EclipseJavaScriptParser(LogManager pLogger) throws InvalidConfigurationException {
    logger = pLogger;
  }

  private IJavaScriptProject createProject() {
    return new JavaProject(new DummyProject(), null);
  }

  @Override
  public ParseResult parseFile(String filename)
      throws ParserException, IOException, InterruptedException {
    Path file = Paths.get(filename);
    parseTimer.start();
    try {
      final String source = MoreFiles.toString(file, encoding);
      parser.setProject(createProject()); // required to resolve bindings
      parser.setUnitName(file.normalize().toString());
      parser.setSource(source.toCharArray());
      parser.setResolveBindings(true);
      parser.setBindingsRecovery(true);
      return buildCFA(parser.createAST(null), new Scope(filename));
    } catch (IOException e) {
      throw new JSParserException(e);
    } finally {
      parseTimer.stop();
    }
  }

  @Override
  public ParseResult parseString(String filename, String code) throws ParserException {
    throw new JSParserException("Parsing of string not yet implemented");
  }

  @Override
  public Timer getParseTime() {
    return parseTimer;
  }

  @Override
  public Timer getCFAConstructionTime() {
    return cfaTimer;
  }

  private ParseResult buildCFA(ASTNode ast, Scope pScope) throws JSParserException {
    cfaTimer.start();

    CFABuilder builder = new CFABuilder(pScope, logger);
    try {
      ast.accept(builder);
      return builder.createCFA();
    } finally {
      cfaTimer.stop();
    }
  }
}
