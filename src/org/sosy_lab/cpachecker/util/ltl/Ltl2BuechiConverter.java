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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jhoafparser.consumer.HOAConsumerPrint;
import jhoafparser.consumer.HOAConsumerStore;
import jhoafparser.parser.HOAFParser;
import jhoafparser.parser.generated.ParseException;
import jhoafparser.storage.StoredAutomaton;
import org.sosy_lab.common.NativeLibraries;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.LtlParserUtils;
import org.sosy_lab.cpachecker.util.ltl.formulas.LtlFormula;

public class Ltl2BuechiConverter {

  private static final String LTL3BA = "./ltl3ba";

  private final LtlFormula ltlFormula;
  private final ProcessBuilder builder;

  public static Automaton convertFormula(LtlFormula pFormula)
      throws InterruptedException, LtlParseException {
    Objects.requireNonNull(pFormula);

    StoredAutomaton storedAutomaton = null;
    Ltl2BuechiConverter converter = new Ltl2BuechiConverter(pFormula);

    try (InputStream is = converter.runLtlExec()) {

      HOAConsumerStore consumer = new HOAConsumerStore();
      HOAFParser.parseHOA(is, consumer);
      storedAutomaton = consumer.getStoredAutomaton();
      storedAutomaton.feedToConsumer(new HOAConsumerPrint(System.out));

    } catch (ParseException e) {
      throw new LtlParseException(
          String.format(
              "An error occured while parsing the output from external tool '%s'", LTL3BA),
          e);
    } catch (IOException e) {
      throw new LtlParseException(e.getMessage(), e);
    }

    return LtlParserUtils.transform(storedAutomaton);
  }

  private Ltl2BuechiConverter(LtlFormula pFormula) {
    ltlFormula = pFormula;
    builder = new ProcessBuilder();

    Path nativeLibraryPath = NativeLibraries.getNativeLibraryPath();
    builder.directory(nativeLibraryPath.toFile());

    /*
     * '-H' to build and output the buechi-automaton in HOA format
     * '-f "formula"' to translate the LTL formula into a never claim
     */
    builder.command(LTL3BA, "-H", "-f", ltlFormula.toString());
  }

  private InputStream runLtlExec() throws InterruptedException, LtlParseException {
    InputStream is;

    try {
      Process process = builder.start();
      is = process.getInputStream();

      int exitvalue = process.waitFor();

      if (exitvalue != 0) {
        String errMsg = convertProcessToStream(is).collect(Collectors.joining("\n"));
        throw new LtlParseException(
            String.format(
                "Tool '%s' exited with error code %d. Message from tool:%n%s",
                LTL3BA, exitvalue, errMsg));
      }

    } catch (IOException | ExecutionException e) {
      throw new LtlParseException(e.getMessage(), e);
    }

    return is;
  }

  private static class StreamGobbler implements Runnable {

    private InputStream input;
    private Consumer<String> consumer;

    StreamGobbler(InputStream input, Consumer<String> consumer) {
      this.input = input;
      this.consumer = consumer;
    }

    @Override
    public void run() {
      InputStreamReader inputStreamReader = new InputStreamReader(input, Charset.defaultCharset());
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
      bufferedReader.lines().forEach(consumer);
    }
  }

  private Stream<String> convertProcessToStream(InputStream is)
      throws InterruptedException, ExecutionException {
    List<String> list = new ArrayList<>();

    ExecutorService executor = Executors.newSingleThreadExecutor();
    StreamGobbler gobbler = new StreamGobbler(is, x -> list.add(x));

    executor.submit(gobbler).get();
    executor.shutdown();

    return list.stream();
  }
}
