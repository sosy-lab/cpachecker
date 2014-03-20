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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Files.DeleteOnCloseFile;

/**
 * Utility class for handling interaction with Simpy for symbolic term
 * simplification.
 */
public class SimpyHandler {

  /**
   * Private constructor in order to prevent instantiation.
   */
  private SimpyHandler() {
  }

  // TODO Install Sympy in CPAchecker, set corrent path for Simpy
  // TODO Detect variables with names that have more then one letter as name

  /**
   * Simplifies a symbolic expression using the Pyhton lib Sympy
   * ({@link "http://sympy.org/en/index.html"})
   *
   * @param exp the expression to simplify
   * @return the simplified expression or <code>null</code> if a problem occured
   * @throws IOException
   * @throws InterruptedException
   */
  public static String simplifyExpression(String exp) throws IOException, InterruptedException {
    String pathForSimpy = "E:\\Uni\\eclipse-SDK-4.2-win32\\workspace1\\CPAchecker\\lib\\python\\sympy";
    StringBuilder simplifiedExpression = new StringBuilder();
    //exp = "(4 * a) + a + b + c + d";
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

    String result = null;

    try (DeleteOnCloseFile tmp = Files.createTempFile("pyfile", ".py")) {

      try (Writer w = Files.openOutputFile(tmp.toPath())) {
        w.write(pyc.toString());
      }
      // start process that uses tmp file
      result = callScript(pathForSimpy, simplifiedExpression);
//      LogManagerWithoutDuplicates l = new LogManagerWithoutDuplicates(null);
//      ProcessExecutor<IOException> pe = new ProcessExecutor<>(l, IOException.class, "py " + tmp);
//      pe.join();
//      if (pe.isFinished()) {
//        List<String> results = pe.getOutput();
//        return results.get(0);
//      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return result;
  }

  /**
   * @param pathForSimpy
   * @param simplifiedExpression
   * @return
   * @throws IOException
   * @throws InterruptedException
   */
  private static String callScript(String pathForSimpy, StringBuilder simplifiedExpression) throws IOException,
      InterruptedException {
    // Execute python code in file, http://stackoverflow.com/a/1410779
    // Build command
    List<String> commands = new ArrayList<>();
    commands.add("py");
    // Add arguments
    commands.add("pyfile.py");
    // Run macro on target
    ProcessBuilder pb = new ProcessBuilder(commands);
    pb.directory(new File(pathForSimpy));
    pb.redirectErrorStream(true);
    Process process = pb.start();
    // Read output
    StringBuilder out = new StringBuilder();
    BufferedReader br = new BufferedReader(new InputStreamReader(
        process.getInputStream()));
    String line = null;
    String previous = null;
    while ((line = br.readLine()) != null) {
      if (!line.equals(previous)) {
        previous = line;
        simplifiedExpression.append(line + "\r\n");
      }
    }
    // Check result
    if (process.waitFor() == 0) {
      return simplifiedExpression.toString();
    } else {
      // Abnormal termination: Log command parameters and output and throw
      // ExecutionException
      System.err.println(commands);
      System.err.println(out.toString());
      return null;
    }
  }

}
