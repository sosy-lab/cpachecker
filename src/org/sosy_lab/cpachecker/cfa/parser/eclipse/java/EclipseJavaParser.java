/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.Parser;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.exceptions.JParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * Wrapper around the JDT Parser and CFA-Builder Implementation.
 *
 */
public class EclipseJavaParser implements Parser {


  private String javaRootPath = null;


  private static final boolean IGNORE_METHOD_BODY = true;
  private static final boolean PARSE_METHOD_BODY = false;
  private static final String JAVA_SOURCE_FILE_REGEX = ".*.java";


  private static final int  LENGTH_OF_SUFFIX = 5;

  private final ASTParser parser = ASTParser.newParser(AST.JLS4);


  private final LogManager logger;

  private final Timer parseTimer = new Timer();
  private final Timer cfaTimer = new Timer();

  private final String encoding;
  private final String version;

  public EclipseJavaParser(LogManager pLogger, String rootPath, String pEncoding, String pVersion) {
    logger = pLogger;
    javaRootPath = rootPath;
    encoding = pEncoding;
    version = pVersion;
  }

  /**
   * Parse the program of the Main class in this file into a CFA.
   *
   * @param fileName  The Main Class File of the program to parse.
   * @return The CFA.
   * @throws IOExceptio
   * @throws ParserException If parser or CFA builder cannot handle the  code.
   */
  @Override
  public ParseResult parseFile(String fileName) throws JParserException {
    File mainClassFile = getMainClassFile(fileName);
    Scope scope = prepareScope(fileName);
    return buildCFA(parse(mainClassFile), scope);
  }

  private File getMainClassFile(String fileName)  {
    File file = new File(fileName);
    return file;
  }

  private Scope prepareScope(String FileName) throws JParserException {


    List<Pair<CompilationUnit,String>> astsOfFoundFiles = getASTsOfProgram();

    Map<String, String> typeOfFiles = new HashMap<String,String>();
    Map<String, JClassOrInterfaceType> types = getTypeHieachie(astsOfFoundFiles, typeOfFiles);
    String mainClassName = getMainClassName(FileName);

    return new Scope(mainClassName, javaRootPath, types, typeOfFiles);
  }

  private String getMainClassName(String pFileName) {

    return pFileName.substring(javaRootPath.length(), pFileName.length() - LENGTH_OF_SUFFIX).replace(File.separatorChar, '.');
  }

  private Map<String, JClassOrInterfaceType> getTypeHieachie( List<Pair<CompilationUnit, String>> astsOfFoundFiles, Map<String, String> pTypeOfFiles) {

    Map<String, JClassOrInterfaceType> types = new HashMap<String, JClassOrInterfaceType>();

    TypeHierachyCreator creator = new TypeHierachyCreator(logger, types, pTypeOfFiles);

    for (Pair<CompilationUnit, String> ast : astsOfFoundFiles) {
      creator.setFileOfCU(ast.getSecond());
      ast.getFirst().accept(creator);
    }

    return types;
  }

  private List<Pair<CompilationUnit, String>> getASTsOfProgram() throws JParserException {
    Queue<File> sourceFileToBeParsed = getProgramFilesInRootPath();
    List<Pair<CompilationUnit,String>> astsOfFoundFiles = new LinkedList<Pair<CompilationUnit,String>>();

    for (File file : sourceFileToBeParsed) {
      astsOfFoundFiles.add(Pair.of(parse(file, IGNORE_METHOD_BODY), file.getName()));
    }

    return astsOfFoundFiles;
  }

  @Override
  public ParseResult parseString(String pCode) throws JParserException {

    throw new JParserException("Function not yet implemented");
  }

  private CompilationUnit parse(File file) throws JParserException {
    return parse(file, PARSE_METHOD_BODY);
  }

  private Queue<File>
                  getProgramFilesInRootPath() throws JParserException {

    File mainDirectory = new File(javaRootPath);
    assert mainDirectory.isDirectory() : "Could not find main directory at" + javaRootPath;

    Queue<File> directorysToBeSearched = new LinkedList<File>();
    Queue<File> sourceFileToBeParsed = new LinkedList<File>();
    directorysToBeSearched.add(mainDirectory);

    while (!directorysToBeSearched.isEmpty()) {

      File directory = directorysToBeSearched.poll();

      for (String filePath : directory.list()) {

        File file =
            new File(directory.getAbsolutePath() + File.separatorChar + filePath);

        if (filePath.matches(JAVA_SOURCE_FILE_REGEX)) {
          sourceFileToBeParsed.add(file);
        } else if (file.isDirectory()) {
          directorysToBeSearched.add(file);
        }
      }
    }

    return sourceFileToBeParsed;
  }

  private CompilationUnit parse(File file, boolean ignoreMethodBody) throws JParserException {

    final String[] sourceFilePath = new String[1];
    sourceFilePath[0] = javaRootPath;

    final String[] encodingList = { encoding };

    parser.setEnvironment(null, sourceFilePath, encodingList, false);
    parser.setResolveBindings(true);
    parser.setStatementsRecovery(true);
    parser.setBindingsRecovery(true);

    // Set Compliance Options to support Version
    @SuppressWarnings("unchecked")
    Hashtable<String, String> options = JavaCore.getOptions();
    JavaCore.setComplianceOptions(version, options);
    parser.setCompilerOptions(options);

    parseTimer.start();
    String source;

    try {
      source = Files.toString(file, Charsets.UTF_8);
      parser.setUnitName(file.getCanonicalPath());
      parser.setSource(source.toCharArray());
      parser.setIgnoreMethodBodies(ignoreMethodBody);
      return (CompilationUnit) parser.createAST(null);
    } catch (IOException e) {
      throw new JParserException(e);
    } finally {
      parseTimer.stop();
    }

  }

  private ParseResult buildCFA(CompilationUnit ast, Scope scope) throws JParserException {

    cfaTimer.start();

    CompilationUnit astNext;

    // AstDebugg checker = new AstDebugg(logger);
    // ast.accept(checker);

    CFABuilder builder = new CFABuilder(logger, scope);
    try {


      ast.accept(builder);

      String nextClassToBeParsed = builder.getScope().getNextClassPath();

      while (nextClassToBeParsed != null) {
        cfaTimer.stop();
        astNext = parseAdditionalClasses(nextClassToBeParsed);
        cfaTimer.start();
        if (astNext != null) {
          //astNext.accept(checker);
          astNext.accept(builder);
        }
        nextClassToBeParsed = builder.getScope().getNextClassPath();
      }

      DynamicBindingCreator tracker = new DynamicBindingCreator(builder);
      tracker.trackAndCreateDynamicBindings();

      return new ParseResult(builder.getCFAs(), builder.getCFANodes(), builder.getStaticFieldDeclarations(), Language.JAVA);
    } catch (CFAGenerationRuntimeException e) {
      throw new JParserException(e);
    } finally {
      cfaTimer.stop();
    }
  }

  private CompilationUnit parseAdditionalClasses(String pFileName) throws JParserException {
    String name = javaRootPath + pFileName;

    // There is a possibility that classes are in one and the same file
    // in that case, the files don't exist
    File file = new File(name);
    if (file.isFile()) {
      return parse(file);
    } else {
      return null;
    }
  }

  @Override
  public Timer getParseTime() {
    return parseTimer;
  }

  @Override
  public Timer getCFAConstructionTime() {
    return cfaTimer;
  }
}