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

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.Parser;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.exceptions.JParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/**
 * Wrapper around the JDT Parser and CFA-Builder Implementation.
 *
 */
public class EclipseJavaParser implements Parser {

  private static final int START_OF_STRING = 0;
  private static final boolean IGNORE_METHOD_BODY = true;
  private static final boolean PARSE_METHOD_BODY = false;
  private static final String JAVA_SOURCE_FILE_REGEX = ".*.java";

  private final ASTParser parser = ASTParser.newParser(AST.JLS4);


  private final LogManager logger;

  private final Timer parseTimer = new Timer();
  private final Timer cfaTimer = new Timer();

  public EclipseJavaParser(LogManager pLogger) {
    logger = pLogger;
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
  public ParseResult parseFile(String filePath) throws JParserException {
    File mainClassFile = getMainClassFile(filePath);
    Scope scope = prepareScope(filePath, mainClassFile);
    return buildCFA(parse(mainClassFile, scope.getRootPath()), scope);
  }

  private File getMainClassFile(String filePath)  {
    File file = new File(filePath);
    return file;
  }

  private Scope prepareScope(String filePath, File mainClassFile) throws JParserException {

    // Find rootPath of Project, which is assumed to be the directory above
    // the top-level package directory of the given main class
    String fileName = filePath.substring(filePath.lastIndexOf(File.separatorChar) + 1, filePath.length());

    String packageOfMainClass = getPackageOfMainClass(mainClassFile);
    String qualifiedMainClassName = getQualifiedMainClassName(filePath, packageOfMainClass, fileName);
    String rootPath = getRootPath(filePath, packageOfMainClass, fileName);

    List<Pair<CompilationUnit,String>> astsOfFoundFiles = getASTsOfProgram(rootPath);

    Map<String, String> typeOfFiles = new HashMap<String,String>();
    Map<String, JClassOrInterfaceType> types = getTypeHieachie(astsOfFoundFiles, typeOfFiles);


    return new Scope(qualifiedMainClassName, rootPath, types, typeOfFiles);
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

  private List<Pair<CompilationUnit, String>> getASTsOfProgram(String rootPath) throws JParserException {
    Queue<File> sourceFileToBeParsed = getProgramFilesInRootPath(rootPath);
    List<Pair<CompilationUnit,String>> astsOfFoundFiles = new LinkedList<Pair<CompilationUnit,String>>();

    for (File file : sourceFileToBeParsed) {
      astsOfFoundFiles.add(Pair.of(parse(file, IGNORE_METHOD_BODY, rootPath), file.getName()));
    }

    return astsOfFoundFiles;
  }

  private String getRootPath(String filePath, String packageOfMainClass, String fileName) {
    if (packageOfMainClass.length() == 0) {
      return filePath.substring(START_OF_STRING, filePath.length() - fileName.length());
    } else {
      return filePath.substring(START_OF_STRING, filePath.length() - packageOfMainClass.length() - fileName.length()
          - 1);
    }
  }

  private String getQualifiedMainClassName(String filePath, String packageOfMainClass, String fileName) {
    if (packageOfMainClass.length() != 0) {
      return packageOfMainClass + "." + fileName.substring(START_OF_STRING, fileName.length() - 5);
    } else {
      return fileName.substring(START_OF_STRING, fileName.length() - 5);
    }
  }

  private String getPackageOfMainClass(File mainClassFile) throws JParserException {

    String packageString = "";
    CompilationUnit ast = parse(mainClassFile, IGNORE_METHOD_BODY);

    if (ast.getPackage() != null) {
      packageString = ast.getPackage().getName().getFullyQualifiedName();
    }
    return packageString;
  }

  @Override
  public ParseResult parseString(String pCode) throws JParserException {

    throw new JParserException("Function not yet implemented");
  }

  private CompilationUnit parse(File file, String rootPath) throws JParserException {
    return parse(file, PARSE_METHOD_BODY, rootPath);
  }

  private Queue<File>
                  getProgramFilesInRootPath(String rootPath) throws JParserException {

    File mainDirectory = new File(rootPath);
    assert mainDirectory.isDirectory() : "Could not find main directory at" + rootPath;

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

  private CompilationUnit parse(File file, boolean ignoreMethodBody, String rootPath) throws JParserException {

    final String[] sourceFilePath = new String[1];
    sourceFilePath[0] = rootPath;

    final String[] encoding = { "utf8" };

    parser.setEnvironment(null, sourceFilePath, encoding, false);
    parser.setResolveBindings(true);
    parser.setStatementsRecovery(true);
    parser.setBindingsRecovery(true);


    // Set Compliance Options to support JDK 1.7
    @SuppressWarnings("unchecked")
    Hashtable<String, String> options = JavaCore.getOptions();
    JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, options);
    parser.setCompilerOptions(options);

    return parse(file, ignoreMethodBody);

  }

  private CompilationUnit parse(File file, boolean ignoreMethodBody) throws JParserException {

    parseTimer.start();
    String source;
    try {
      source = FileUtils.readFileToString(file);
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
        astNext = parseAdditionalClasses(nextClassToBeParsed, scope.getRootPath());
        cfaTimer.start();
        if (astNext != null) {
          //astNext.accept(checker);
          astNext.accept(builder);
        }
        nextClassToBeParsed = builder.getScope().getNextClassPath();
      }

      DynamicBindingCreator tracker = new DynamicBindingCreator(builder);
      tracker.trackAndCreateDynamicBindings();

      return new ParseResult(builder.getCFAs(), builder.getCFANodes(), builder.getStaticFieldDeclarations());
    } catch (CFAGenerationRuntimeException e) {
      throw new JParserException(e);
    } finally {
      cfaTimer.stop();
    }
  }

  private CompilationUnit parseAdditionalClasses(String pFileName, String rootPath) throws JParserException {
    String name = rootPath + pFileName;

    // There is a possibility that classes are in one and the same file
    // in that case, the files don't exist
    File file = new File(name);
    if (file.isFile()) {
      return parse(file, rootPath);
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