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
package org.sosy_lab.cpachecker.cpa.smg.SMGJoin;


public enum SMGJoinStatus {
  EQUAL,
  LEFT_ENTAIL,
  RIGHT_ENTAIL,
  INCOMPARABLE,
  INCOMPLETE;

  public static SMGJoinStatus updateStatus(SMGJoinStatus pStatus1, SMGJoinStatus pStatus2) {
    if (pStatus1 == SMGJoinStatus.EQUAL) {
      return pStatus2;
    } else if (pStatus2 == SMGJoinStatus.EQUAL) {
      return pStatus1;
    } else if (pStatus1 == SMGJoinStatus.INCOMPARABLE ||
               pStatus2 == SMGJoinStatus.INCOMPARABLE ||
               pStatus1 != pStatus2) {
      return SMGJoinStatus.INCOMPARABLE;
    }
    return pStatus1;
  }
}
