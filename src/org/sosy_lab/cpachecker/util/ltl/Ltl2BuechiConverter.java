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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jhoafparser.consumer.HOAConsumerPrint;
import jhoafparser.consumer.HOAConsumerStore;
import jhoafparser.parser.HOAFParser;
import jhoafparser.parser.generated.ParseException;
import jhoafparser.storage.StoredAutomaton;
import org.sosy_lab.common.NativeLibraries;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.LtlParserUtils;
import org.sosy_lab.cpachecker.util.ltl.formulas.LabelledFormula;
import org.sosy_lab.cpachecker.util.ltl.formulas.Literal;

public class Ltl2BuechiConverter {

  private static final String LTL3BA = "./ltl3ba";

  private final LabelledFormula labelledFormula;
  private final ProcessBuilder builder;

  private final LogManager logger;

  /**
   * Entry point to convert a ltl property into an {@link Automaton}.
   *
   * <p>This method takes a {@link LabelledFormula} and passes the contained ltl-property to an
   * external tool, which in turn transforms it into a buechi-automaton.
   *
   * <p>The output from the external tool is specified to be in 'Hanoi-Omega-Automaton' (HOA)
   * format, and parsed as such accordingly afterwards. The resulting object will again be
   * transformed into the final {@link Automaton}.
   *
   * @param pFormula the ltl-property together with a list of its atomic propositions
   * @return an automaton from the automaton-framework in CPAchecker
   * @throws LtlParseException if the transformation fails either due to some false values in the
   *     intermediate resulting StoredAutomaton or because of an erroneous config.
   */
  public static Automaton convertFormula(
      LabelledFormula pFormula,
      Configuration pConfig,
      LogManager pLogger,
      MachineModel pMachineModel,
      Scope pScope)
      throws InterruptedException, LtlParseException {
    checkNotNull(pFormula);

    StoredAutomaton hoaAutomaton = new Ltl2BuechiConverter(pFormula, pLogger).createHoaAutomaton();
    return LtlParserUtils.transform(hoaAutomaton, pConfig, pLogger, pMachineModel, pScope);
  }

  /**
   * Produces an {@link Automaton} from a ltl-property without the necessity of specifying a logger,
   * machine-model and scope.
   *
   * <p>This method is mainly used for testing / debugging the transformation of ltl properties to
   * automatons outside of CPAchecker.
   */
  static Automaton convertFormula(LabelledFormula pFormula, LogManager pLogger)
      throws InterruptedException, LtlParseException {
    checkNotNull(pFormula);

    StoredAutomaton hoaAutomaton = new Ltl2BuechiConverter(pFormula, pLogger).createHoaAutomaton();
    return LtlParserUtils.transform(hoaAutomaton);
  }

  /**
   * Constructor setting up the options for the executing the external 'ltl-to-buechi' tool.
   *
   * @param pFormula the ltl property to be transformed
   * @param pLogger a logger object
   */
  private Ltl2BuechiConverter(LabelledFormula pFormula, LogManager pLogger) {
    labelledFormula = pFormula;
    logger = pLogger;
    builder = new ProcessBuilder();

    Path nativeLibraryPath = NativeLibraries.getNativeLibraryPath();
    builder.directory(nativeLibraryPath.toFile());

    /*
     * '-H' to print the resulting output (the buechi-automaton) in HOA format
     * '-f "formula"' to translate the LTL formula into a never claim
     */
    String formula =
        LtlStringVisitor.toString(labelledFormula.getFormula(), labelledFormula.getAPs());
    builder.command(LTL3BA, "-H", "-f", formula);
  }

  /**
   * Invoke the execution of the external tool, which transforms the ltl property into a
   * buechi-automaton. The result is parsed afterwards and stored as {@link StoredAutomaton} (i.e.
   * an object containing an automaton in HOA format).
   *
   * @return the resulting {@link StoredAutomaton}
   */
  private StoredAutomaton createHoaAutomaton() throws InterruptedException, LtlParseException {

    try (InputStream is = runLtlExec()) {

      HOAConsumerStore consumer = new HOAConsumerStore();
      HOAFParser.parseHOA(is, consumer);
      StoredAutomaton storedAutomaton = consumer.getStoredAutomaton();

      // Convert the aliases back to their original ap's
      List<String> list = new ArrayList<>(storedAutomaton.getStoredHeader().getAPs().size());
      for (String s : storedAutomaton.getStoredHeader().getAPs()) {
        int index = Integer.parseInt(Iterables.get(Splitter.on("val").split(s), 1));
        list.add(labelledFormula.getAPs().get(index).getAtom());
      }
      storedAutomaton.getStoredHeader().setAPs(list);

      if (!storedAutomaton
          .getStoredHeader()
          .getAPs()
          .stream()
          .anyMatch(x -> labelledFormula.getAPs().contains(Literal.of(x, false)))) {
        throw new RuntimeException(
            "Output from external tool contains APs that are not consistent with APs from the parsed ltl formula");
      }

      ByteArrayOutputStream os = new ByteArrayOutputStream();
      storedAutomaton.feedToConsumer(new HOAConsumerPrint(os));
      logger.log(Level.FINEST, os.toString(StandardCharsets.UTF_8.name()));

      return storedAutomaton;
    } catch (ParseException e) {
      throw new LtlParseException(
          String.format(
              "An error occured while parsing the output from the external tool '%s'", LTL3BA),
          e);
    } catch (IOException e) {
      throw new LtlParseException(e.getMessage(), e);
    }
  }

  /**
   * Execute the external tool to transform a ltl property to a buechi-automaton. The output is
   * provided as input stream, which is required by the hoaf-library for parsing the resulting
   * output afterwards.
   *
   * @return The input stream with the automaton description
   */
  private InputStream runLtlExec() throws LtlParseException, InterruptedException {
    try {
      Process process = builder.start();
      InputStream is = process.getInputStream();

      int exitvalue = process.waitFor();

      if (exitvalue != 0) {
        String errMsg = convertProcessToStream(is).collect(Collectors.joining("\n"));
        throw new LtlParseException(
            String.format(
                "Tool '%s' exited with error code %d. Message from tool:%n%s",
                LTL3BA, exitvalue, errMsg));
      }

      return is;
    } catch (IOException e) {
      throw new LtlParseException(e.getMessage(), e);
    }
  }

  /**
   * Convert an {@link InputStream} into a Java 8 {@link Stream}. This is used to forward error
   * messages from the external tool to the logger.
   */
  private Stream<String> convertProcessToStream(InputStream is) {
    List<String> list = new ArrayList<>();

    InputStreamReader inputStreamReader = new InputStreamReader(is, Charset.defaultCharset());
    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
    bufferedReader.lines().forEach(x -> list.add(x));

    return list.stream();
  }
}
