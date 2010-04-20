package fllesh;

import java.io.File;
import java.util.LinkedList;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cmdline.CPAMain;

import common.configuration.Configuration;
import compositeCPA.CompositeCPA;

import cpa.common.LogManager;
import cpa.common.ReachedElements;
import cpa.common.algorithm.Algorithm;
import cpa.common.algorithm.CPAAlgorithm;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.CPAFactory;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.location.LocationCPA;
import exceptions.CPAException;
import fllesh.cpa.edgevisit.EdgeVisitCPA;
import fllesh.fql.fllesh.util.CPAchecker;
import fllesh.fql.fllesh.util.Cilly;

public class Main {

  private static final String mConfig = "-config";
  private static final String mPropertiesFile = "test/config/simpleMustMayAnalysis.properties";
  
  /**
   * @param pArguments
   * @throws Exception 
   */
  public static void main(String[] pArguments) throws Exception {
    assert(pArguments != null);
    assert(pArguments.length > 1);
    
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    
    // check cilly invariance of source file, i.e., is it changed when preprocessed by cilly?
    Cilly lCilly = new Cilly();
    
    String lSourceFileName = pArguments[1];
    
    if (!lCilly.isCillyInvariant(pArguments[1])) {
      File lCillyProcessedFile = lCilly.cillyfy(pArguments[1]);
      
      lSourceFileName = lCillyProcessedFile.getAbsolutePath();
      
      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }
    
    // set source file name
    lArguments[2] = lSourceFileName;
    
    Configuration lConfiguration = CPAMain.createConfiguration(lArguments);

    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
    
    
    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();
    
    
    EdgeVisitCPA.Factory lFactory = new EdgeVisitCPA.Factory(lMainFunction);
    ConfigurableProgramAnalysis lEdgeVisitCPA = lFactory.createInstance();
    
    System.out.println(lFactory.getMapping());
    
    
    CPAFactory lCPAFactory = CompositeCPA.factory();
    
    CPAFactory lLocationCPAFactory = LocationCPA.factory();
    
    
    
    try {
      ConfigurableProgramAnalysis lLocationCPA = lLocationCPAFactory.createInstance();
      
      LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<ConfigurableProgramAnalysis>();
      
      lComponentAnalyses.add(lLocationCPA);
      lComponentAnalyses.add(lEdgeVisitCPA);
      
      lCPAFactory.setChildren(lComponentAnalyses);
      
      ConfigurableProgramAnalysis lCPA = lCPAFactory.createInstance();
      
      Algorithm lAlgorithm = new CPAAlgorithm(lCPA, lLogManager);
      
      AbstractElement initialElement = lCPA.getInitialElement(lMainFunction);
      Precision initialPrecision = lCPA.getInitialPrecision(lMainFunction);
            
      ReachedElements lReachedElements = new ReachedElements(ReachedElements.TraversalMethod.DFS, true);
      lReachedElements.add(initialElement, initialPrecision);
      
      lAlgorithm.run(lReachedElements, true);
      
      for (AbstractElement reachedElement : lReachedElements) {
        System.out.println(reachedElement);
      }
      
    } catch (CPAException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}

