/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.ltl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.sosy_lab.cpachecker.util.ltl.formulas.Formula;

public class LTL2BuechiConverter {

  // ltl3ba can be found at https://sourceforge.net/projects/ltl3ba/
  // and requires the 'BuDDy'-library (http://sourceforge.net/projects/buddy/) in order
  // to compile successfully

  // TODO @PW - the following three variables are only temporary, and will be updated to
  // relative paths soon
  private static final String EXECUTABLE_DIR =
      System.getProperty("user.home") + "/SoSy-Lab/ltl-buechi-rltd/ltl3ba-1.1.3";

  private static final String LD_LIBRARY_PATH = "LD_LIBRARY_PATH";

  private static final String USR_LOCAL_LIB = "/usr/local/lib/";

  private static final String LTL3BA = "./ltl3bas";

  private static class StreamGobbler implements Runnable {

    private InputStream input;
    private Consumer<String> consumer;

    public StreamGobbler(InputStream input, Consumer<String> consumer) {
      this.input = input;
      this.consumer = consumer;
    }

    @Override
    public void run() {
      InputStreamReader inputStreamReader = new InputStreamReader(input);
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
      bufferedReader.lines().forEach(consumer);
    }
  }

  public static InputStream convertFormula(Formula f) throws InterruptedException, IOException {
    if (!System.getProperty("os.name").equals("Linux")) {
      throw new UnsupportedOperationException("Only Linux is currently supported as OS");
    }

    Objects.requireNonNull(f);
    return new LTL2BuechiConverter(f).runLtlExec();
  }

  private final Formula formula;
  private final ProcessBuilder builder;

  private LTL2BuechiConverter(Formula f) {
    formula = f;
    builder = new ProcessBuilder();

    builder.directory(new File(EXECUTABLE_DIR));
    builder.environment().put(LD_LIBRARY_PATH, USR_LOCAL_LIB);
    builder.redirectErrorStream(true);
    builder.command(LTL3BA, "-H", "-f", formula.toString());
  }

  private InputStream runLtlExec() throws IOException, InterruptedException {
    // TODO check that ltl3ba tool exists

    Process process = builder.start();
    InputStream is = process.getInputStream();

    int exitvalue = process.waitFor();
    assert exitvalue == 0 : "Tool 'ltl3ba' exited with error code: " + exitvalue;

    return is;
  }

  public static Stream<String> convertProcessToStream(InputStream is) {
    List<String> list = new ArrayList<>();

    ExecutorService executor = Executors.newSingleThreadExecutor();
    StreamGobbler gobbler = new StreamGobbler(is, (x) -> list.add(x));

    executor.submit(gobbler);
    executor.shutdown();

    return list.stream();
  }
}
