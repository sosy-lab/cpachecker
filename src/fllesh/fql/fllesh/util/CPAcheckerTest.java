package fllesh.fql.fllesh.util;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import common.configuration.Configuration;

import cpa.common.LogManager;
import exceptions.CPAException;

public class CPAcheckerTest {
  private String mPropertiesFile = "test/config/simpleMustMayAnalysis.properties";

  @Before
  public void tearDown() {
    /* XXX: Currently this is necessary to pass all assertions. */
    cpa.common.CPAchecker.logger = null;
  }

  @Test
  public void test_01() throws IOException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/functionCall.c");
    
    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);
    
    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
    
    // TODO: reimplement or reuse?
    //System.out.println(lCPAchecker.runAlgorithm().toString());
  }
  
  @Test
  public void test_02() throws IOException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/loop1.c");
    
    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);
    
    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
    
    // TODO: reimplement or reuse?
    //System.out.println(lCPAchecker.runAlgorithm().toString());
  }
  
  @Test
  public void test_03() throws IOException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/uninitVars.cil.c");
        
    /*
     * Note: This analysis returns most of the time
     * bottom elements for the must analysis since
     * it can not handle pointers at the moment.
     */
    
    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);
    
    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
   
    // TODO: reimplement or reuse?
    //System.out.println(lCPAchecker.runAlgorithm().toString());
  }
}
