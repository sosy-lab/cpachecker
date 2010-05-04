package org.sosy_lab.cpachecker.plugin.eclipse;

import java.util.List;
import org.sosy_lab.common.configuration.Configuration;


public class TaskRunner {

	public void run(List<Task> tasks) {
		// TODO Auto-generated method stub
		System.out.println("TaskRunner : run some tests");
		// use:
		/*CPAcheckerPlugin.getPlugin().fireTasksStarted(10);
		CPAcheckerPlugin.getPlugin().fireTaskStarted("someid");
		CPAcheckerPlugin.getPlugin().fireTaskFailed("someid");
		CPAcheckerPlugin.getPlugin().fireTasksFinished();
		*/
	}
	public interface ITask {
		public String getName();
		public Configuration getConfig();
		public String getSourceFileName();
	}
	
	public static class Task implements ITask {
		String name;
		Configuration config;
		String sourceFileName = "noSourceFileSpecified";
		private String configFile;
		public Task(String name, Configuration config, String sourceFileName) {
			super();
			this.name = name;
			this.config = config;
			this.sourceFileName = sourceFileName;
		}
		public Task(String name, String configFile, String sourceFileName) {
			super();
			this.name = name;
			this.configFile = configFile;
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
			return configFile;
		}
		
	}
}
