/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.termination;

import static com.google.common.base.Preconditions.checkNotNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.sosy_lab.common.ChildFirstPatternClassLoader;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.Classes.UnsuitedClassException;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.solver.SolverContextFactory;
import org.sosy_lab.solver.SolverContextFactory.Solvers;
import org.sosy_lab.solver.api.SolverContext;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Loads a {@link LassoAnalysis} implementation using an ugly class loader hack required
 * because CPAchecker and SMTInterpol use incompatible JavaCup versions.
 *
 * @see SolverContextFactory
 */
public class LassoAnalysisLoader {

  private final static String LASSO_ANALYSIS_IMPL =
      "org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.LassoAnalysisImpl";
  private final static String LASSO_CLASS = "de.uni_freiburg.informatik.ultimate.lassoranker.Lasso";
  private final static String SMT_UTILS_CLASS =
      "de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils";
  private final static String TOOL_CHAIN_STORAGE =
      "de.uni_freiburg.informatik.ultimate.core.coreplugin.services.ToolchainStorage";
  private final static String I_TOOL_CHAIN_STORAGE =
      "de.uni_freiburg.informatik.ultimate.core.model.services.IToolchainStorage";
  private final static String TOOL_CHAIN_CANCEL_EXCEPTION =
      "de.uni_freiburg.informatik.ultimate.util.ToolchainCanceledException";
  private final static Pattern LASSO_RANKER_CLASSES =
      Pattern.compile(
          "^de\\.uni_freiburg\\.informatik\\.ultimate\\.lassoranker\\..*|"
              + "^de\\.uni_freiburg\\.informatik\\.ultimate\\.modelcheckerutils\\..*|"
              + "^de\\.uni_freiburg\\.informatik\\.ultimate\\.core\\..*|"
              + "^de\\.uni_freiburg\\.informatik\\.ultimate\\.util\\.ToolchainCanceledException|"
              + "^org\\.sosy_lab\\.cpachecker\\.core\\.algorithm\\.termination\\.lasso_analysis\\..*");

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;
  private final TerminationStatistics statistics;

  public LassoAnalysisLoader(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      TerminationStatistics pStatistics) {
    config = checkNotNull(pConfig);
    logger = checkNotNull(pLogger);
    shutdownNotifier = checkNotNull(pShutdownNotifier);
    cfa = checkNotNull(pCfa);
    statistics = checkNotNull(pStatistics);
  }

  @SuppressFBWarnings("DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED")
  public LassoAnalysis load() throws InvalidConfigurationException {
    ClassLoader TerminationAlgorithmClassLoader = TerminationAlgorithm.class.getClassLoader();
    SolverContext solverContext =
        SolverContextFactory.createSolverContext(
            config, logger, shutdownNotifier, Solvers.SMTINTERPOL);
    ClassLoader smtInterpolClassLoader =
        solverContext.getFormulaManager().getClass().getClassLoader();

    String lassoClass = LASSO_CLASS.replace('.', File.separatorChar) + ".class";
    String smtUtilsClass = SMT_UTILS_CLASS.replace('.', File.separatorChar) + ".class";
    String toolStorageClass = TOOL_CHAIN_STORAGE.replace('.', File.separatorChar) + ".class";
    String iToolStorageClass = I_TOOL_CHAIN_STORAGE.replace('.', File.separatorChar) + ".class";
    String toolchainCancelExceptionClass =
        TOOL_CHAIN_CANCEL_EXCEPTION.replace('.', File.separatorChar) + ".class";
    URL lassoClassUrl = TerminationAlgorithmClassLoader.getResource(lassoClass);
    URL smtUtilsClassUrl = TerminationAlgorithmClassLoader.getResource(smtUtilsClass);
    URL toolStorageUrl = TerminationAlgorithmClassLoader.getResource(toolStorageClass);
    URL iToolStorageUrl = TerminationAlgorithmClassLoader.getResource(iToolStorageClass);
    URL toolchainCancelExceptionURL =
        TerminationAlgorithmClassLoader.getResource(toolchainCancelExceptionClass);

    ClassLoader lassoAnalysisClassLoader = TerminationAlgorithmClassLoader;
    if (lassoClassUrl != null
        && lassoClassUrl.getProtocol().equals("jar")
        && lassoClassUrl.getFile().contains("!")
        && smtUtilsClassUrl != null
        && smtUtilsClassUrl.getProtocol().equals("jar")
        && smtUtilsClassUrl.getFile().contains("!")
        && toolStorageUrl != null
        && toolStorageUrl.getProtocol().equals("jar")
        && toolStorageUrl.getFile().contains("!")
        && iToolStorageUrl != null
        && iToolStorageUrl.getProtocol().equals("jar")
        && iToolStorageUrl.getFile().contains("!")
        && toolchainCancelExceptionURL != null
        && toolchainCancelExceptionURL.getProtocol().equals("jar")
        && toolchainCancelExceptionURL.getFile().contains("!")) {
      try {
        lassoClassUrl =
            new URL(lassoClassUrl.getFile().substring(0, lassoClassUrl.getFile().lastIndexOf('!')));
        smtUtilsClassUrl =
            new URL(
                smtUtilsClassUrl
                    .getFile()
                    .substring(0, smtUtilsClassUrl.getFile().lastIndexOf('!')));
        toolStorageUrl =
            new URL(
                toolStorageUrl.getFile().substring(0, toolStorageUrl.getFile().lastIndexOf('!')));
        iToolStorageUrl =
            new URL(
                iToolStorageUrl.getFile().substring(0, iToolStorageUrl.getFile().lastIndexOf('!')));
        toolchainCancelExceptionURL =
            new URL(
                toolchainCancelExceptionURL
                    .getFile()
                    .substring(0, toolchainCancelExceptionURL.getFile().lastIndexOf('!')));

        URL[] urls = {
          lassoClassUrl,
          smtUtilsClassUrl,
          toolStorageUrl,
          iToolStorageUrl,
          toolchainCancelExceptionURL,
          TerminationAlgorithm.class.getProtectionDomain().getCodeSource().getLocation(),
        };

        // By using ChildFirstPatternClassLoader we ensure that classes
        // do not get loaded by the parent class loader.
        lassoAnalysisClassLoader =
            new ChildFirstPatternClassLoader(LASSO_RANKER_CLASSES, urls, smtInterpolClassLoader);

      } catch (MalformedURLException e) {
        logger.logUserException(
            Level.WARNING,
            e,
            "Could not create proper classpath for LassoAnalysis, "
                + "loading correct java-cup classes may fail.");
      }
    } else {
      logger.log(
          Level.WARNING,
          "Could not create proper classpath for LassoRanker because location of LassoRanker "
              + "classes is unexpected, loading correct java-cup classes may fail. "
              + "Locations of LassoRanker are ",
          lassoClassUrl,
          smtUtilsClassUrl);
    }

    LassoAnalysis.Factory factory;
    try {
      Class<?> lassoImpl = Class.forName(LASSO_ANALYSIS_IMPL, true, lassoAnalysisClassLoader);
      factory = Classes.createFactory(LassoAnalysis.Factory.class, lassoImpl);
    } catch (ClassNotFoundException | UnsuitedClassException e) {
      throw new RuntimeException(e);
    }

    return factory.create(logger, config, shutdownNotifier, solverContext, cfa, statistics);
  }
}
