// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.giaTestcaseGen;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.giageneration.GIAGenerator;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class TestCaseGenState
    implements LatticeAbstractState<TestCaseGenState>, Serializable, Graphable {

  private static final long serialVersionUID = -7715698130885640052L;
  private final LogManager logger;

  private final List<TestcaseEntry> entries;
  private Optional<AutomatonState> automatonState;

  public TestCaseGenState(LogManager pLogger) {
    this.entries = new ArrayList<>();
    automatonState = Optional.empty();
    this.logger = pLogger;
  }

  private TestCaseGenState(
      List<TestcaseEntry> pEntries, Optional<AutomatonState> pAutomatonState, LogManager pLogger) {
    this.automatonState = pAutomatonState;
    this.entries = pEntries;
    this.logger = pLogger;
  }

  public void setAutomatonState(Optional<AutomatonState> pAutomatonState) {
    automatonState = pAutomatonState;
  }

  public void addNewTestcase(TestcaseEntry entry) {
    entries.add(entry);
  }

  public TestCaseGenState copy() {
    List<TestcaseEntry> copied = new ArrayList<>();
    entries.forEach(e -> copied.add(e.copy()));
    return new TestCaseGenState(copied, automatonState, logger);
  }

  @Override
  public TestCaseGenState join(TestCaseGenState other) throws InterruptedException {
    logger.log(
        Level.WARNING, "Merging of TestCaseGenStates is not supported! Returning the other state");
    return other;
  }

  @Override
  public boolean isLessOrEqual(TestCaseGenState other) throws CPAException, InterruptedException {
    if (other.entries.size() < this.entries.size()) {
      return false;
    }
    for (int i = 0; i < this.entries.size(); i++) {
      if (!other.entries.get(i).equals(this.entries.get(i))) {
        return false;
      }
    }
    // We don't need to compare the automatonState, as they are not relevant for less or equal
    return true;
  }

  @Override
  public String toDOTLabel() {
    return String.format(
        "[%s], \n ++%s++",
        entries.stream().map(e -> e.getValue()).collect(Collectors.joining(",")),
        this.automatonState.isPresent() ? this.automatonState.get().getInternalStateName() : "");
  }

  @Override
  public boolean shouldBeHighlighted() {
    if (this.automatonState.isPresent()) {
      return this.automatonState
          .get()
          .getInternalStateName()
          .equals(GIAGenerator.NAME_OF_NEWTESTINPUT_STATE);
    }
    return false;
  }

  public boolean isNewTestCaseState() {
    if (this.automatonState.isPresent()) {
      return this.automatonState
          .get()
          .getInternalStateName()
          .equals(GIAGenerator.NAME_OF_NEWTESTINPUT_STATE);
    }
    return false;
  }

  /**
   * Dumps the currently stored testcase to a file using the Testcomp testcase format. The result
   * will look similar to: <?xml version="1.0" encoding="UTF-8" standalone="no"?> <!DOCTYPE testcase
   * PUBLIC "+//IDN sosy-lab.org//DTD test-format testcase 1.1//EN"
   * "https://sosy-lab.org/test-format/testcase-1.1.dtd"> <testcase> <input variable="x"
   * type="int">1023</input> <input variable="y" type="unsigned char">254</input> </testcase>
   *
   * @param pPath the path to store the testcase at
   */
  public void dumpToTestcase(Path pPath) throws IOException {
    StringBuilder sb = new StringBuilder();
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
    sb.append(System.lineSeparator());
    sb.append(
        "<!DOCTYPE testcase PUBLIC \"+//IDN sosy-lab.org//DTD test-format testcase 1.1//EN\""
            + " \"https://sosy-lab.org/test-format/testcase-1.1.dtd\">");
    sb.append(System.lineSeparator());
    sb.append("<testcase>");
    sb.append(System.lineSeparator());
    for (TestcaseEntry entry : entries) {
      sb.append(entry.toXMLTestcaseLine());
      sb.append(System.lineSeparator());
    }
    sb.append("</testcase>");
    IO.writeFile(pPath, Charset.defaultCharset(), sb.toString());
  }
}
