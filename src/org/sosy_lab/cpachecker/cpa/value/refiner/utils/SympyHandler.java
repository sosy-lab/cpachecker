/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value.refiner.utils;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.ProcessExecutor;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Files.DeleteOnCloseFile;

/**
 * Utility class for handling interaction with Simpy for symbolic term
 * simplification.
 */
public class SympyHandler {

  /**
   * Private constructor in order to prevent instantiation.
   */
  private SympyHandler() {}

  // TODO Detect variables with names that have more then one letter as name

  /**
   * Simplifies a symbolic expression using the Pyhton lib Sympy
   * (<code>http://sympy.org/en/index.html</code>)
   *
   * @param exp the expression to simplify
   * @param logger the logger
   * @return the simplified expression or <code>null</code> if a problem occured
   * @throws IOException
   * @throws InterruptedException
   */
  public static String simplifyExpression(String exp, LogManager logger) {
    // Write python file with code for simplification
    List<String> vars = new ArrayList<>();
    StringBuilder pyc = new StringBuilder();
    pyc.append("from sympy import *\r\n\r\n");
    for (int i = 0; i < exp.length(); ++i) {
      String var = exp.substring(i, i + 1);
      // Is variable?
      if (var.matches("[a-zA-Z]")) {
        // Is not already present?
        if (!vars.contains(var)) {
          vars.add(var);
          pyc.append(var + " = symbols('" + var + "')\r\n");
        }
      }
    }
    pyc.append("\r\nz = " + exp + "\r\n\r\n");
    pyc.append("print(simplify(z))\r\n");

    List<String> result = null;

    try (DeleteOnCloseFile tmp = Files.createTempFile("pyfile", ".py")) {

      try (Writer w = Files.openOutputFile(tmp.toPath())) {
        w.write(pyc.toString());
      }
      // start process that uses tmp file
      //result = callScript(pathForSimpy, simplifiedExpression, tmp);
      // Script must be in Sympy folder in order to find the lib
      String[] args = { "python", "lib/python/sympy/sympyCaller.py" };
      ProcessExecutor<RuntimeException> pe = new ProcessExecutor<>(logger, RuntimeException.class, args);
      // Pass temp file name to caller script
      pe.print(tmp.toPath().toString());
      pe.sendEOF();
      pe.join();
      result = pe.getOutput();
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (result.size() > 0) {
      // System.out.println(result.get(0));
      return result.get(0);
    } else {
      return null;
    }
  }

  //  /**
  //   * @param pathForSimpy
  //   * @param simplifiedExpression
  //   * @param tmp
  //   * @return
  //   * @throws IOException
  //   * @throws InterruptedException
  //   */
  //  private static String callScript(String pathForSimpy,
  //      StringBuilder simplifiedExpression, DeleteOnCloseFile tmp) throws IOException,
  //      InterruptedException {
  //    // Execute python code in file, http://stackoverflow.com/a/1410779
  //    // Build command
  //    List<String> commands = new ArrayList<>();
  //    commands.add("py");
  //    // Add arguments
  //    //int t = tmp.toPath().toString().lastIndexOf("\\");
  //    //String name = tmp.toPath().toString().substring(t+1);
  //    //System.out.println(name);
  //    commands.add(tmp.toPath().toString());
  //    // Run macro on target
  //    ProcessBuilder pb = new ProcessBuilder(commands);
  //    pb.directory(new File(pathForSimpy));
  //    pb.redirectErrorStream(true);
  //    Process process = pb.start();
  //    // Read output
  //    StringBuilder out = new StringBuilder();
  //    BufferedReader br = new BufferedReader(new InputStreamReader(
  //        process.getInputStream()));
  //    String line = null;
  //    String previous = null;
  //    while ((line = br.readLine()) != null) {
  //      if (!line.equals(previous)) {
  //        previous = line;
  //        simplifiedExpression.append(line + "\r\n");
  //      }
  //    }
  //    // Check result
  //    if (process.waitFor() == 0) {
  //      return simplifiedExpression.toString();
  //    } else {
  //      // Abnormal termination: Log command parameters and output and throw
  //      // ExecutionException
  //      System.err.println(commands);
  //      System.err.println(out.toString());
  //      return null;
  //    }
  //  }

}
