package org.sosy_lab.cpachecker.fllesh.experiments;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.sosy_lab.cpachecker.fllesh.FlleShResult;
import org.sosy_lab.cpachecker.fllesh.Main;
import org.sosy_lab.cpachecker.fllesh.util.profiling.TimeAccumulator;

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
  
  public FlleShResult execute(String[] lArguments) throws IOException {
    Preconditions.checkNotNull(lArguments);
    Preconditions.checkArgument(lArguments.length == 3 || lArguments.length == 4);
    
    TimeAccumulator lTime = new TimeAccumulator();
    
    lTime.proceed();
    
    FlleShResult lResult = Main.run(lArguments);
    
    lTime.pause();
    
    mExperiment.addExperiment(lArguments[0], lArguments[1], lArguments[2], (lArguments.length == 4), lResult, lTime.getSeconds());
    
    return lResult;
  }
  
}
