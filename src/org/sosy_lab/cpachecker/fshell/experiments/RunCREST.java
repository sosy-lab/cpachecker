/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.fshell.experiments;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class RunCREST {

  public static void main(String[] args) throws IOException, InterruptedException {
    String lSourceFileName = "test.c";

    File lSourceFile = new File(lSourceFileName);

    File lTemporaryDirectory = new File("tmp-testing");

    if (lTemporaryDirectory.exists()) {
      throw new RuntimeException();
    }

    if (!lTemporaryDirectory.mkdir()) {
      throw new RuntimeException();
    }


    // TODO implement test generation process
    // we assume the source file ends with .c

    String lFilename = lSourceFile.getName();
    int lFilenameLength = lFilename.length();

    if (!lFilename.substring(lFilenameLength - 2, lFilenameLength).equals(".c")) {
      throw new RuntimeException();
    }

    String lBasename = lFilename.substring(0, lFilenameLength - 2);

    File lCrestFile = new File(lTemporaryDirectory, lBasename + "-crest.c");

    List<String> lCommand = new LinkedList<String>();

    lCommand.add("gcc");
    lCommand.add("-E");
    lCommand.add("-D__CREST__");
    lCommand.add(lSourceFile.getAbsolutePath());
    lCommand.add("-o");
    lCommand.add(lCrestFile.getAbsolutePath());

    ProcessBuilder lBuilder = new ProcessBuilder();
    lBuilder.command(lCommand);
    lBuilder.redirectErrorStream(true);

    Process lGCC = lBuilder.start();

    // TODO read input

    int lReturnValue = lGCC.waitFor();

    // TODO check return value

    lCommand.clear();
    lCommand.add("crestc");
    lCommand.add(lCrestFile.getAbsolutePath());

    lBuilder.command(lCommand);

    Process lCrestC = lBuilder.start();

    // TODO read input

    lReturnValue = lCrestC.waitFor();

    // TODO check return value

    lCommand.clear();
    lCommand.add("run_crest");
    lCommand.add("." + File.separator + lCrestFile.getName().substring(0, lCrestFile.getName().length() - 2));
    lCommand.add("100");
    lCommand.add("-dfs");

    lBuilder.command(lCommand);

    Process lRunCrest = lBuilder.start();

    // TODO read input

    lReturnValue = lRunCrest.waitFor();

    // TODO check return value

    // TODO transform test suite

    // TODO check coverage

    // cleanup
    if (!lTemporaryDirectory.delete()) {
      throw new RuntimeException();
    }
  }

}
