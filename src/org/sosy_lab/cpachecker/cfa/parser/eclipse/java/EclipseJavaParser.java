// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

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
import org.sosy_lab.cpachecker.cfa.JavaParser;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.exceptions.JParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/**
 * Wrapper around the JDT Parser and CFA-Builder Implementation.
 */
@Options
class EclipseJavaParser implements JavaParser {

  @Option(
      secure = true,
      name = "java.encoding",
      description = "use the following encoding for java files")
  private Charset encoding = StandardCharsets.UTF_8;

  @Option(
      secure = true,
      name = "java.version",
      description = "Specifies the java version of source code accepted")
  private String version = JavaCore.VERSION_1_7;

  @Option(
      secure = true,
      name = "java.sourcepath",
      description =
          "Specify the source code path to " + "search for java class or interface definitions")
  // Make sure to keep the option name synchronized with CPAMain#areJavaOptionsSet
  private String javaSourcepath = "";

  @Option(
      secure = true,
      name = "java.classpath",
      description =
          "Specify the class code path to " + "search for java class or interface definitions")
  // Make sure to keep the option name synchronized with CPAMain#areJavaOptionsSet
  private String javaClasspath = "";

  @Option(
      secure = true,
      name = "java.exportTypeHierarchy",
      description = "export TypeHierarchy as .dot file")
  private boolean exportTypeHierarchy = true;

