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
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.CFA;

public class CFileReader {

  private CFA cfa;
  private List<Path> fileLocation;
  private String content;
  private BufferedReader reader;

  public CFileReader(CFA cfa) {
    this.cfa = cfa;
    fileLocation = cfa.getFileNames();
    content = "";
    readFile();
  }

  private void readFile() {
    for (Path p : fileLocation) {
      try {
        BufferedReader reader = new BufferedReader(new FileReader(p.toFile()));
        String line = "";
        while (line != null) {
          line = reader.readLine();
          content = content + line + System.lineSeparator();
        }
      } catch (IOException e) {
        // TODO Auto-generated catch block
      }
    }
  }

  public String getContentString() {
    return content;
  }

  public char[] getContentCharArray() {
    return content.toCharArray();
  }

}
