/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.usage;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo.Access;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * Information about special functions like sdlFirst() and sdlNext();
 */
public class BinderFunctionInfo {

  public static class ParameterInfo {
    public final Access access;
    public final int dereference;

    public ParameterInfo(Access a, int d) {
      access = a;
      dereference = d;
    }
  }

  public static class LinkerInfo {
    public final int num;
    public final int dereference;

    LinkerInfo(int p, int d) {
      num = p;
      dereference = d;
    }
  }

  String name;
  int parameters;
  public final ImmutableList<ParameterInfo> pInfo;
  /*
   * 0 - before equal,
   * 1 - first parameter, etc..
   */
  public final Pair<LinkerInfo, LinkerInfo> linkInfo;

  @SuppressWarnings("deprecation")
  BinderFunctionInfo(String nm, Configuration pConfig, LogManager l) {
    name = nm;
    try {
      parameters = Integer.parseInt(pConfig.getProperty(name + ".parameters"));
      String line = pConfig.getProperty(name + ".pInfo");
      Preconditions.checkNotNull(line);
      String[] options = line.split(", *");
      String[] pOption;
      List<ParameterInfo> tmpInfo = new LinkedList<>();
      for (String option : options) {
        pOption = option.split(":");
        if (pOption.length == 1) {
          tmpInfo.add(new ParameterInfo(Access.valueOf(pOption[0]), 0));
        } else {
          tmpInfo.add(new ParameterInfo(Access.valueOf(pOption[0]), Integer.parseInt(pOption[1])));
        }
      }
      line = pConfig.getProperty(name + ".linkInfo");
      if (line != null) {
        options = line.split(", *");
        assert options.length == 2;
        LinkerInfo[] lInfo = new LinkerInfo[2];
        for (int i = 0; i < 2; i++) {
          pOption = options[i].split(":");
          if (pOption.length == 1) {
            lInfo[i] = new LinkerInfo(Integer.parseInt(pOption[0]), 0);
          } else {
            lInfo[i] = new LinkerInfo(Integer.parseInt(pOption[0]), Integer.parseInt(pOption[1]));
          }
        }
        linkInfo = Pair.of(lInfo[0], lInfo[1]);
      } else {
        linkInfo = null;
      }
      pInfo = ImmutableList.copyOf(tmpInfo);
    } catch (NumberFormatException e) {
      l.log(Level.WARNING, "No information about parameters in " + name + " function");
      throw e;
    }
  }
}
