/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.speci;

public class AutomatonEdge {

  //private final int source;
  private final int sink;

  private final String statement;

  public AutomatonEdge(/*int source,*/ int sink, String statement) {

    this.sink = sink;
    //this.source = source;
    this.statement = statement;
  }

  /*
  public final int getSource() {
    return source;
  }*/

  /**
   * @return the sink
   */
  public final int getSink() {
    return sink;
  }

  /**
   * @return the statement
   */
  public final String getStatement() {
    return statement;
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) { return true; }

    if (!(pObj instanceof AutomatonEdge)) { return false; }

    AutomatonEdge other = (AutomatonEdge) pObj;

    return (sink == other.sink && statement.equals(other.statement));

  }
}
