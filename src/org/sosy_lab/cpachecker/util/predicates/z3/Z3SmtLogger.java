/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.z3;

import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

@Options
public class Z3SmtLogger {

  @Option(name = "logfile", description = "Export solver queries in Smtlib2 format.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File basicLogfile = new File("z3smtlog.%d.smt2");
  private static int logfileCounter = 0;

  private File logfile;

  private final long z3context;
  private final HashSet<Long> declarations = Sets.newHashSet();

  private int itpIndex = 0; // each interpolation gets its own index
  private final HashMap<Long, String> interpolationGroups = Maps.newHashMap(); // for mathsat-compatibility

  public Z3SmtLogger(long z3context, Configuration config) throws InvalidConfigurationException {
    config.inject(this);
    this.z3context = z3context;
    initLogfile(basicLogfile);
  }

  /** copy constructor */
  public Z3SmtLogger(Z3SmtLogger original) {
    this.z3context = original.z3context;
    initLogfile(original.basicLogfile);
  }

  private void initLogfile(File basicLogfile) {
    if (basicLogfile == null) { // option noout
      this.logfile = null;
    } else {
      String filename = String.format(basicLogfile.getAbsolutePath(), logfileCounter++);
      this.logfile = new File(filename);
      log("", false); // create or clean the file
    }
  }

  public void logOption(String option, String value) {
    if (logfile == null) { return; }
    logBracket("set-option :" + option + " " + value);
  }

  public void logDeclaration(long name, long returnType, long... inputTypes) {
    if (logfile == null) { return; }
    if (declarations.add(name)) {
      String s = "declare-fun " + ast_to_string(z3context, name) + " (";
      for (long it : inputTypes) {
        s += sort_to_string(z3context, it) + " ";
      }
      s += ") " + sort_to_string(z3context, returnType);

      logBracket(s);
    }
  }

  public void logPush(int n) {
    if (logfile == null) { return; }
    logBracket("push " + n);
  }

  public void logPop(int n) {
    if (logfile == null) { return; }
    logBracket("pop " + n);
  }

  public void logAssert(long expr) {
    //    if (smtlogfile == null) { return; }
    //    logBracket("assert " + ast_to_string(z3context, expr));
    logInterpolationAssert(expr); // for mathsat, to use "one" declarations-file
  }

  public void logInterpolationAssert(long expr) {
    if (logfile == null) { return; }
    itpIndex++;
    String name = "g" + itpIndex;
    logBracket("assert (! " + ast_to_string(z3context, expr) + " :interpolation-group " + name + ")");
    interpolationGroups.put(expr, name);
  }

  public void logCheck() {
    if (logfile == null) { return; }
    logBracket("check-sat");
  }

  public void logGetModel() {
    if (logfile == null) { return; }
    logBracket("get-model");
  }


  public void logInterpolation(List<Long> formulasOfA) {
    if (logfile == null) { return; }

    // this is a mathsat-specific output!
    String getItps = "get-interpolant (";

    for (long f : formulasOfA) {
      Preconditions.checkArgument(interpolationGroups.containsKey(f));
      getItps += interpolationGroups.get(f) + " ";
    }

    getItps += ")";

    logCheck();
    logBracket(getItps);
  }

  public void logBracket(String s) {
    if (logfile == null) { return; }
    log("(" + s + ")\n", true);
  }

  private synchronized void log(String s, boolean append) {
    try {
      if (append) {
        Files.append(s, logfile, Charset.defaultCharset());
      } else {
        Files.write(s, logfile, Charset.defaultCharset());
      }
    } catch (IOException e) {
      throw new AssertionError("IO-Error in smtlogfile");
    }
  }

}
