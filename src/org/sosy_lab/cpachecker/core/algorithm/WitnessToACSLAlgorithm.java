/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.base.Optional;
import com.google.common.io.MoreFiles;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import org.eclipse.cdt.core.dom.ast.ASTGenericVisitor;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.ParserFactory;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContentProvider;
import org.eclipse.core.runtime.CoreException;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.Parsers;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.bmc.CandidateGenerator;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.core.algorithm.invariants.KInductionInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.KInductionInvariantGenerator.KInductionInvariantGeneratorOptions;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.automaton.CachingTargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;

public class WitnessToACSLAlgorithm implements Algorithm {

  private final Configuration config;
  private final Specification specification;
  private final LogManager logger;
  private final CFA cfa;
  private final ShutdownManager shutdownManager;
  private final KInductionInvariantGeneratorOptions kindOptions;
  private final TargetLocationProvider targetLocationProvider;
  private final ToCExpressionVisitor toCExpressionVisitor;
  private final ILanguage lang;

  public WitnessToACSLAlgorithm(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, CPAException {
    config = pConfig;
    specification = pSpecification;
    logger = pLogger;
    cfa = pCfa;
    shutdownManager = ShutdownManager.createWithParent(pShutdownNotifier);
    kindOptions = new KInductionInvariantGeneratorOptions();
    config.inject(kindOptions);
    targetLocationProvider = new CachingTargetLocationProvider(pShutdownNotifier, logger, cfa);
    toCExpressionVisitor = new ToCExpressionVisitor(cfa.getMachineModel(), logger);
    lang = GCCLanguage.getDefault();
    //    KInductionInvariantGenerator invGen =
    //        KInductionInvariantGenerator.create(
    //            pConfig,
    //            pLogger,
    //            shutdownManager,
    //            cfa,
    //            specification,
    //            new ReachedSetFactory(config, logger),
    //            pTargetLocationProvider,
    //            pAggregatedReachedSets);

  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    final CandidateGenerator gen;
    final Set<String> files = new LinkedHashSet<>();
    Map<CFANode, CExpression> invMap = new HashMap<>();
    try {
      gen =
          KInductionInvariantGenerator.getCandidateInvariants(
              kindOptions,
              config,
              logger,
              cfa,
              shutdownManager,
              targetLocationProvider,
              specification);
    } catch (InvalidConfigurationException e) {
      throw new CPAException("Invalid Configuration while analyzing witness", e);
    }
    // this is important because otherwise the candidates will not be displayed!
    gen.produceMoreCandidates();
    ArrayList<ExpressionTreeLocationInvariant> cands = new ArrayList<>();
    java.util.Iterator<CandidateInvariant> it = gen.iterator();
    while (it.hasNext()) {
      CandidateInvariant inv = it.next();

      if (inv instanceof ExpressionTreeLocationInvariant) {
        cands.add((ExpressionTreeLocationInvariant) inv);
      }
    }

    // Extract invariants as CExpressions and nodes
    for (ExpressionTreeLocationInvariant c : cands) {
      CFANode loc = c.getLocation();
      Optional<? extends AAstNode> astNodeOptional = loc.getLeavingEdge(0).getRawAST();

      @SuppressWarnings("unchecked")
      CExpression exp =
          ((ExpressionTree<AExpression>) (Object) c.asExpressionTree())
              .accept(toCExpressionVisitor);
      invMap.put(loc, exp);
      if (astNodeOptional.isPresent()) {
        AAstNode astNode = astNodeOptional.get();
        FileLocation fileLoc = astNode.getFileLocation();
        //          List<Object> li = rewrite.getComments(astNodeOptional.get(),
        // CommentPosition.leading);
        //          rewrite.addComment(astNodeOptional.get(), li.get(0), CommentPosition.leading);
        files.add(fileLoc.getFileName());
      } else {
      }
    }

    // Compile found files into Eclipse ASTs.

    Map <String,IASTTranslationUnit> astMap = new HashMap<>();
    for (String file : files) {
      char[] source = null;
      try {
        source =
            MoreFiles.asCharSource(Paths.get(file), Charset.defaultCharset()).read().toCharArray();
      } catch (IOException e1) {
        logger.logfUserException(Level.SEVERE, e1, "Could not read file %s", file);
      }


      TranslationUnitInfo info = createTranslationUnitInfo(file, source);
      try {
        IASTTranslationUnit translationUnit =
            lang.getASTTranslationUnit(
                info.getFileContent(),
                info.getScannerInfo(),
                info.getFileContentProvider(), // needed for imports!
                null,
                ILanguage.OPTION_NO_IMAGE_LOCATIONS,
                ParserFactory.createDefaultLogService());
        astMap.put(file, translationUnit);
      } catch (CoreException e) {
        logger.logfUserException(Level.SEVERE, e, "Failed to create AST for file %s", file);
      }

    }

    for (Entry<String, IASTTranslationUnit> e : astMap.entrySet()) {
      String file = e.getKey();
      IASTTranslationUnit tu = e.getValue();
      ASTRewrite rewrite = ASTRewrite.create(tu);
      for (Entry<CFANode, CExpression> e2 : invMap.entrySet()) {
        CFANode node = e2.getKey();
        CExpression inv = e2.getValue();
        ASTVisitor myV = new myVisitor();
        tu.accept(myV);
        break;
      }

    }

    return AlgorithmStatus.SOUND_AND_PRECISE;
  }

  /*
   * This method encapsulates ugly stuff that we currently need to get the ASTTranslationUnit that we want.
   * A future TODO is to find a nicer way to do this
   */
  private TranslationUnitInfo createTranslationUnitInfo(String file, char[] source) {
    FileContent fileContent = FileContent.create(file, source);
    InternalFileContentProvider fileContentProvider = null;
    IScannerInfo scannerInfo = null;
    ClassLoader loader;
    try {
      Method m =
          Parsers.class.getDeclaredMethod("getClassLoader", new Class[] {LogManager.class});
      m.setAccessible(true);
      loader = (ClassLoader) m.invoke(null, logger);
      Class<? extends Object> parserClass =
          loader.loadClass("org.sosy_lab.cpachecker.cfa.parser.eclipse.c.EclipseCParser");
      List<Class> classes = Arrays.asList(parserClass.getDeclaredClasses());
      Class scannerClass = null;
      Class fileContentProviderClass = null;
      for (Class c : classes) {
        if (c.getName().contains("StubScannerInfo")) {
          scannerClass = c;
        } else if (c.getName().contains("FileContentProvider")) {
          {
            fileContentProviderClass = c;
          }
        }
      }
      final Field field = scannerClass.getDeclaredField("instance");
      field.setAccessible(true);
      // org.eclipse.cdt.core.parser.IScannerInfo
      //        Class<?> type = field.getType();
      Object o = field.get(null);
      //        if (IScannerInfo.class.isInstance(o)) {
      scannerInfo =
          new IScannerInfo() {
            @Override
            public java.util.Map<String, String> getDefinedSymbols() {
              try {
                Object instance = field.get(null);
                Method m;
                m =
                    field
                        .getType()
                        .getDeclaredMethod("getDefinedSymbols", new Class[] {});
                m.setAccessible(true);
                return (Map<String, String>) m.invoke(instance);
              } catch (NoSuchMethodException
                  | SecurityException
                  | IllegalArgumentException
                  | IllegalAccessException
                  | InvocationTargetException e) {
                logger.logException(Level.SEVERE, e, null);
                return null;
              }
            };

            @Override
            public String[] getIncludePaths() {
              Object instance;
              try {
                instance = field.get(null);
                Method m =
                    field
                        .getType()
                        .getDeclaredMethod("getIncludePaths", new Class[] {});
                m.setAccessible(true);
                return (String[]) m.invoke(instance);
              } catch (IllegalArgumentException
                  | IllegalAccessException
                  | NoSuchMethodException
                  | SecurityException
                  | InvocationTargetException e) {
                logger.logException(Level.SEVERE, e, null);
                return null;
              }
            };
          };
      Field scannerField = field;
      assert scannerInfo != null;
      final Field field2 = fileContentProviderClass.getDeclaredField("instance");
      field2.setAccessible(true);
      o = field2.get(null);
      if (o instanceof InternalFileContentProvider) {
        fileContentProvider = (InternalFileContentProvider) o;
      }

    } catch (IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException
        | NoSuchMethodException
        | SecurityException
        | ClassNotFoundException
        | NoSuchFieldException e1) {
      logger.logException(Level.SEVERE, e1, null);
    }

    TranslationUnitInfo info =
        new TranslationUnitInfo(fileContent, fileContentProvider, scannerInfo);
    return info;
  }

  private class myVisitor extends ASTGenericVisitor {

    private int rank = 0;

    public myVisitor() {
      super(true); // visit nodes
      // TODO Auto-generated constructor stub
    }

    @Override
    protected int genericVisit(IASTNode node) {
      return PROCESS_CONTINUE;
    }

    @Override
    protected int genericLeave(IASTNode pNode) {
      // TODO Auto-generated method stub
      rank--;
      return super.genericLeave(pNode);
    }
  }

  private class TranslationUnitInfo {
    private FileContent fileContent;
    private InternalFileContentProvider fileContentProvider;
    private IScannerInfo scannerInfo;

    public TranslationUnitInfo(
        FileContent pFileContent,
        InternalFileContentProvider pFileContentProvider,
        IScannerInfo pScannerInfo) {
      fileContent = pFileContent;
      fileContentProvider = pFileContentProvider;
      scannerInfo = pScannerInfo;
    }

    public FileContent getFileContent() {
      return fileContent;
    }

    public InternalFileContentProvider getFileContentProvider() {
      return fileContentProvider;
    }

    public IScannerInfo getScannerInfo() {
      return scannerInfo;
    }
  }
}
