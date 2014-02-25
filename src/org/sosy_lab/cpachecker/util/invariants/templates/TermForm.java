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
package org.sosy_lab.cpachecker.util.invariants.templates;

public class TermForm {

  private final TemplateTerm asTemplate;
  private final TemplateTerm asForm;

  public TermForm(TemplateTerm t) {
    t = t.copy();
    t.generalize();
    asTemplate = t;

    t = t.copy();
    t.writeAsForm(true);
    asForm = t;
  }

  @Override
  public boolean equals(Object o) {
    boolean ans = false;
    if (o instanceof TermForm) {
      TermForm f = (TermForm) o;
      String s1 = toString();
      String s2 = f.toString();
      ans = s1.equals(s2);
    }
    return ans;
  }

  /**
   * HashSet only looks to the equals method if the hashCodes of the
   * two objects are the same.
   */
  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public String toString() {
    return asForm.toString();
  }

  public TemplateTerm getTemplate() {
    return asTemplate;
  }

}
