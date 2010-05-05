package org.sosy_lab.cpachecker.plugin.eclipse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.eclipse.cdt.core.model.ITranslationUnit;
import org.sosy_lab.common.configuration.Configuration;


public class TaskRunner {

	public void run(List<Task> tasks) {
		String str = "TaskRunner run Tasks:";
		for (Task t : tasks) {
			str = str + " " + t.getName();
		}
		System.out.println(str);
		// use:
		/*CPAcheckerPlugin.getPlugin().fireTasksStarted(10);
		CPAcheckerPlugin.getPlugin().fireTaskStarted("someid");
		CPAcheckerPlugin.getPlugin().fireTaskFailed("someid");
		CPAcheckerPlugin.getPlugin().fireTasksFinished();
		*/
	}
	
	public static class Task {
		String name;
		Configuration config;
		public ITranslationUnit sourceTranslationUnit;
		private String configFilePath;

		public Task(String name, Configuration config,
				ITranslationUnit selected) {
			super();
			this.name = name;
			this.config = config;
			sourceTranslationUnit = selected;
		}
		public Configuration getConfig() {
			return config;
		}
		public void setConfig(Configuration config) {
			this.config = config;
		}
		public void setConfigFilePath(String configFilePath) {
			this.configFilePath = configFilePath;
		}
		public String getName() {
			return name;
		}
		public String getConfigFilePath() {
			return configFilePath;
		}
		public void reloadConfigFile() {
			try {
				this.config = new Configuration(configFilePath, Collections.<String,String>emptyMap());
			} catch (IOException e) {
				this.config = null;
				// TODO: issue log Message
			}
		}
		/**
		 * PreRunError == an error that does not allow the task to be run.
		 * @return
		 */
		public boolean hasPreRunError() {
			return this.config == null;
		}
		public String getErrorMessage() {
			if (this.config == null) {
				return "Could not parse the configuration file \"" + this.configFilePath +" \".";
			} else {
				return "";
			}
		}
		public Object getTranslationUnit() {
			return this.sourceTranslationUnit;
		}
		
		
		
	}
}
