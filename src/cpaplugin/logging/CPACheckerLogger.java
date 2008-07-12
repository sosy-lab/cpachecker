package cpaplugin.logging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import cpaplugin.CPAConfig;

public class CPACheckerLogger {
	
	public static Logger mainLogger = Logger.getLogger(CPAConfig.LogPath);

	public static void init(){
		// TODO read from config // Array that includes levels to include
		ArrayList<String> levelList = new ArrayList<String>();
		levelList.add("MainApplicationLevel");
		levelList.add("CentralCPAAlgorithmLevel");
		levelList.add("SpecificCPALevel");
		levelList.add("ExternalToolLevel");
		CustomLogLevel.initializeLevels(levelList);
		
		try {
			FileHandler fileHandler = new FileHandler("/home/erkan/CPAlog.txt");
			Formatter formatter = new SimpleFormatter();
			fileHandler.setFormatter(formatter);
			mainLogger.addHandler(fileHandler);
			mainLogger.setLevel(Level.FINE);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void log(Level level, String msg){
		mainLogger.log(level, msg);
	}
	
}
