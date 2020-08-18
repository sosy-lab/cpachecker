// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.ifcsecurity.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;

/**
 * Used for Parsing those Variables/functions that should be checked for security violation at every state and not only at the end.
 */
public class ImmediateChecksParser {

  /**
   * Internal set of Variables/functions that should be checked for security violation at every
   * state.
   */
  private NavigableSet<Variable> set = new TreeSet<>();

  /**
   * Starts and execute the ImmediateChecksParser for parsing those Variables/functions that should be checked for security violation at every state.
  * @param pFile the file to be parsed.
   */
  @SuppressWarnings("resource")
  public ImmediateChecksParser(LogManager pLogger,Path pFile){
    set=new TreeSet<>();

    List<String> contents = null;
    try {
      contents = Files.readAllLines(pFile, Charset.defaultCharset());
    } catch (IOException e) {
      pLogger.logUserException(
          Level.WARNING, e, "Could not read intial security mapping from file named " + pFile);
      return ;
    }

    for (String strLine : contents) {
      if (strLine.trim().isEmpty()) {
        continue;

      } else if(strLine.contains(";")){
        int sem=strLine.indexOf(";");
        Variable var=new Variable(strLine.substring(0, sem));
        set.add(var);
      }
    }
  }

  /**
   * Returns the set of Variables/function that should be checked for security violation at every
   * state.
   *
   * @return set of Variables/functions.
   */
  public NavigableSet<Variable> getSet() {
    return set;
  }
}
