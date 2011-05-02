package org.sosy_lab.cpachecker.fshell.experiments;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.sosy_lab.common.TimeAccumulator;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.fshell.FShell3Result;
import org.sosy_lab.cpachecker.fshell.Main;

import com.google.common.base.Preconditions;

public abstract class ExperimentalSeries {

  private static Experiment mExperiment = null;
  
  @BeforeClass
  public static void createLogFile() {
    if (mExperiment != null) {
      throw new RuntimeException();
    }
    
    SimpleDateFormat lDateFormat = new SimpleDateFormat("'log.test_locks.'yyyy-MM-dd'.'HH-mm-ss'.csv'");
    String lFileName = "test" + File.separator + "output" + File.separator + lDateFormat.format(new Date());
    
    mExperiment = new Experiment(lFileName);
  }
  
  @AfterClass
  public static void closeLogFile() {
    mExperiment.close();
    
    mExperiment = null;
  }
  
  public FShell3Result execute(String[] pArguments) throws IOException, InvalidConfigurationException {
    Preconditions.checkNotNull(pArguments);
    //Preconditions.checkArgument(pArguments.length == 3 || pArguments.length == 4);
    
    TimeAccumulator lTime = new TimeAccumulator();
    
    lTime.proceed();
    
    FShell3Result lResult = Main.run(pArguments);
    
    lTime.pause();
    
    boolean lCilPreprocessing = false;
    
    for (int lIndex = 3; lIndex < pArguments.length; lIndex++) {
      String lOption = pArguments[lIndex].trim();
      
      if (lOption.equals("--withoutCilPreprocessing")) {
        lCilPreprocessing = true;
      }
    }
    
    mExperiment.addExperiment(pArguments[0], pArguments[1], pArguments[2], lCilPreprocessing, lResult, lTime.getSeconds());
    
    return lResult;
  }
  
}
