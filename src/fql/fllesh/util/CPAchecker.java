package fql.fllesh.util;

import java.util.Map;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cmdline.stubs.StubFile;

import common.Pair;
import common.configuration.Configuration;

import cpa.common.LogManager;
import exceptions.InvalidConfigurationException;

// TODO: where is the right place to collect statistics?
public class CPAchecker extends cpa.common.CPAchecker {
  private Map<String, CFAFunctionDefinitionNode> mCFAMap;
  private CFAFunctionDefinitionNode mMainFunction;
  
  public CPAchecker(Configuration pConfiguration, LogManager pLogManager) throws InvalidConfigurationException {
    super(pConfiguration, pLogManager);
    
    // get code file name
    String[] names = pConfiguration.getPropertiesArray("analysis.programNames");
    if (names.length != 1) {
      cpa.common.CPAchecker.logger.log(Level.SEVERE, "Exactly one code file has to be given!");
      
      System.exit(1);
    }
    
    StubFile lSourceFile = new StubFile(names[0]);
    
    // parse code file
    IASTTranslationUnit lAst = null;
    try {
      lAst = super.parse(lSourceFile);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    
    Pair<Map<String, CFAFunctionDefinitionNode>, CFAFunctionDefinitionNode> lCFA = super.createCFA(lAst);
    
    mCFAMap = lCFA.getFirst();
    mMainFunction = lCFA.getSecond();
  }
  
  public Map<String, CFAFunctionDefinitionNode> getCFAMap() {
    return mCFAMap;
  }
  
  public CFAFunctionDefinitionNode getMainFunction() {
    return mMainFunction;
  }

  /*public ReachedElements run(Algorithm pAlgorithm, AbstractElement pInitialElement, Precision pInitialPrecision) throws CPAException {
    
    ReachedElements lReached = null;
    
    try {
      
      lReached = new ReachedElements(mConfiguration.getProperty("analysis.traversal"));
    } catch (IllegalArgumentException e) {
      
      mLogManager.logException(Level.SEVERE, e, "ERROR, unknown traversal option");
      System.exit(1);
    }
    
    lReached.add(pInitialElement, pInitialPrecision);

    run(pAlgorithm, lReached);

    return lReached;
  }
  
  public void run(Algorithm pAlgorithm, ReachedElements pReachedElements) throws CPAException {
    
    assert(pAlgorithm != null);
    assert(pReachedElements != null);
    
    mLogManager.log(Level.FINE, "CPA Algorithm starting ...");
    mStatistics.startAnalysisTimer();
    
    pAlgorithm.run(pReachedElements, mConfiguration.getBooleanValue("analysis.stopAfterError"));
    
    mStatistics.stopAnalysisTimer();
    mLogManager.log(Level.FINE, "CPA Algorithm finished");
  }
  
  public CPAStatistics.Result runAlgorithm() throws CPAException {

    mLogManager.log(Level.FINE, "Creating CPAs");
      
    ConfigurableProgramAnalysis cpa = CompositeCPA.getCompositeCPA(mMainFunction);

    if (mConfiguration.getBooleanValue("analysis.useART")) {
      cpa = ARTCPA.getARTCPA(mMainFunction, cpa);
    }
          
    if (cpa instanceof CPAWithStatistics) {
      ((CPAWithStatistics)cpa).collectStatistics(mStatistics.getSubStatistics());
    }
      
    // create algorithm
    Algorithm algorithm = new CPAAlgorithm(cpa);
      
    if (mConfiguration.getBooleanValue("analysis.useRefinement")) {
      algorithm = new CEGARAlgorithm(algorithm);
    }
      
    if (mConfiguration.getBooleanValue("analysis.useInvariantDump")) {
      algorithm = new InvariantCollectionAlgorithm(algorithm);
    }
      
    if (mConfiguration.getBooleanValue("analysis.useCBMC")) {
      algorithm = new CBMCAlgorithm(mCFAMap, algorithm);
    }
    
    ReachedElements reached = run(algorithm, cpa.getInitialElement(mMainFunction), cpa.getInitialPrecision(mMainFunction));
    
    for (AbstractElement reachedElement : reached) {
      if (reachedElement.isError()) {
        return CPAStatistics.Result.UNSAFE;
      }
    }
        
    return CPAStatistics.Result.SAFE;
  }*/
  
}
