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
import java.util.Map;
import java.util.Queue;

import org.apache.commons.io.FileUtils;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.CFAGenerationRuntimeException;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/**
 * Base implementation that should work with all CDT versions we support.
 */
public  class EclipseJavaParser implements CParser {

  private static final int START_OF_STRING = 0;
  private static final boolean IGNORE_METHOD_BODY = true;
  private static final boolean PARSE_METHOD_BODY = false;
  private static final String JAVA_SOURCE_FILE_REGEX = ".*.java";
  private final ASTParser  parser = ASTParser.newParser(AST.JLS4);

  //TODO Prototype, think about how root Path can be smoothly given to builder
  private String rootPath;

  private boolean ignoreCasts;


  private final LogManager logger;

  private final Timer parseTimer = new Timer();
  private final Timer cfaTimer = new Timer();
  private String qualifiedNameOfMainClass;
  //private String unitName;



  public EclipseJavaParser(LogManager pLogger) {
    logger = pLogger;
  }

  @Override
  public ParseResult parseFile(String pFilename) throws ParserException, IOException {

    return buildCFA(parse(pFilename));
  }

  @Override
  public ParseResult parseString(String pCode) throws ParserException {

    return buildCFA(parse((pCode)));
  }

  @Override
  public org.sosy_lab.cpachecker.cfa.ast.c.CAstNode parseSingleStatement(String pCode) throws ParserException {
      throw new ParserException("Not Implemented");
  }

  @SuppressWarnings("unchecked")
  private CompilationUnit parse(final String pFileName) throws ParserException {
    parseTimer.start();

    try {

      File file = new File(pFileName);



      CompilationUnit ast = parseOnlyDeclarations(file);

      //Find rootPath of Project, which is assumed to be the directory above
      // the top-level package directory of the given main class
      String fileName = pFileName.substring(pFileName.lastIndexOf(File.separatorChar) + 1 , pFileName.length());

      String packageString = "";
      if(ast.getPackage() != null){
        packageString = ast.getPackage().getName().getFullyQualifiedName();
        qualifiedNameOfMainClass = packageString + "." + fileName.substring(START_OF_STRING , fileName.length() - 5);
      } else {
        qualifiedNameOfMainClass = fileName.substring(START_OF_STRING, fileName.length() - 5);
      }

      if(packageString.length() == 0) {
        rootPath = pFileName.substring( START_OF_STRING , pFileName.length() - fileName.length());
      } else {
        rootPath = pFileName.substring( START_OF_STRING , pFileName.length() - packageString.length() - fileName.length() - 1);
      }

      CompilationUnit unit = parse(file);

      return unit;

    } catch (CFAGenerationRuntimeException e) {
      throw new ParserException(e);
    } catch (IOException e) {
      throw new ParserException(e);
    } finally {
      parseTimer.stop();
    }
  }


  private CompilationUnit parse(File file) throws IOException {
    return parse(file , PARSE_METHOD_BODY);
  }

  private Map<String, JClassOrInterfaceType> getTypeHierachie() throws IOException {

    File mainDirectory = new File(rootPath);
    assert mainDirectory.isDirectory() : "Could not find main directory at" + rootPath;

    Queue<File> directorysToBeSearched = new LinkedList<File>();
    Queue<File> sourceFileToBeParsed = new LinkedList<File>();
    directorysToBeSearched.add(mainDirectory);

    while (!directorysToBeSearched.isEmpty()) {
      File directory = directorysToBeSearched.poll();


      for (String filePath : directory.list()) {
        File file = new File(directory.getAbsolutePath() + File.separatorChar + filePath);

        if (filePath.matches(JAVA_SOURCE_FILE_REGEX)) {
          sourceFileToBeParsed.add(file);
        } else if (file.isDirectory()) {
          directorysToBeSearched.add(file);
        }
      }
    }

    Map<String, JClassOrInterfaceType> types = new HashMap<String, JClassOrInterfaceType>();
    TypeHierachyCreator creator = new TypeHierachyCreator(logger, types);

    for( File file  : sourceFileToBeParsed){

      CompilationUnit co = parse(file , IGNORE_METHOD_BODY);
      co.accept(creator);
    }

    return types;
  }

  private CompilationUnit parse(File file, boolean ignoreMethodBody) throws IOException {
    final String[] sourceFilePath = new String[1];
    sourceFilePath[0] = rootPath;

    String source = FileUtils.readFileToString(file);

    parser.setIgnoreMethodBodies(ignoreMethodBody);

    final String[] encoding = {"utf8" };

    parser.setSource(source.toCharArray());
    parser.setEnvironment(null, sourceFilePath, encoding, false);
    parser.setResolveBindings(true);
    parser.setStatementsRecovery(true);
    parser.setBindingsRecovery(true);


    // Set Compliance Options to support JDK 1.7
    @SuppressWarnings("unchecked")
    Hashtable<String , String> options = JavaCore.getOptions();
    JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, options);
    parser.setCompilerOptions(options);

