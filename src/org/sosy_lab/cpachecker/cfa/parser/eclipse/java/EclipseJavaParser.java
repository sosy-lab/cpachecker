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

import com.google.common.base.Splitter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.Parser;
import org.sosy_lab.cpachecker.exceptions.JParserException;


/**
 * Wrapper around the JDT Parser and CFA-Builder Implementation.
 *
 */
@Options
class EclipseJavaParser implements Parser {

  @Option(secure=true, name ="java.encoding",
      description="use the following encoding for java files")
  private Charset encoding = StandardCharsets.UTF_8;

  @Option(secure=true, name ="java.version",
      description="Specifies the java version of source code accepted")
  private String version = JavaCore.VERSION_1_7;

  @Option(secure=true, name ="java.sourcepath",
      description="Specify the source code path to " +
          "search for java class or interface definitions")
  private String javaSourcepath = "";

  @Option(secure=true, name ="java.classpath",
      description="Specify the class code path to " +
          "search for java class or interface definitions")
  private String javaClasspath = "";

  @Option(secure=true, name="java.exportTypeHierarchy",
      description="export TypeHierarchy as .dot file")
  private boolean exportTypeHierarchy = true;

  @Option(secure=true, name="java.typeHierarchyFile",
      description="export TypeHierarchy as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportTypeHierarchyFile = Paths.get("typeHierarchy.dot");

  @SuppressWarnings("deprecation")
  private final ASTParser parser = ASTParser.newParser(AST.JLS4);

  private final LogManager logger;

  private final Timer parseTimer = new Timer();
  private final Timer cfaTimer = new Timer();

  private final String[] javaSourcePaths;
  private final String[] javaClassPaths;

  private static final boolean IGNORE_METHOD_BODY = true;
  private static final boolean PARSE_METHOD_BODY = false;
  private static final String JAVA_SOURCE_FILE_REGEX = ".*\\.java";

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
    List<String> resultList = new ArrayList<>();

    for (String path : Splitter.on(File.pathSeparator).trimResults().omitEmptyStrings().split(javaPath)) {
      Path directory = Paths.get(path);
      if (!Files.exists(directory)) {
        logger.log(Level.WARNING, "Path " + directory + " could not be found.");
      } else {
        resultList.add(directory.toAbsolutePath().toString());
      }
    }

    return resultList.toArray(new String[resultList.size()]);
  }

  /**
   * Parse the program of the Main class in this file into a CFA.
   *
   * @param mainClassName The Main Class File of the program to parse.
   * @return The CFA.
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
    if (exportTypeHierarchy && exportTypeHierarchyFile != null) {
      try (Writer w = MoreFiles.openOutputFile(exportTypeHierarchyFile, StandardCharsets.UTF_8)) {
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

    return new Scope(mainClassName, typeHierarchy, logger);
  }

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  private List<JavaFileAST> getASTsOfProgram() throws JParserException {
    Set<Path> sourceFileToBeParsed = getJavaFilesInSourcePaths();
    List<JavaFileAST> astsOfFoundFiles = new LinkedList<>();

    for (Path file : sourceFileToBeParsed) {
      String fileName = file.getFileName().toString();
      CompilationUnit ast = parse(file, IGNORE_METHOD_BODY);
      astsOfFoundFiles.add(new JavaFileAST(fileName, ast));
    }

    return astsOfFoundFiles;
  }

  private Set<Path> getJavaFilesInSourcePaths() {

    Set<Path> sourceFileToBeParsed = new HashSet<>();

    for (String path : javaSourcePaths) {
      sourceFileToBeParsed.addAll(getJavaFilesInPath(path));
    }

    return sourceFileToBeParsed;
  }

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  private Set<Path> getJavaFilesInPath(String path) {

    Path mainDirectory = Paths.get(path);

    assert Files.isDirectory(mainDirectory) : "Could not find directory at" + path;

    Set<Path> sourceFileToBeParsed = new HashSet<>();
    Queue<Path> directorysToBeSearched = new LinkedList<>();
    Set<Path> directorysReached = new HashSet<>();

    addDirectory(mainDirectory, directorysToBeSearched, directorysReached);

    while (!directorysToBeSearched.isEmpty()) {

      Path directory = directorysToBeSearched.poll();

      if (Files.isDirectory(directory)) {
        for (String fileName : directory.toFile().list()) {
          addFileWhereAppropriate(fileName, directory,
              sourceFileToBeParsed, directorysToBeSearched, directorysReached);
        }
      }
    }

    return sourceFileToBeParsed;
  }

  private void addFileWhereAppropriate(String fileName, Path directory,
      Set<Path> sourceFileToBeParsed, Queue<Path> directorysToBeSearched, Set<Path> pDirectorysReached) {

    Path file = directory.toAbsolutePath().resolve(fileName);

    if (fileName.matches(JAVA_SOURCE_FILE_REGEX)) {
      addJavaFile(file, sourceFileToBeParsed);
    } else if (Files.isDirectory(file)) {
      addDirectory(file, directorysToBeSearched, pDirectorysReached);
    }
  }

  private void addDirectory(Path file, Queue<Path> directorysToBeSearched, Set<Path> directorysReached) {
    if (Files.isDirectory(file) && !directorysReached.contains(file)) {
      directorysToBeSearched.add(file);
      directorysReached.add(file);
    } else {
      logger.log(Level.WARNING, "No permission to read directory " + file.getFileName() + ".");
    }
  }

  private void addJavaFile(Path file, Set<Path> sourceFileToBeParsed) {
    if (Files.isReadable(file) && !sourceFileToBeParsed.contains(file)) {
      sourceFileToBeParsed.add(file);
    } else {
      logger.log(Level.WARNING, "No permission to read java file " + file.getFileName() + ".");
    }
  }

  @Override
  public ParseResult parseString(String pFilename, String pCode) throws JParserException {

    throw new JParserException("Function not yet implemented");
  }

  private CompilationUnit parse(Path file) throws JParserException {
    return parse(file, PARSE_METHOD_BODY);
  }

  private CompilationUnit parse(Path file, boolean ignoreMethodBody) throws JParserException {

    parser.setEnvironment(javaClassPaths, javaSourcePaths, getEncodings(), false);
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
      source = MoreFiles.toString(file, encoding);
      parser.setUnitName(file.normalize().toString());
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
    Arrays.fill(encodings, encoding.name());
    return encodings;
  }

  private ParseResult buildCFA(CompilationUnit ast, Scope scope) throws JParserException {

    cfaTimer.start();

    // AstDebugg checker = new AstDebugg(logger);
    // ast.accept(checker);

    CFABuilder builder = new CFABuilder(logger, scope);
    try {

      ast.accept(builder);

      while (scope.hasLocalClassPending()) {
        AnonymousClassDeclaration nextLocalClassToBeParsed = scope.getNextLocalClass();
        nextLocalClassToBeParsed.accept(builder);
      }

      String nextClassToBeParsed = scope.getNextClass();
      while (nextClassToBeParsed != null) {

        Path classFile = searchForClassFile(nextClassToBeParsed);

        if (classFile != null) {

          cfaTimer.stop();
          CompilationUnit astNext = parse(classFile);
          cfaTimer.start();

          //astNext.accept(checker);
          astNext.accept(builder);
        }

        while (scope.hasLocalClassPending()) {
          AnonymousClassDeclaration nextLocalClassToBeParsed = scope.getNextLocalClass();
          nextLocalClassToBeParsed.accept(builder);
        }

        nextClassToBeParsed = scope.getNextClass();
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

      if (Files.exists(file)) {
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
