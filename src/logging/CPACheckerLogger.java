package logging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import logging.CustomLogLevel;

import cmdline.CPAMain;


public class CPACheckerLogger {
    
        public static class ConfigLevel extends Level {

            public ConfigLevel(int val) {
                super("ConfigLevel", val);
            }
            
            public static ConfigLevel create(String configVal) {
                if (configVal.toLowerCase().equals("off")) {
                    return new ConfigLevel(Level.OFF.intValue());
                } else if (configVal.toLowerCase().equals("on")) {
                    return new ConfigLevel(Level.FINE.intValue());
                } else if (configVal.toLowerCase().equals("all")) {
                    return new ConfigLevel(Level.ALL.intValue());
                } else if (configVal.toLowerCase().equals("info")) {
                    return new ConfigLevel(Level.INFO.intValue());
                } else if (configVal.toLowerCase().equals("fine")) {
                    return new ConfigLevel(Level.FINE.intValue());
                } else if (configVal.toLowerCase().equals("finer")) {
                    return new ConfigLevel(Level.FINER.intValue());
                } else if (configVal.toLowerCase().equals("finest")) {
                    return new ConfigLevel(Level.FINEST.intValue());
                } else {
                    int val = Level.ALL.intValue();
                    try {
                        val = Integer.parseInt(configVal);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                    return new ConfigLevel(val);
                }
            }

            /**
             * 
             */
            private static final long serialVersionUID = 3305833981214128835L;
        }
	
	public static Logger mainLogger = Logger.getLogger(CPAMain.cpaConfig.getProperty("log.path"));
	private static FileHandler fileHandler = null;
	public static void init(){
		// TODO read from config // Array that includes levels to include
		ArrayList<String> levelList = new ArrayList<String>();
//		levelList.add("MainApplicationLevel");
//		levelList.add("CentralCPAAlgorithmLevel");
		levelList.add("SpecificCPALevel");
//		levelList.add("ExternalToolLevel");
		CustomLogLevel.initializeLevels(levelList);
		/*
		FileWriter writer;
	    try {
	      writer = new FileWriter("/tmp/myFile.txt");
	      writer.write(CPAMain.cpaConfig.getProperty("log.path"));
	      writer.close();
	    } catch (IOException e) {
	      // TODO Auto-generated catch block
	      e.printStackTrace();
	    }
	    */
		try {
			fileHandler = new FileHandler(CPAMain.cpaConfig.getProperty("log.path"), true);
			Formatter formatter = new SimpleFormatter();
			fileHandler.setFormatter(formatter);
			mainLogger.addHandler(fileHandler);
			// TODO read from config file 
			//CPAMain.cpaConfig.getProperty("log.level");
			//mainLogger.setLevel(Level.OFF);
			Level cfg = ConfigLevel.create(
			        CPAMain.cpaConfig.getProperty("log.level"));
			mainLogger.setLevel(cfg);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void log(Level level, String msg){
		mainLogger.log(level, msg);
	}
	
	public static int getLevel() {
	    return mainLogger.getLevel().intValue();
	}
	public static void clear()
	{
		mainLogger.removeHandler(fileHandler);
		if(fileHandler != null)
			fileHandler.close();
	}
	
}
