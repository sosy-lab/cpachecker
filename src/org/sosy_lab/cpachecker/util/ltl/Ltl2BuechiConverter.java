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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.CharStreams;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import jhoafparser.consumer.HOAConsumerStore;
import jhoafparser.parser.HOAFParser;
import jhoafparser.parser.generated.ParseException;
import jhoafparser.storage.StoredAutomaton;
import org.sosy_lab.common.NativeLibraries;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.BuechiConverterUtils;
import org.sosy_lab.cpachecker.util.ltl.formulas.LabelledFormula;
import org.sosy_lab.cpachecker.util.ltl.formulas.Literal;

public class Ltl2BuechiConverter {

  private static final Converter EXECUTABLE = Converter.LTL3BA;

  private final LabelledFormula labelledFormula;
  private final ProcessBuilder builder;

  /**
   * Entry point to convert a ltl property into an {@link Automaton}.
   *
   * <p>This method takes a {@link LabelledFormula} and passes the contained ltl-property to an
   * external tool, which in turn transforms it into a buechi-automaton.
   *
   * <p>The output from the external tool is required to be in 'Hanoi-Omega-Automaton' (HOA) format,
   * as it is parsed as such afterwards. The resulting object will then be transformed into the
   * final {@link Automaton}.
   *
   * @param pFormula the ltl-property together with a list of its atomic propositions
   * @param pEntryFunction the name of the entry-function
   * @return an automaton from the automaton-framework in CPAchecker
   * @throws LtlParseException if the transformation fails either due to some false values in the
   *     intermediate resulting StoredAutomaton or because of an erroneous config.
   */
  public static Automaton convertFormula(
      LabelledFormula pFormula,
      String pEntryFunction,
      Configuration pConfig,
      LogManager pLogger,
      MachineModel pMachineModel,
      Scope pScope,
      ShutdownNotifier pShutdownNotifier)
      throws InterruptedException, LtlParseException {
    checkNotNull(pFormula);

    StoredAutomaton hoaAutomaton = new Ltl2BuechiConverter(pFormula).createHoaAutomaton();
    return BuechiConverterUtils.convertFromHOAFormat(
        hoaAutomaton, pEntryFunction, pConfig, pLogger, pMachineModel, pScope, pShutdownNotifier);
  }

  /**
   * Produces an {@link Automaton} from a ltl-property without the necessity of specifying a logger,
   * machine-model and scope.
   *
   * <p>This method is mainly used for testing / debugging the transformation of ltl properties to
   * automatons outside of CPAchecker.
   */
  static Automaton convertFormula(LabelledFormula pFormula)
      throws InterruptedException, LtlParseException {
    checkNotNull(pFormula);

    StoredAutomaton hoaAutomaton = new Ltl2BuechiConverter(pFormula).createHoaAutomaton();
    return BuechiConverterUtils.convertFromHOAFormat(hoaAutomaton);
  }

  public static String getNameOfExecutable() {
    return EXECUTABLE.getToolName();
  }

  /**
   * Constructor that sets up the options for the execution of an external 'ltl-to-buechi'
   * transformation tool.
   *
   * @param pFormula the ltl property to be transformed
   */
  private Ltl2BuechiConverter(LabelledFormula pFormula) {
    labelledFormula = pFormula;
    builder = new ProcessBuilder();

    Path nativeLibraryPath = NativeLibraries.getNativeLibraryPath();
    builder.directory(nativeLibraryPath.toFile());

    String formula =
        LtlStringVisitor.toString(labelledFormula.getFormula(), labelledFormula.getAPs());
    ImmutableList<String> commands =
        ImmutableList.<String>builder()
            .add(EXECUTABLE.execTool())
            .addAll(EXECUTABLE.getArgs())
            .add("-f", formula)
            .build();
    builder.command(commands);
  }

  /**
   * Invoke the execution of the external tool, which transforms the ltl property into a
   * buechi-automaton. The result is parsed and stored as {@link StoredAutomaton} (i.e., an object
   * containing an automaton in HOA format).
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
          .allMatch(x -> labelledFormula.getAPs().contains(Literal.of(x, false)))) {
        throw new RuntimeException(
            "Output from external tool contains APs which are not consistent with the APs from the provided ltl formula");
      }

      return storedAutomaton;
    } catch (ParseException e) {
      throw new LtlParseException(
          String.format(
              "An error occured while parsing the output from the external tool '%s'",
              EXECUTABLE.getToolName()),
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
        String errMsg = readLinesFromStream(is);
        throw new LtlParseException(
            String.format(
                "Tool '%s' exited with error code %d. Message from tool:%n%s",
                EXECUTABLE.getToolName(),
                exitvalue,
                errMsg));
      }

      return is;
    } catch (IOException e) {
      throw new LtlParseException(e.getMessage(), e);
    }
  }

  /**
   * Convert an {@link InputStream} to a human-readable string. This is used to forward error
   * messages from the external tool to the logger.
   *
   * @return A readable string taken and transformed from the {@link InputStream}
   * @throws IOException In case an I/O error occurs
   */
  private String readLinesFromStream(InputStream is) throws IOException {
    try (BufferedReader br =
        new BufferedReader(new InputStreamReader(is, Charset.defaultCharset())); ) {
      return CharStreams.toString(br);
    }
  }

  private static class Converter {

    // '-H' lets the tool 'ltl3ba' print its output (the buechi-automaton) in HOA-format
    private static final Converter LTL3BA = new Converter("ltl3ba", "-H");

    private final String toolName;
    private final ImmutableList<String> commands;

    private Converter(String toolName, String... commands) {
      this.toolName = toolName;
      this.commands = ImmutableList.copyOf(commands);
    }

    public String getToolName() {
      return toolName;
    }

    private String execTool() {
      return "./" + getToolName();
    }

    public ImmutableList<String> getArgs() {
      return commands;
    }

  }
}
