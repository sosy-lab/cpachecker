package cpaplugin.logging;

import java.util.List;
import java.util.logging.Level;

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