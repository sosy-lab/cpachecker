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

import java.nio.file.Path;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.cpachecker.cpa.smg.SMGOptions.SMGExportLevel;

public class SMGExportDotOption {

  private static final SMGExportDotOption NO_EXPORT =
      new SMGExportDotOption(null, SMGExportLevel.NEVER);

  private final SMGExportLevel exportSMG;

  private PathTemplate exportSMGFilePattern;
  private boolean isRefinement;
  private int refinement_id;

  public SMGExportDotOption(PathTemplate pExportSMGFilePattern, SMGExportLevel pExportSMG) {
    exportSMGFilePattern = pExportSMGFilePattern;
    exportSMG = pExportSMG;
  }

  public PathTemplate getExportSMGFilePattern() {
    return exportSMGFilePattern;
  }

  public void exportSMGSeperatedByRefinements(PathTemplate pExportSMGFilePattern) {
    exportSMGFilePattern = pExportSMGFilePattern;
    refinement_id = 0;
  }

  public void nextRefinement() {
    refinement_id = refinement_id + 1;
  }

  public boolean exportSMG(SMGExportLevel pLevel) {
    return pLevel.compareTo(exportSMG) <= 0 && exportSMGFilePattern != null;
  }

  @Override
  public String toString() {
    return "SMGExportDotOption [exportSMG=" + exportSMG + ", exportSMGFilePattern="
        + exportSMGFilePattern + ", isRefinement=" + isRefinement + ", refinement_counter="
        + refinement_id + "]";
  }

  public Path getOutputFilePath(String pSMGName) {

    if (isRefinement) {
      return exportSMGFilePattern.getPath(refinement_id, pSMGName);
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

  public void changeToRefinement(PathTemplate pNewPathTemplate) {
    exportSMGFilePattern = pNewPathTemplate;
    isRefinement = true;
    refinement_id = 0;
  }
}