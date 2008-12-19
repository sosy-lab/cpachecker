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

import java.util.List;
import java.util.logging.Level;

import logging.CustomLogLevel;

public class CustomLogLevel extends Level{

	private static final long serialVersionUID = 6137713281684646662L;
	public static Level MainApplicationLevel;
	public static Level CFABuilderLevel;
	public static Level CentralCPAAlgorithmLevel;
	public static Level CompositeCPALevel;
	public static Level SpecificCPALevel;
	public static Level ExternalToolLevel;

	protected CustomLogLevel(String name, int value) {
		super(name, value);
	}

	protected static void initializeLevels(List<String> levels){
		if(levels.contains("MainApplicationLevel")){
			MainApplicationLevel = new CustomLogLevel("MainApplicationLevel", Level.FINE.intValue());
		}
		else{
			MainApplicationLevel = new CustomLogLevel("MainApplicationLevel", Level.FINEST.intValue());
		}
		if(levels.contains("CFABuilderLevel")){
			CFABuilderLevel = new CustomLogLevel("CFABuilderLevel", Level.FINE.intValue());
		}
		else{
			CFABuilderLevel = new CustomLogLevel("CFABuilderLevel", Level.FINEST.intValue());
		}
		if(levels.contains("CentralCPAAlgorithmLevel")){
			CentralCPAAlgorithmLevel = new CustomLogLevel("CentralCPAAlgorithmLevel", Level.FINE.intValue());
		}
		else{
			CentralCPAAlgorithmLevel = new CustomLogLevel("CentralCPAAlgorithmLevel", Level.FINEST.intValue());
		}
		if(levels.contains("CompositeCPALevel")){
			CompositeCPALevel = new CustomLogLevel("CompositeCPALevel", Level.FINE.intValue());
		}
		else{
			CompositeCPALevel = new CustomLogLevel("CompositeCPALevel", Level.FINEST.intValue());
		}
		if(levels.contains("SpecificCPALevel")){
			SpecificCPALevel = new CustomLogLevel("SpecificCPALevel", Level.FINE.intValue());
		}
		else{
			SpecificCPALevel = new CustomLogLevel("SpecificCPALevel", Level.FINEST.intValue());
		}
		if(levels.contains("ExternalToolLevel")){
			ExternalToolLevel = new CustomLogLevel("ExternalToolLevel", Level.FINE.intValue());
		}
		else{
			ExternalToolLevel = new CustomLogLevel("ExternalToolLevel", Level.FINEST.intValue());
		}
	}
}