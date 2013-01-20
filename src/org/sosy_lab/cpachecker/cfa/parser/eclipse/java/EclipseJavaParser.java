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
import java.util.logging.Level;

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

  private static final boolean IGNORE_METHOD_BODY = true;
  private static final boolean PARSE_METHOD_BODY = false;
  private static final String JAVA_SOURCE_FILE_REGEX = ".*.java";
  private static final int JAVA_ROOT_PATH = 0;

  private static final int  LENGTH_OF_SUFFIX = 5;

  private final ASTParser parser = ASTParser.newParser(AST.JLS4);

  private final LogManager logger;

  private final Timer parseTimer;
  private final Timer cfaTimer;

  private final String encoding;
  private final String version;

  private final String[] javaSourcePaths;
  private final String[] javaClassPaths;

  public EclipseJavaParser(LogManager pLogger, String javaRootPath, String pEncoding, String pVersion,
      String javaSourcepath, String javaClasspath, Timer pParseTimer, Timer pCfaTimer) {
    logger = pLogger;
    encoding = pEncoding;
    version = pVersion;
    parseTimer = pParseTimer;
    cfaTimer = pCfaTimer;
    javaClassPaths = getJavaPaths(javaClasspath, javaRootPath);

    if (javaSourcepath.isEmpty()) {
      javaSourcePaths = javaClassPaths;
    } else {
      javaSourcePaths = getJavaPaths(javaSourcepath, javaRootPath);
    }
  }

  private String[] getJavaPaths(String javaPath, String javaRootPath) {

    if (javaPath.isEmpty()) {
      String[] result = { javaRootPath };
      return result;
    } else {
      String sourcepath = javaRootPath + File.pathSeparator + javaPath;
      String[] paths = sourcepath.split(File.pathSeparator);
      String[] result;

      if (existsNonExistingPath(paths)) {
        result = deleteNonExistingPaths(paths);
      } else {
        result = paths;
      }

      return result;
    }
  }

  private boolean existsNonExistingPath(String[] paths) {

    for (String path : paths) {
      if (!new File(path).exists()) { return true; }
    }
    return false;
  }

  private String[] deleteNonExistingPaths(String[] pSourcepaths) {

    LinkedList<String> resultList = new LinkedList<>();

    for (String path : pSourcepaths) {
      File directory = new File(path);
      if (directory.exists()) {
        resultList.add(path);
      } else {
        logger.log(Level.WARNING, "Path " + directory + "could not be found.");
      }
    }

    String[] result = new String[resultList.size()];

    int counter = 0;
    for (String path : resultList) {

      result[counter] = path;
      counter++;
    }

    return result;
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

    Map<String, String> typeOfFiles = new HashMap<>();
    Map<String, JClassOrInterfaceType> types
                            = getTypeHieachie(astsOfFoundFiles, typeOfFiles);

    String mainClassName = getMainClassName(FileName);

    return new Scope(mainClassName, types, typeOfFiles);
  }

  private String getMainClassName(String pFileName) {

    int pathLength = javaSourcePaths[JAVA_ROOT_PATH].length();

    return pFileName.substring(pathLength, pFileName.length() - LENGTH_OF_SUFFIX).replace(File.separatorChar, '.');
  }

  private Map<String, JClassOrInterfaceType> getTypeHieachie(List<Pair<CompilationUnit,
      String>> astsOfFoundFiles, Map<String, String> pTypeOfFiles) {

    Map<String, JClassOrInterfaceType> types = new HashMap<>();

    TypeHierachyCreator creator = new TypeHierachyCreator(logger, types, pTypeOfFiles);

    for (Pair<CompilationUnit, String> ast : astsOfFoundFiles) {
      creator.setFileOfCU(ast.getSecond());
      ast.getFirst().accept(creator);
    }

    return types;
  }

  private List<Pair<CompilationUnit, String>> getASTsOfProgram() throws JParserException {
    Queue<File> sourceFileToBeParsed = getJavaFilesInSourcePaths();
    List<Pair<CompilationUnit, String>> astsOfFoundFiles = new LinkedList<>();

    for (File file : sourceFileToBeParsed) {
      astsOfFoundFiles.add(Pair.of(parse(file, IGNORE_METHOD_BODY), file.getName()));
    }

    return astsOfFoundFiles;
  }

  private Queue<File> getJavaFilesInSourcePaths() throws JParserException {

    Queue<File> sourceFileToBeParsed = new LinkedList<>();

    for (String path : javaSourcePaths) {
      sourceFileToBeParsed.addAll(getJavaFilesInPath(path));
    }

    return sourceFileToBeParsed;
  }

  private Queue<File> getJavaFilesInPath(String path) throws JParserException {

    File mainDirectory = new File(path);
    assert mainDirectory.isDirectory() : "Could not find directory at" + path;

    Queue<File> sourceFileToBeParsed = new LinkedList<>();
    Queue<File> directorysToBeSearched = new LinkedList<>();
    directorysToBeSearched.add(mainDirectory);

    while (!directorysToBeSearched.isEmpty()) {

      File directory = directorysToBeSearched.poll();

      for (String fileName : directory.list()) {

        File file =
            new File(directory.getAbsolutePath() + File.separatorChar + fileName);

        if (fileName.matches(JAVA_SOURCE_FILE_REGEX)) {
          sourceFileToBeParsed.add(file);
        } else if (file.isDirectory()) {
          directorysToBeSearched.add(file);
        }
      }
    }

    return sourceFileToBeParsed;
  }

  @Override
  public ParseResult parseString(String pCode) throws JParserException {

    throw new JParserException("Function not yet implemented");
  }

  private CompilationUnit parse(File file) throws JParserException {
    return parse(file, PARSE_METHOD_BODY);
  }

  private CompilationUnit parse(File file, boolean ignoreMethodBody) throws JParserException {

    final String[] encodingList = getEncodings();

    parser.setEnvironment(javaClassPaths, javaSourcePaths, encodingList, false);
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

  private String[] getEncodings() {

    String[] encodings = new String[javaSourcePaths.length];

    for (int counter = 0; counter < encodings.length; counter++) {
      encodings[counter] = encoding;
    }

    return encodings;
  }

  private ParseResult buildCFA(CompilationUnit ast, Scope scope) throws JParserException {

    cfaTimer.start();

    // AstDebugg checker = new AstDebugg(logger);
    // ast.accept(checker);

    CFABuilder builder = new CFABuilder(logger, scope);
    try {

      ast.accept(builder);

      String nextClassToBeParsed = builder.getScope().getNextClass();

      while (nextClassToBeParsed != null) {

        File classFile = searchForClassFile(nextClassToBeParsed);

        if (classFile != null) {

          cfaTimer.stop();
          CompilationUnit astNext = parse(classFile);
          cfaTimer.start();

          //astNext.accept(checker);
          astNext.accept(builder);
        }

        nextClassToBeParsed = builder.getScope().getNextClass();
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

  private File searchForClassFile(String nextClassToBeParsed) {

    String classFilePathPart = nextClassToBeParsed.replace('.', File.separatorChar) + ".java";

    for (String sourcePath : javaSourcePaths) {
      File file = new File(sourcePath + classFilePathPart);

      if (file.exists()) {
        return file;
      }
    }

    return null;
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