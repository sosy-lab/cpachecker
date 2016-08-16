/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.ifcsecurity.util;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

/**
 * Used for Parsing those Variables/functions that should be checked for security violation at every state and not only at the end.
 */
public class ImmediateChecksParser {

  /**
   * Internal set of Variables/functions that should be checked for security violation at every state.
   */
  private SortedSet<Variable> set=new TreeSet<>();

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
      pLogger.logUserException(Level.WARNING, e, "Could not read intial security mapping from file named " + pFile);
      return ;
    }

    for (String strLine : contents) {
      if (strLine.trim().isEmpty()) {
        continue;

      } else if(strLine.contains(";")){
        int sem=strLine.indexOf(";");
        Variable var=new Variable(strLine.substring(0, sem));
        if(!set.contains(var)){
          set.add(var);
        }
      }
    }
  }

  /**
   * Returns the set of Variables/function that should be checked for security violation at every state.
   * @return set of Variables/functions.
   */
  public SortedSet<Variable> getSet(){
    return set;
  }
}
