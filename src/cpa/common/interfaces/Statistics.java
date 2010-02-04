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
package cpa.common.interfaces;

import java.io.PrintWriter;

import cpa.common.ReachedElements;

/**
 * A class to hold statistics of the analysis
 * @author alb
 */
public interface Statistics {
    
    public static enum Result { UNKNOWN, UNSAFE, SAFE }

    /**
     * Prints this group of statistics using the given writer
     * @param out the writer to use for printing the statistics
     * @param result the result of the analysis 
     * @param reached the final reached set
     */
    public void printStatistics(PrintWriter out, Result result, ReachedElements reached);

    /**
     * @return The name for this group of statistics
     */
    public String getName();
}
