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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
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
  * @param file the file to be parsed.
   */
  public ImmediateChecksParser(LogManager logger,String file) throws FileNotFoundException, IOException{
    set=new TreeSet<>();

    FileInputStream fstream;
    try {
      fstream = new FileInputStream(file);
      BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

      //Read File Line By Line
      String strLine= br.readLine();
      while (strLine  != null)   {
        if(strLine.contains(";")){
          int sem=strLine.indexOf(";");
          Variable var=new Variable(strLine.substring(0, sem));
          if(!set.contains(var)){
            set.add(var);
          }
        }
        strLine=br.readLine();
      }

      //Close the input stream
      br.close();
    } catch (Exception e) {
      logger.log(Level.WARNING,e);
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
