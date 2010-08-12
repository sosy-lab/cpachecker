/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.fllesh;

import java.io.File;
import java.io.IOException;

import org.sosy_lab.cpachecker.fllesh.util.Cilly;

public class Main {
  
  public static FlleShResult mResult = null;
  
  public static void main(String[] pArguments) throws IOException {
    assert(pArguments != null);
    assert(pArguments.length > 1);
    
    mResult = null;
    
    String lFQLSpecificationString = pArguments[0];
    String lSourceFileName = pArguments[1];
    
    String lEntryFunction = "main";
    
    if (pArguments.length > 2) {
      lEntryFunction = pArguments[2];
    }
    
    // TODO implement nicer mechanism for disabling cilly preprocessing
    if (pArguments.length <= 3) {  
      // check cilly invariance of source file, i.e., is it changed when preprocessed by cilly?
      Cilly lCilly = new Cilly();
  
      if (!lCilly.isCillyInvariant(lSourceFileName)) {
        File lCillyProcessedFile = lCilly.cillyfy(lSourceFileName);
        //lCillyProcessedFile.deleteOnExit();
  
        lSourceFileName = lCillyProcessedFile.getAbsolutePath();
  
        System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
      }
    }

    mResult = FlleSh.run(lSourceFileName, lFQLSpecificationString, lEntryFunction, false);
    
    System.out.println("#Goals: " + mResult.getTask().getNumberOfTestGoals() + ", #Feas: " + mResult.getNumberOfFeasibleTestGoals() + ", #Infeas: " + mResult.getNumberOfInfeasibleTestGoals());
  }
  
}

