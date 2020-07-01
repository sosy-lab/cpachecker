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
import com.google.common.collect.ImmutableList;
import com.google.common.io.MoreFiles;
import com.google.errorprone.annotations.MustBeClosed;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
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
  // Make sure to keep the option name synchronized with CPAMain#areJavaOptionsSet
  private String javaSourcepath = "";

  @Option(secure=true, name ="java.classpath",
      description="Specify the class code path to " +
          "search for java class or interface definitions")
  // Make sure to keep the option name synchronized with CPAMain#areJavaOptionsSet
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

  private final ImmutableList<Path> javaSourcePaths;
  private final ImmutableList<Path> javaClassPaths;

  private final List<Path> parsedFiles = new ArrayList<>();

  private static final boolean IGNORE_METHOD_BODY = true;
  private static final boolean PARSE_METHOD_BODY = false;
  static final String JAVA_SOURCE_FILE_EXTENSION = ".java";

  public EclipseJavaParser(LogManager pLogger, Configuration config) throws InvalidConfigurationException {

    config.inject(this);

    logger = pLogger;

    javaClassPaths = getJavaPaths(javaClasspath);

    if (javaSourcepath.isEmpty()) {
      javaSourcePaths = javaClassPaths;
    } else {
      javaSourcePaths = getJavaPaths(javaSourcepath);
    }

    if (javaSourcePaths.isEmpty()) {
      throw new InvalidConfigurationException("No valid Paths could be found.");
    }
  }

  private ImmutableList<Path> getJavaPaths(String javaPath) {
    ImmutableList.Builder<Path> result = ImmutableList.builder();

    for (String path : Splitter.on(File.pathSeparator).trimResults().omitEmptyStrings().split(javaPath)) {
      Path directory = Paths.get(path);
      if (!Files.exists(directory)) {
        logger.log(Level.WARNING, "Path", directory, "could not be found.");
      } else {
        result.add(directory);
      }
    }

    return result.build();
  }

  /**
   * Parse the program of the Main class in this file into a CFA.
   *
   * @param mainClassName The Main Class File of the program to parse.
   * @return The CFA.
   */
  @Override
  public ParseResult parseFile(String mainClassName) throws JParserException, IOException {
    Path mainClassFile =
        searchForClassFile(mainClassName)
            .orElseThrow(
                () -> new JParserException("Could not find main class in the specified paths"));
    Scope scope = prepareScope(mainClassName);
    ParseResult result = buildCFA(parse(mainClassFile), scope);
    exportTypeHierarchy(scope);
    return result;
  }

  private void exportTypeHierarchy(Scope pScope) {

    // write CFA to file
    if (exportTypeHierarchy && exportTypeHierarchyFile != null) {
      try (Writer w = IO.openOutputFile(exportTypeHierarchyFile, StandardCharsets.UTF_8)) {
        THDotBuilder.generateDOT(w, pScope);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e,
            "Could not write TypeHierarchy to dot file");
        // continue with analysis
      }
    }
  }

  private Scope prepareScope(String mainClassName) throws JParserException, IOException {

    List<JavaFileAST> astsOfFoundFiles = getASTsOfProgram();

    TypeHierarchy typeHierarchy = TypeHierarchy.createTypeHierachy(logger, astsOfFoundFiles);

    return new Scope(mainClassName, typeHierarchy, logger);
  }

  private List<JavaFileAST> getASTsOfProgram() throws IOException {
    List<JavaFileAST> astsOfFoundFiles = new ArrayList<>();

    for (Path directory : javaSourcePaths) {
      try (Stream<Path> files = getJavaFilesInPath(directory)) {
        for (Path file : files.collect(Collectors.toList())) {
          CompilationUnit ast = parse(file, IGNORE_METHOD_BODY);
          astsOfFoundFiles.add(new JavaFileAST(file, ast));
        }
      }
    }

    return astsOfFoundFiles;
  }

  @MustBeClosed
  @SuppressWarnings("StreamResourceLeak") // https://github.com/google/error-prone/issues/893
  private Stream<Path> getJavaFilesInPath(Path mainDirectory) throws IOException {
    return Files.walk(mainDirectory, FileVisitOption.FOLLOW_LINKS)
        .filter(Files::isRegularFile)
        .filter(file -> file.toString().endsWith(JAVA_SOURCE_FILE_EXTENSION))
        .filter(
            file -> {
              if (Files.isReadable(file)) {
                return true;
              }
              logger.log(Level.WARNING, "No permission to read java file %s.", file);
              return false;
            });
  }

  @Override
  public ParseResult parseString(String pFilename, String pCode) throws JParserException {

    throw new JParserException("Function not yet implemented");
  }

  private CompilationUnit parse(Path file) throws IOException {
    return parse(file, PARSE_METHOD_BODY);
  }

  private CompilationUnit parse(Path file, boolean ignoreMethodBody) throws IOException {
    parsedFiles.add(file);

    String[] encodings =
        Collections.nCopies(javaSourcePaths.size(), encoding.name()).toArray(new String[0]);
    parser.setEnvironment(asStrings(javaClassPaths), asStrings(javaSourcePaths), encodings, false);
    parser.setResolveBindings(true);
    parser.setStatementsRecovery(true);
    parser.setBindingsRecovery(true);

    // Set Compliance Options to support Version
    @SuppressWarnings("unchecked")
    Map<String, String> options = JavaCore.getOptions();
    JavaCore.setComplianceOptions(version, options);
    parser.setCompilerOptions(options);

    parseTimer.start();

    try {
      parser.setUnitName(file.normalize().toString());
      parser.setSource(IO.toCharArray(MoreFiles.asCharSource(file, encoding)));
      parser.setIgnoreMethodBodies(ignoreMethodBody);
      return (CompilationUnit) parser.createAST(null);
    } finally {
      parseTimer.stop();
    }
  }

  private String[] asStrings(List<Path> files) {
    return files.stream().map(Path::toString).toArray(String[]::new);
  }

  private ParseResult buildCFA(CompilationUnit ast, Scope scope)
      throws IOException, JParserException {

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

        Optional<Path> classFile = searchForClassFile(nextClassToBeParsed);

        if (classFile.isPresent()) {

          cfaTimer.stop();
          CompilationUnit astNext = parse(classFile.orElseThrow());
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

      return new ParseResult(
          builder.getCFAs(),
          builder.getCFANodes(),
          builder.getStaticFieldDeclarations(),
          parsedFiles);
    } catch (CFAGenerationRuntimeException e) {
      throw new JParserException(e);
    } finally {
      cfaTimer.stop();
    }
  }

  private Optional<Path> searchForClassFile(String nextClassToBeParsed) {
    String classFilePathPart = nextClassToBeParsed.replace('.', File.separatorChar) + ".java";

    return javaSourcePaths
        .stream()
        .map(sourcePath -> sourcePath.resolve(classFilePathPart))
        .filter(Files::exists)
        .findFirst();
  }

  @Override
  public Timer getParseTime() {
    return parseTimer;
  }

  @Override
  public Timer getCFAConstructionTime() {
    return cfaTimer;
  }

  static final class JavaFileAST {

    private final Path file;

    private final  CompilationUnit ast;

    public JavaFileAST(Path pFile, CompilationUnit pAst) {
      file = pFile;
      ast = pAst;
    }

    public CompilationUnit getAst() {
      return ast;
    }

    public Path getFile() {
      return file;
    }
  }
}