  @Option(
      secure = true,
      name = "java.typeHierarchyFile",
      description = "export TypeHierarchy as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportTypeHierarchyFile = Paths.get("typeHierarchy.dot");

  @SuppressWarnings("deprecation")
  private final ASTParser parser = ASTParser.newParser(AST.JLS4);

  private final LogManager logger;

  private final Timer parseTimer = new Timer();
  private final Timer cfaTimer = new Timer();

  private final String entryMethod;

  private ImmutableList<Path> javaSourcePaths;
  private ImmutableList<Path> javaClassPaths;

  private final List<Path> parsedFiles = new ArrayList<>();

  private static final boolean IGNORE_METHOD_BODY = true;
  private static final boolean PARSE_METHOD_BODY = false;
  static final String JAVA_SOURCE_FILE_EXTENSION = ".java";

  public EclipseJavaParser(LogManager pLogger, Configuration config, String pEntryMethod)
      throws InvalidConfigurationException {

    config.inject(this);
    logger = pLogger;
    entryMethod = pEntryMethod;

    if (!javaSourcepath.isEmpty() && javaClasspath.isEmpty()) {
      javaClasspath = javaSourcepath;
    }

    if (!javaClasspath.isEmpty()) {
      javaClassPaths = convertToPathList(javaClasspath);

      if (javaSourcepath.isEmpty()) {
        javaSourcePaths = javaClassPaths;
      } else {
        javaSourcePaths = convertToPathList(javaSourcepath);
      }

      if (javaSourcePaths.isEmpty()) {
        throw new InvalidConfigurationException("No valid Paths could be found.");
      }
    }
  }

  private void setMissingClassPath(List<String> sourceFiles) throws InvalidConfigurationException {

    if (sourceFiles.size() == 1 && sourceFiles.get(0).endsWith(".java")) {
      ImmutableList<Path> pathToProgram = convertToPathList(sourceFiles.get(0));
      javaClassPaths = ImmutableList.of(pathToProgram.get(0).getParent());
    } else {
      javaClassPaths =
          transformedImmutableListCopy(sourceFiles, v -> {
            assert v != null;
            return Paths.get(v);
          });
    }
    if (javaSourcepath.isEmpty()) {
      javaSourcePaths = javaClassPaths;
    } else {
      javaSourcePaths = convertToPathList(javaSourcepath);
    }

    if (javaSourcePaths.isEmpty()) {
      throw new InvalidConfigurationException("No valid Paths could be found.");
    }
  }

  /**
   * Converts a string with a path or multiple paths to files separated by a path separator to an
   * immutable list of Paths. Path separator in Linux is ':'
   *
   * @param javaPath String of paths to java files, separated by path separator
   * @return Immutable list of Paths of files in input string
   */
  private ImmutableList<Path> convertToPathList(String javaPath) {
    ImmutableList.Builder<Path> result = ImmutableList.builder();

    for (String pathAsString : Splitter.on(File.pathSeparator).trimResults().omitEmptyStrings()
        .split(javaPath)) {
      Path path = Paths.get(pathAsString);
      if (!Files.exists(path)) {
        logger.log(Level.WARNING, "Path", path, "could not be found.");
      } else {
        result.add(path);
      }
    }

    return result.build();
  }

  @Override
  public ParseResult parseFile(List<String> sourceFiles)
      throws ParserException, IOException, InvalidConfigurationException {
    if (javaClasspath.isEmpty()) {
      setMissingClassPath(sourceFiles);
      return parseFile(entryMethod);
    } else {
      return parseFile(sourceFiles.get(0));
    }

  }

  /**
   * Parse the program of the Main class in this file into a CFA.
   *
   * @param entryFunction The Main Class File of the program to parse.
   * @return The CFA.
   */
  @Override
  public ParseResult parseFile(String entryFunction) throws JParserException, IOException {
    String mainClassAbsolutePath = getAbsolutePathToEntryFunction(entryFunction) + ".java";
    String mainClassFile = stripMethodNameFromEntryFunction(entryFunction);
    Scope scope = prepareScope(mainClassFile);
    ParseResult result = buildCFA(parse(getPathToFile(mainClassAbsolutePath)), scope);
    exportTypeHierarchy(scope);
    return result;
  }

  private String stripMethodNameFromEntryFunction(String mainFunctionName) {
    Optional<Path> mainClassFile = searchForClassFile(mainFunctionName);
    while (mainClassFile.isEmpty() && !mainFunctionName.isEmpty()) {
      if (mainFunctionName.contains(".")) {
        mainFunctionName = removeFromStringEverythingAfterLastOccurrenceOf(mainFunctionName, ".");
        mainClassFile = searchForClassFile(mainFunctionName);
      } else {
        break;
      }
    }

    return mainFunctionName;
  }

  private static String removeFromStringEverythingAfterLastOccurrenceOf(
      String string, final String separator) {
    int indexOfLastString = string.lastIndexOf(separator);
    if (indexOfLastString >= 0) {
      return string.substring(0, indexOfLastString);
    } else {
      return string;
    }
  }

  /**
   * Returns the path of a file using javaSourcePaths variable.
   *
   * @param fileName Name of the file
   * @return path to file
   * @throws JParserException is thrown if file is not found
   */
  private Path getPathToFile(String fileName) throws JParserException {
    Path mainClassFile;
    if (!fileName.endsWith(".java")) {
      mainClassFile =
          searchForClassFile(fileName)
              .orElseThrow(
                  () -> new JParserException("Could not find main class in the specified paths"));
    } else {
      mainClassFile = Paths.get(fileName); // TODO check for file exists
    }
    return mainClassFile;
  }

  /**
   * Splits the path to an entry point into path to class and entry method. If the entry point
   * method is not given, method will return default entry method. JavaClassPaths has to be set for
   * this method to work!
   *
   * @param entryFunctionPath path to entry method
   * @return Array with first element being absolute path to class, second element relative path and
   *     third element entry method
   */
  private String getAbsolutePathToEntryFunction(String entryFunctionPath) throws JParserException {
    if (entryFunctionPath.endsWith(JAVA_SOURCE_FILE_EXTENSION)) {
      entryFunctionPath =
          entryFunctionPath.substring(
              0, entryFunctionPath.length() - JAVA_SOURCE_FILE_EXTENSION.length());
    }
    // TODO check if replacing with slash works for windows
    entryFunctionPath = entryFunctionPath.replaceAll("\\.", "/");
    Optional<String> mainClassAbsolutePath =
        findMethodInPathList(javaSourcePaths, entryFunctionPath);
    if (mainClassAbsolutePath.isEmpty()) {
      mainClassAbsolutePath = findMethodInPathList(javaClassPaths, entryFunctionPath);
    }
    String finalEntryFunctionPath = entryFunctionPath;
    return mainClassAbsolutePath.orElseThrow(
        () ->
            new JParserException(
                "Could not find entry point. JavaClassPaths: "
                    + javaClassPaths
                    + " EntryFunctionPath: "
                    + finalEntryFunctionPath));
  }

  private Optional<String> findMethodInPathList(List<Path> pPathList, String pMainFunctionPath) {
    for (Path path : pPathList) {
      // In case only file without method name is given
      Path pathToFile = path.resolve(pMainFunctionPath + JAVA_SOURCE_FILE_EXTENSION);
      if (Files.exists(pathToFile)) {
        return Optional.of(path.resolve(pMainFunctionPath).toString());
      }
      // In case file and method name is given
      int indexOfLastSlash = pMainFunctionPath.lastIndexOf('/');
      if (indexOfLastSlash >= 0) {
        Path pathToFileParent =
            path.resolve(
                pMainFunctionPath.substring(0, indexOfLastSlash) + JAVA_SOURCE_FILE_EXTENSION);
        if (Files.exists(pathToFileParent)) {
          return Optional.of(
              path.resolve(pMainFunctionPath.substring(0, indexOfLastSlash)).toString());
        }
      }
    }
    return Optional.empty();
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

  private Scope prepareScope(String mainClassFileName) throws JParserException, IOException {

    List<JavaFileAST> astsOfFoundFiles = getASTsOfProgram();

    TypeHierarchy typeHierarchy = TypeHierarchy.createTypeHierachy(logger, astsOfFoundFiles);

    return new Scope(mainClassFileName, typeHierarchy, logger);
  }

  private List<JavaFileAST> getASTsOfProgram() throws IOException {
    List<JavaFileAST> astsOfFoundFiles = new ArrayList<>();
    List<Path> alreadyWalkedPaths = new ArrayList<>();
    for (Path directory : javaSourcePaths) {
      try (Stream<Path> files = getJavaFilesInPath(directory)) {
        for (Path filePath : files.collect(ImmutableList.toImmutableList())) {
          if (alreadyWalkedPaths.contains(filePath)) {
            continue;
          }
          CompilationUnit ast = parse(filePath, IGNORE_METHOD_BODY);
          astsOfFoundFiles.add(new JavaFileAST(filePath, ast));
          alreadyWalkedPaths.add(filePath);
        }
      }
    }
    return astsOfFoundFiles;
  }

  @MustBeClosed
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
    if (!parsedFiles.contains(file)) {
      parsedFiles.add(file);
    }
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

  /**
   * Search the path of a class file, checks if it exists and returns an Optional of class path.
   * Uses using javaSourcePaths variable, thus it must be set.
   *
   * @param nextClassToBeParsed Name of the class to be parsed, without its path and file ending
   * @return {@code Optional<Path>} to file. Empty if file does not exist
   */
  private Optional<Path> searchForClassFile(String nextClassToBeParsed) {
    String classFilePathPart = nextClassToBeParsed.replace('.', File.separatorChar) + ".java";

    return javaSourcePaths.stream()
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
