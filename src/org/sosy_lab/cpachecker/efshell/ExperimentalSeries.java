package org.sosy_lab.cpachecker.efshell;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.sosy_lab.common.TimeAccumulator;
import org.sosy_lab.common.configuration.InvalidConfigurationException;

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

  public FShell3Result execute(String[] lArguments) throws IOException, InvalidConfigurationException {
    Preconditions.checkNotNull(lArguments);
    Preconditions.checkArgument(lArguments.length == 3 || lArguments.length == 4);

    TimeAccumulator lTime = new TimeAccumulator();

    lTime.proceed();

    FShell3Result lResult = Main.run(lArguments,null);

   // lTime.pause();

    mExperiment.addExperiment(lArguments[0], lArguments[1], lArguments[2], (lArguments.length == 4), lResult, lTime.getSeconds());

    return lResult;
  }

}
