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

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
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
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.ltl.formulas.LabelledFormula;
import org.sosy_lab.cpachecker.util.ltl.formulas.Literal;

public class Ltl2BuechiConverter {

  private static final String LTL3BA = "./ltl3ba";

  private final LabelledFormula labelledFormula;
  private final ProcessBuilder builder;

  public static Automaton convertFormula(LabelledFormula pFormula)
      throws InterruptedException, LtlParseException {
    Objects.requireNonNull(pFormula);

    StoredAutomaton storedAutomaton = null;
    Ltl2BuechiConverter converter = new Ltl2BuechiConverter(pFormula);

    try (InputStream is = converter.runLtlExec()) {

      HOAConsumerStore consumer = new HOAConsumerStore();
      HOAFParser.parseHOA(is, consumer);
      storedAutomaton = consumer.getStoredAutomaton();

      // Convert the aliases back to their original ap's
      List<String> list = new ArrayList<>(storedAutomaton.getStoredHeader().getAPs().size());
      for (String s : storedAutomaton.getStoredHeader().getAPs()) {
        int index = Integer.parseInt(Iterables.get(Splitter.on("val").split(s), 1));
        list.add(pFormula.getAPs().get(index).getAtom());
      }
      storedAutomaton.getStoredHeader().setAPs(list);

      if (!storedAutomaton
          .getStoredHeader()
          .getAPs()
          .stream()
          .anyMatch(x -> pFormula.getAPs().contains(Literal.of(x, false)))) {
        throw new RuntimeException(
            "Output from external tool contains APs that are not consistent with the parsed ltl formula");
      }

      // TODO: replace outputstream with logger
      storedAutomaton.feedToConsumer(new HOAConsumerPrint(System.out));

    } catch (ParseException e) {
      throw new LtlParseException(
          String.format(
              "An error occured while parsing the output from external tool '%s'", LTL3BA),
          e);
    } catch (IOException e) {
      throw new LtlParseException(e.getMessage(), e);
    }

    try {
      return LtlParserUtils.transform(storedAutomaton);
    } catch (UnrecognizedCodeException e) {
      throw new LtlParseException(e.getMessage(), e);
    }
  }

  private Ltl2BuechiConverter(LabelledFormula pFormula) {
    labelledFormula = pFormula;
    builder = new ProcessBuilder();

    Path nativeLibraryPath = NativeLibraries.getNativeLibraryPath();
    builder.directory(nativeLibraryPath.toFile());

    /*
     * '-H' to build and output the buechi-automaton in HOA format
     * '-f "formula"' to translate the LTL formula into a never claim
     */
    String formula =
        LtlStringVisitor.toString(labelledFormula.getFormula(), labelledFormula.getAPs());
    builder.command(LTL3BA, "-H", "-f", formula);
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
