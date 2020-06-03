/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package LoopAcc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.sosy_lab.cpachecker.cfa.CFA;

public class CFileChanger {

  private File file = new File("/home/bensky/cpachecker/doc/examples/exampleBeneChangeFile.c");
  private String content;
  private BufferedReader reader;
  private LoopInformation sFL;

  private final int counterStart = 1;
  private final String emptyString = "";

  public CFileChanger(CFA cfa) throws FileNotFoundException {
    // sFL = new LoopInformation(cfa);
    // content = "";
    // reader = new BufferedReader(new FileReader(file));
    // readline();

  }

  private void readline() {
    String line = emptyString;
    int counter = counterStart;
    try {
      line = reader.readLine();
    } catch (IOException e) {
      // TODO Auto-generated catch block
    }

    while (line != null) {

      LoopData temp = sFL.getLoopData().get(0);

      if (counter == temp.getLoopStart().getNodeNumber()) {
        content = content + "if(" + temp.getCondition() + "){" + System.lineSeparator();
        content =
            content
                + "nondet("
                + temp.outputToString()
                + ");"
                + System.lineSeparator();
        content = content + "assume(" + temp.getCondition() + ");" + System.lineSeparator();
      } else if (counter == temp.getLoopEnd().getNodeNumber() - 2) {
        content = content + "assume(!(" + temp.getCondition() + "));" + System.lineSeparator();
        content = content + "}" + System.lineSeparator();

      } else {
      content = content + line + System.lineSeparator();
      }

      counter++;
      try {
        line = reader.readLine();
      } catch (IOException e) {
        // TODO Auto-generated catch block
      }
    }

  }
}
