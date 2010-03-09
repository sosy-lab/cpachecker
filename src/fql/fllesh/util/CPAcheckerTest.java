package fql.fllesh.util;

import java.io.IOException;

import org.junit.Test;

import common.configuration.Configuration;

import cmdline.CPAMain;
import cmdline.CPAMain.InvalidCmdlineArgumentException;
import cpa.common.LogManager;
import exceptions.CPAException;

public class CPAcheckerTest {
  private String mConfig = "-config";
  private String mPropertiesFile = "test/config/simpleMustMayAnalysis.properties";
  
  @Test
  public void test_01() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/programs/simple/functionCall.c";
    
    Configuration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
    
    // TODO: reimplement or reuse?
    //System.out.println(lCPAchecker.runAlgorithm().toString());
  }
  
  @Test
  public void test_02() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/programs/simple/loop1.c";
        
    Configuration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
    
    // TODO: reimplement or reuse?
    //System.out.println(lCPAchecker.runAlgorithm().toString());
  }
  
  @Test
  public void test_03() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/programs/simple/uninitVars.cil.c";
        
    /*
     * Note: This analysis returns most of the time
     * bottom elements for the must analysis since
     * it can not handle pointers at the moment.
     */
    
    Configuration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
   
    // TODO: reimplement or reuse?
    //System.out.println(lCPAchecker.runAlgorithm().toString());
  }
}
