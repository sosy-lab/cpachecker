package org.sosy_lab.cpachecker.plugin.eclipse;

import java.util.List;
import org.sosy_lab.common.configuration.Configuration;


public class TaskRunner {

	public void run(List<Task> tasks) {
		// TODO Auto-generated method stub
		
		// use:
		CPAcheckerPlugin.getPlugin().fireTestsStarted(10);
		CPAcheckerPlugin.getPlugin().fireTestStarted("someid");
		CPAcheckerPlugin.getPlugin().fireTestFailed("someid");
		CPAcheckerPlugin.getPlugin().fireTestsFinished();
	}
	public interface ITask {
		public String getName();
		public Configuration getConfig();
		public String getSourceFileName();
	}
	
	public static class Task implements ITask {
		String name;
		Configuration config;
		String sourceFileName;
		public Task(String name, Configuration config, String sourceFileName) {
			super();
			this.name = name;
			this.config = config;
			this.sourceFileName = sourceFileName;
		}
		@Override
		public Configuration getConfig() {
			return config;
		}
		public void setConfig(Configuration config) {
			this.config = config;
		}
		@Override
		public String getSourceFileName() {
			return sourceFileName;
		}
		public void setSourceFileName(String sourceFileName) {
			this.sourceFileName = sourceFileName;
		}
		@Override
		public String getName() {
			return name;
		}
		public String getConfigName() {
			return "unknown config file name"; // TODO: implement
		}
		
	}
}
