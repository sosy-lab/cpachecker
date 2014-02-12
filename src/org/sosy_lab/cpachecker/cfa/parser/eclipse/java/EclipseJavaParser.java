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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.Parser;
import org.sosy_lab.cpachecker.exceptions.JParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

import com.google.common.base.Charsets;


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

  @Option(name="java.exportTypeHierarchy",
      description="export TypeHierarchy as .dot file")
  private boolean exportTypeHierarchy = true;

  @Option(name="java.typeHierarchyFile",
      description="export TypeHierarchy as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportTypeHierarchyFile = Paths.get("typeHierarchy.dot");

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
      Path directory = Paths.get(path);
      if (!directory.exists()) {
        logger.log(Level.WARNING, "Path " + directory + " could not be found.");
      } else if (!directory.canRead()) {
        logger.log(Level.WARNING, "Path " + directory + " can not be read.");
      } else {
        resultList.add(directory.toAbsolutePath().getPath());
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
    Path mainClassFile = getMainClassFile(mainClassName);
    Scope scope = prepareScope(mainClassName);
    ParseResult result = buildCFA(parse(mainClassFile), scope);
    exportTypeHierarchy(scope);
    return result;
  }

  private void exportTypeHierarchy(Scope pScope) {

    // write CFA to file
    if (exportTypeHierarchy) {
      try (Writer w = exportTypeHierarchyFile.asCharSink(Charsets.UTF_8).openStream()) {
        THDotBuilder.generateDOT(w, pScope);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e,
            "Could not write TypeHierarchy to dot file");
        // continue with analysis
      }
    }
  }

  private Path getMainClassFile(String mainClassName) throws JParserException  {

     Path mainClass = searchForClassFile(mainClassName);
     if (mainClass == null) {
       throw new JParserException("Could not find main class in the specified paths");
     }

     return mainClass;
  }

  private Scope prepareScope(String mainClassName) throws JParserException {

    List<JavaFileAST> astsOfFoundFiles = getASTsOfProgram();

    TypeHierarchy typeHierarchy = TypeHierarchy.createTypeHierachy(logger, astsOfFoundFiles);

    return new Scope(mainClassName, typeHierarchy);
  }

  private List<JavaFileAST> getASTsOfProgram() throws JParserException {
    Set<Path> sourceFileToBeParsed = getJavaFilesInSourcePaths();
    List<JavaFileAST> astsOfFoundFiles = new LinkedList<>();

    for (Path file : sourceFileToBeParsed) {
      String fileName = file.getName();
      CompilationUnit ast = parse(file, IGNORE_METHOD_BODY);
      astsOfFoundFiles.add(new JavaFileAST(fileName, ast));
    }

    return astsOfFoundFiles;
  }

  private Set<Path> getJavaFilesInSourcePaths() throws JParserException {

    Set<Path> sourceFileToBeParsed = new HashSet<>();

    for (String path : javaSourcePaths) {
      sourceFileToBeParsed.addAll(getJavaFilesInPath(path));
    }



    return sourceFileToBeParsed;
  }

  private Set<Path> getJavaFilesInPath(String path) throws JParserException {

    Path mainDirectory = Paths.get(path);

    assert mainDirectory.isDirectory() : "Could not find directory at" + path;

    Set<Path> sourceFileToBeParsed = new HashSet<>();
    Queue<Path> directorysToBeSearched = new LinkedList<>();
    Set<Path> directorysReached = new HashSet<>();

    addDirectory(mainDirectory, directorysToBeSearched, directorysReached);

    while (!directorysToBeSearched.isEmpty()) {

      Path directory = directorysToBeSearched.poll();

      if (directory.exists() && directory.canRead()) {
        for (String fileName : directory.list()) {
          addFileWhereAppropriate(fileName, directory,
              sourceFileToBeParsed, directorysToBeSearched, directorysReached);
        }
      }
    }

    return sourceFileToBeParsed;
  }

  private void addFileWhereAppropriate(String fileName, Path directory,
      Set<Path> sourceFileToBeParsed, Queue<Path> directorysToBeSearched, Set<Path> pDirectorysReached) {

    Path file =
        Paths.get(directory.getAbsolutePath(), fileName);

    if (fileName.matches(JAVA_SOURCE_FILE_REGEX)) {
      addJavaFile(file, sourceFileToBeParsed);
    } else if (file.isDirectory()) {
      addDirectory(file, directorysToBeSearched, pDirectorysReached);
    }
  }

  private void addDirectory(Path file, Queue<Path> directorysToBeSearched, Set<Path> directorysReached) {
    if (file.exists() && file.canRead() && !directorysReached.contains(file)) {
      directorysToBeSearched.add(file);
      directorysReached.add(file);
    } else {
      logger.log(Level.WARNING, "No permission to read directory " + file.getName() + ".");
    }
  }

  private void addJavaFile(Path file, Set<Path> sourceFileToBeParsed) {
    if (file.exists() && file.canRead() && !sourceFileToBeParsed.contains(file)) {
      sourceFileToBeParsed.add(file);
    } else {
      logger.log(Level.WARNING, "No permission to read java file ");
      logger.log(Level.WARNING, file.getName());
      logger.log(Level.WARNING, ".");
    }
  }

  @Override
  public ParseResult parseString(String pCode) throws JParserException {

    throw new JParserException("Function not yet implemented");
  }

  private CompilationUnit parse(Path file) throws JParserException {
    return parse(file, PARSE_METHOD_BODY);
  }

  private CompilationUnit parse(Path file, boolean ignoreMethodBody) throws JParserException {

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
      source = file.asCharSource(Charsets.UTF_8).read();
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

        Path classFile = searchForClassFile(nextClassToBeParsed);

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

  private Path searchForClassFile(String nextClassToBeParsed) {

    String classFilePathPart = nextClassToBeParsed.replace('.', File.separatorChar) + ".java";

    for (String sourcePath : javaSourcePaths) {
      Path file = Paths.get(sourcePath, classFilePathPart);

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

  public static final class JavaFileAST {

    private final  String fileName;

    private final  CompilationUnit ast;

    public JavaFileAST(String pFileName, CompilationUnit pAst) {
      fileName = pFileName;
      ast = pAst;
    }

    public CompilationUnit getAst() {
      return ast;
    }

    public String getFileName() {
      return fileName;
    }
  }


}