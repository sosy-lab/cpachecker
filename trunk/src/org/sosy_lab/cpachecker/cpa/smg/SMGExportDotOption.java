/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg;

import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.cpachecker.cpa.smg.SMGCPA.SMGExportLevel;

import java.nio.file.Path;

public class SMGExportDotOption {

  private static final SMGExportDotOption NO_EXPORT =
      new SMGExportDotOption(null, SMGExportLevel.NEVER);

  private final SMGExportLevel exportSMG;

  private PathTemplate exportSMGFilePattern;
  private boolean isRefinment;
  private int refinment_id;

  public SMGExportDotOption(PathTemplate pExportSMGFilePattern, SMGExportLevel pExportSMG) {
    exportSMGFilePattern = pExportSMGFilePattern;
    exportSMG = pExportSMG;
  }

  public PathTemplate getExportSMGFilePattern() {
    return exportSMGFilePattern;
  }

  public void exportSMGSeperatedByRefinments(PathTemplate pExportSMGFilePattern) {
    exportSMGFilePattern = pExportSMGFilePattern;
    refinment_id = 0;
  }

  public void nextRefinment() {
    refinment_id = refinment_id + 1;
  }

  public boolean exportSMG(SMGExportLevel pLevel) {
    return pLevel == exportSMG && exportSMGFilePattern != null;
  }

  @Override
  public String toString() {
    return "SMGExportDotOption [exportSMG=" + exportSMG + ", exportSMGFilePattern="
        + exportSMGFilePattern + ", isRefinment=" + isRefinment + ", refinment_counter="
        + refinment_id + "]";
  }

  public Path getOutputFilePath(String pSMGName) {

    if (isRefinment) {
      return exportSMGFilePattern.getPath(refinment_id, pSMGName);
    } else {
      return exportSMGFilePattern.getPath(pSMGName);
    }
  }

  public boolean hasExportPath() {
    return exportSMGFilePattern != null;
  }

  public static SMGExportDotOption getNoExportInstance() {
    return NO_EXPORT;
  }

  public void changeToRefinment(PathTemplate pNewPathTemplate) {
    exportSMGFilePattern = pNewPathTemplate;
    isRefinment = true;
    refinment_id = 0;
  }
}