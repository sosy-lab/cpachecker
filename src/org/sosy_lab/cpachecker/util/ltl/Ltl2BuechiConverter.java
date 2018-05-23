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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;
import jhoafparser.consumer.HOAConsumerPrint;
import jhoafparser.consumer.HOAConsumerStore;
import jhoafparser.parser.HOAFParser;
import jhoafparser.parser.generated.ParseException;
import jhoafparser.storage.StoredAutomaton;
import org.sosy_lab.common.NativeLibraries;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.util.ltl.formulas.LtlFormula;

public class Ltl2BuechiConverter {

  private static final String LTL3BA = "./ltl3ba";

  private final LtlFormula ltlFormula;
  private final ProcessBuilder builder;

  public static Automaton convertFormula(LtlFormula pFormula)
      throws InterruptedException, IOException, LtlParseException {
    Objects.requireNonNull(pFormula);

    StoredAutomaton storedAutomaton;
    Ltl2BuechiConverter converter = new Ltl2BuechiConverter(pFormula);

    try (InputStream is = converter.runLtlExec()) {

      HOAConsumerStore consumer = new HOAConsumerStore();
      HOAFParser.parseHOA(is, consumer);
      storedAutomaton = consumer.getStoredAutomaton();
      storedAutomaton.feedToConsumer(new HOAConsumerPrint(System.out));

    } catch (ParseException e) {
      throw new LtlParseException(
          String.format(
              "An error occured while parsing the output from the external tool '%s'", LTL3BA),
          e);
    }

    //    return LtlParserUtils.transform(storedAutomaton);
    return null;
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

  private InputStream runLtlExec() throws IOException, InterruptedException, LtlParseException {
    Process process = builder.start();
    InputStream is = process.getInputStream();

    int exitvalue = process.waitFor();
    if (exitvalue != 0) {
      throw new LtlParseException(
          String.format("Tool %s exited with error code: %d", LTL3BA, exitvalue));
    }

    return is;
  }
}
