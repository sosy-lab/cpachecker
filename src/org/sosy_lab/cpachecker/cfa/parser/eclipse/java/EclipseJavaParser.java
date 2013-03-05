/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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
@Options
public class EclipseJavaParser implements Parser {

  @Option(name ="java.encoding",
      description="use the following encoding for java files")
  private String encoding = "utf8";

  @Option(name ="java.version",
      description="Specifies the java version of source code accepted")
  private String version = JavaCore.VERSION_1_7;

  @Option(name ="java.sourcepath",
      description="Specify the source code path to " +
          "search for java class or interface definitions")
  private String javaSourcepath = "";

  @Option(name ="java.classpath",
      description="Specify the class code path to " +
          "search for java class or interface definitions")
  private String javaClasspath = "";

  private final ASTParser parser = ASTParser.newParser(AST.JLS4);

  private final LogManager logger;

  private final Timer parseTimer = new Timer();
  private final Timer cfaTimer = new Timer();

  private final String[] javaSourcePaths;
  private final String[] javaClassPaths;

  private static final boolean IGNORE_METHOD_BODY = true;
  private static final boolean PARSE_METHOD_BODY = false;
  private static final String JAVA_SOURCE_FILE_REGEX = ".*.java";

  public EclipseJavaParser(LogManager pLogger, Configuration config) throws InvalidConfigurationException {

    config.inject(this);

    logger = pLogger;

    javaClassPaths = getJavaPaths(javaClasspath);

    if (javaSourcepath.isEmpty()) {
      javaSourcePaths = javaClassPaths;
    } else {
      javaSourcePaths = getJavaPaths(javaSourcepath);
    }

    if (javaSourcePaths.length == 0) {
      throw new InvalidConfigurationException("No valid Paths could be found.");
    }
  }

  private String[] getJavaPaths(String javaPath) {

      String[] paths = javaPath.split(File.pathSeparator);
      String[] result;

      result = deleteNonValidPaths(paths);

      return result;
  }

  private String[] deleteNonValidPaths(String[] pSourcepaths) {

    LinkedList<String> resultList = new LinkedList<>();

    for (String path : pSourcepaths) {
      File directory = new File(path);
      if (!directory.exists()) {
        logger.log(Level.WARNING, "Path " + directory + " could not be found.");
      } else if (!directory.canRead()) {
        logger.log(Level.WARNING, "Path " + directory + " can not be read.");
      } else {
        resultList.add(directory.getAbsolutePath());
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
   * @throws ParserException If parser or CFA builder cannot handle the  code.
   */
  @Override
  public ParseResult parseFile(String mainClassName) throws JParserException {
    File mainClassFile = getMainClassFile(mainClassName);
    Scope scope = prepareScope(mainClassName);
    return buildCFA(parse(mainClassFile), scope);
  }

  private File getMainClassFile(String mainClassName) throws JParserException  {

     File mainClass = searchForClassFile(mainClassName);
     if (mainClass == null) {
       throw new JParserException("Could not find main class in the specified paths");
     }

     return mainClass;
  }

  private Scope prepareScope(String mainClassName) throws JParserException {

    List<Pair<CompilationUnit, String>> astsOfFoundFiles = getASTsOfProgram();

    Map<String, String> typeOfFiles = new HashMap<>();
    Map<String, JClassOrInterfaceType> types
                            = getTypeHieachie(astsOfFoundFiles, typeOfFiles);

    return new Scope(mainClassName, types, typeOfFiles);
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

    addDirectory(mainDirectory, directorysToBeSearched);

    while (!directorysToBeSearched.isEmpty()) {

      File directory = directorysToBeSearched.poll();

      if (directory.exists() && directory.canRead()) {
        for (String fileName : directory.list()) {
          addFileWhereAppropriate(fileName, directory,
              sourceFileToBeParsed, directorysToBeSearched);
        }
      }
    }

    return sourceFileToBeParsed;
  }

  private void addFileWhereAppropriate(String fileName, File directory,
      Queue<File> sourceFileToBeParsed, Queue<File> directorysToBeSearched) {

    File file =
        new File(directory.getAbsolutePath() + File.separatorChar + fileName);

    if (fileName.matches(JAVA_SOURCE_FILE_REGEX)) {
      addJavaFile(file, sourceFileToBeParsed);
    } else if (file.isDirectory()) {
      addDirectory(file, directorysToBeSearched);
    }
  }

  private void addDirectory(File file, Queue<File> directorysToBeSearched) {
    if (file.exists() && file.canRead()) {
      directorysToBeSearched.add(file);
    } else {
      logger.log(Level.WARNING, "No permission to read directory " + file.getName() + ".");
    }
  }

  private void addJavaFile(File file, Queue<File> sourceFileToBeParsed) {
    if (file.exists() && file.canRead()) {
      sourceFileToBeParsed.add(file);
    } else {
      logger.log(Level.WARNING, "No permission to read java file "
                                  + file.getName() + ".");
    }
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
      File file = new File(sourcePath + File.separatorChar + classFilePathPart);

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