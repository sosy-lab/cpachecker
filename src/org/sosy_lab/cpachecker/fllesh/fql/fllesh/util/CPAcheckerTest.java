package org.sosy_lab.cpachecker.fllesh.fql.fllesh.util;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.common.configuration.Configuration;

import org.sosy_lab.cpachecker.core.LogManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InvalidConfigurationException;

public class CPAcheckerTest {
  private String mPropertiesFile = "test/config/simpleMustMayAnalysis.properties";

  @Before
  public void tearDown() {
    /* XXX: Currently this is necessary to pass all assertions. */
    org.sosy_lab.cpachecker.core.CPAchecker.logger = null;
  }

  @Test
  public void test_01() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/functionCall.c");
    
    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);
    
    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
    
    // TODO: reimplement or reuse?
    //System.out.println(lCPAchecker.runAlgorithm().toString());
  }
  
  @Test
  public void test_02() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/loop1.c");
    
    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);
    
    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
    
    // TODO: reimplement or reuse?
    //System.out.println(lCPAchecker.runAlgorithm().toString());
  }
  
  @Test
  public void test_03() throws IOException, InvalidConfigurationException, CPAException {
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
