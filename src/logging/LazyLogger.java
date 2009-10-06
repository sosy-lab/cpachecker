/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package logging;

import java.util.logging.Level;

import logging.CPACheckerLogger;


/**
 * A class that uses a sort-of "lazy evaluation" for constructing (and
 * printing) log messages. Sometimes, simply building the string
 * representation of some objects might be costly (for instance, if we want to
 * print big MathSAT formulas). If logging is turned off, there's no need of
 * constructing this string representation if arguments to the log function
 * are evaluated "lazily". This is what this class provides. The logging
 * itself is performed by calling CPACheckerLogger
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class LazyLogger {
    
    // TODO: move to CPACheckerLogger or CustomLogLevel
    private static class LogLevel extends Level {
        
        private static final long serialVersionUID = -2573882076078285905L;

        public LogLevel(String name, int value) {
            super(name, value);
        }
    }
    
    public static Level DEBUG_1 = new LogLevel("AG_DEBUG_1",
            Level.FINE.intValue()-10);
    public static Level DEBUG_2 = new LogLevel("AG_DEBUG_2",
            Level.FINE.intValue()-20);
    public static Level DEBUG_3 = new LogLevel("AG_DEBUG_3",
            Level.FINE.intValue()-30);
    public static Level DEBUG_4 = new LogLevel("AG_DEBUG_4",
            Level.FINE.intValue()-40);

    public static void log(Level lvl, Object... args) {
        if (CPACheckerLogger.getLevel() <= lvl.intValue()) {
            if (args.length == 1) {
              CPACheckerLogger.log(lvl, args[0].toString());
              
            } else {
              StringBuffer buf = new StringBuffer();
              for (Object o : args) {
                  buf.append(o.toString());
              }
              CPACheckerLogger.log(lvl, buf.toString());
            }
        }
    }
}