    //unitName = file.getCanonicalPath();

    parser.setUnitName(file.getCanonicalPath());

    return (CompilationUnit)parser.createAST(null);
  }

  private CompilationUnit parseOnlyDeclarations(File file) throws IOException {

    String source = FileUtils.readFileToString(file);

    parser.setIgnoreMethodBodies(true);
    parser.setSource(source.toCharArray());
    CompilationUnit ast = (CompilationUnit) parser.createAST(null);


    return ast;
  }

  private ParseResult buildCFA(CompilationUnit ast) throws ParserException {
    cfaTimer.start();

    CompilationUnit astNext;

    try {
      AstErrorChecker checker = new AstErrorChecker(logger);

      //Is Needed For Complete Functionality

      Map<String, JClassOrInterfaceType> types = getTypeHierachie();

      CFABuilder builder = new CFABuilder(logger, ignoreCasts , qualifiedNameOfMainClass, types);
      try {

        ast.accept(checker);
        ast.accept(builder);

        String nextClassToBeParsed = builder.getScope().getNextClassPath();

        while(nextClassToBeParsed != null ){
          cfaTimer.stop();
          astNext = parseAdditionalClasses(nextClassToBeParsed);
          cfaTimer.start();
          if (astNext != null) {
            astNext.accept(checker);
            astNext.accept(builder);
          }
          nextClassToBeParsed = builder.getScope().getNextClassPath();
       }

      } catch (CFAGenerationRuntimeException e) {
        throw new ParserException(e);
      }

      DynamicBindingCreator tracker = new DynamicBindingCreator(builder , types);
      tracker.trackAndCreateDynamicBindings();

      return new ParseResult(builder.getCFAs(), builder.getCFANodes(), builder.getGlobalDeclarations());

    } catch (IOException e) {
      throw new ParserException(e);
    } finally {
      cfaTimer.stop();
    }
  }


  private CompilationUnit parseAdditionalClasses(String pFileName) throws ParserException {

    parseTimer.start();
     String name = rootPath + pFileName;




     try {

       // There is a possibility that classes are in one and the same file
       // in that case, the files don't exist
      File file = new File(name);
      if (file.isFile()) {
        return parse(file);
      } else {
        return null;
      }

     } catch (CFAGenerationRuntimeException e) {
        throw new ParserException(e);
      } catch (IOException e) {
        throw new ParserException(e);
      } finally {
        parseTimer.stop();
      }

       /*
      File file = new File(name);

      // There is a possibility that classes are in one and the same file
      // in that case, the files don't exist
      if (file.isFile()) {

        String source = FileUtils.readFileToString(file);

        String[] sourceFilePath = { rootPath };
        //TODO Should be decided by user
        String[] encoding = { "utf8" };

        parser.setSource(source.toCharArray());
        parser.setEnvironment(null, sourceFilePath, encoding, true);
        parser.setResolveBindings(true);
        parser.setStatementsRecovery(true);
        parser.setBindingsRecovery(true);

        // Set Compliance Options to support JDK 1.7
        @SuppressWarnings("unchecked")
        Hashtable<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, options);
        parser.setCompilerOptions(options);

        parser.setUnitName(unitName);
        CompilationUnit unit = (CompilationUnit) parser.createAST(null);
        return unit;
      } else {
        return null;
      }



    } catch (CFAGenerationRuntimeException e) {
      throw new ParserException(e);
    } catch (IOException e) {
      throw new ParserException(e);
    } finally {
      parseTimer.stop();
    }
*/

  }

  @Override
  public Timer getParseTime() {
    return parseTimer;
  }

  @Override
  public Timer getCFAConstructionTime() {
    return cfaTimer;
  }

  /**
   * Private class that tells the Eclipse CDT scanner that no macros and include
   * paths have been defined externally.
   */
  protected static class StubScannerInfo implements IScannerInfo {

    protected final static IScannerInfo instance = new StubScannerInfo();

    @Override
    public Map<String, String> getDefinedSymbols() {
      // the externally defined pre-processor macros
      return null;
    }

    @Override
    public String[] getIncludePaths() {
      return new String[0];
    }
  }

  public void setIgnoreCasts(boolean pIgnoreCasts) {
    ignoreCasts = pIgnoreCasts;
  }
}