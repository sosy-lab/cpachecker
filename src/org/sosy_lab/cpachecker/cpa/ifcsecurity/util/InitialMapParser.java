// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.ifcsecurity.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.policies.PredefinedPolicies;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.policies.SecurityClasses;

/**
 * Used for Parsing the allowed SecurityClass-Mapping.
 */
public class InitialMapParser {

  /**
   * Internal variable that contains the SecurityClass-Mapping
   */
  private Map<Variable,SecurityClasses> map=new TreeMap<>();

  /**
   * Starts and execute the InitialMapParser for parsing the allowed SecurityClass-Mapping.
  * @param pFile the file to be parsed.
   */
  @SuppressWarnings("resource")
  public InitialMapParser(LogManager pLogger, Path pFile) {
    map=new TreeMap<>();

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

      } else if(strLine.contains("=") && strLine.contains(";")){
        int eqsign=strLine.indexOf("=");
        int sem=strLine.indexOf(";");
        assert(eqsign<sem);
        Variable var=new Variable(strLine.substring(0, eqsign));
        try {
          Field f = PredefinedPolicies.class.getField(strLine.substring(eqsign+1, sem));
          SecurityClasses clas = (SecurityClasses) f.get(null);
          if(!map.containsKey(var)){
            map.put(var, clas);
          }
        } catch (NoSuchFieldException e) {
          pLogger.logUserException(Level.WARNING, e, "");
        } catch (SecurityException e) {
          pLogger.logUserException(Level.WARNING, e, "");
        } catch (IllegalArgumentException e) {
          pLogger.logUserException(Level.WARNING, e, "");
        } catch (IllegalAccessException e) {
          pLogger.logUserException(Level.WARNING, e, "");
        }

      }
    }
  }

  /**
   * Returns the parsed allowed SecurityClass-Mapping.
   * @return SecurityClass-Mapping.
   */
  public Map<Variable,SecurityClasses> getInitialMap(){
    return map;
  }
}
