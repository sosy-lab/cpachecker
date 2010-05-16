package org.sosy_lab.cpachecker.plugin.eclipse;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.StreamHandler;

import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.LogManager.ConsoleLogFormatter;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;


public class TaskRunner {

	public static void run(final List<Task> tasks) {
		CPAcheckerPlugin.getPlugin().fireTasksStarted(tasks.size());
		
		CPAcheckerPlugin.getPlugin().addTestListener(new ITestListener.DefaultImplementation() {
			int currentTaskIndex = 1;
			@Override
			public void taskHasPreRunError(Task t, String errorMessage) {
				if (currentTaskIndex >= tasks.size()) {
					CPAcheckerPlugin.getPlugin().fireTasksFinished();
				} else {
					startNext();
				}
			}
			@Override
			public void taskFinished(Task id, CPAcheckerResult results) {
				if (currentTaskIndex >= tasks.size()) {
					CPAcheckerPlugin.getPlugin().fireTasksFinished();
				} else {
					startNext();
				}
			}
			private void startNext() {
				startSingleTask(tasks.get(currentTaskIndex++));
			}
		});
		startSingleTask(tasks.get(0));
	}
	public static void startSingleTask(Task t) {
		CPAcheckerPlugin.getPlugin().fireTaskStarted(t);
		if (t.hasPreRunError()) {
			CPAcheckerPlugin.getPlugin().firePreRunError(t, t.getErrorMessage());
		} else {
			MessageConsole con = createMessageConsole("CPAchecker : " + t.getName());
			Configuration config;
			try {
				config = t.loadConfig();
				OutputStream outStream = con.newMessageStream();
				LogManager logger = new LogManager(config, new StreamHandler(outStream , new ConsoleLogFormatter()));		
				Thread run = new Thread(new TaskRun(t.getTranslationUnit(), config, outStream, logger, t));
				run.start();
			} catch (IOException e) {
				// cannot happen because this would have been a preRunError
				assert false;
			} catch (CoreException e) {
				// cannot happen because this would have been a preRunError
				assert false;
			} catch (InvalidConfigurationException e) {
				// cannot happen because this would have been a preRunError
				assert false;
			}
		}
	}
	
	public static void runParallel(List<Task> tasks) {
		CPAcheckerPlugin.getPlugin().fireTasksStarted(tasks.size());
		for (Task t : tasks) {
			CPAcheckerPlugin.getPlugin().fireTaskStarted(t);
			if (t.hasPreRunError()) {
				CPAcheckerPlugin.getPlugin().firePreRunError(t, t.getErrorMessage());
			} else {
				MessageConsole con = createMessageConsole("CPAchecker : " + t.getName());
				Configuration config;
				try {
					config = t.loadConfig();
					OutputStream outStream = con.newMessageStream();
					LogManager logger = new LogManager(config, new StreamHandler(outStream , new ConsoleLogFormatter()));		
					Thread run = new Thread(new TaskRun(t.getTranslationUnit(), config, outStream, logger, t));
					run.start();
				} catch (IOException e) {
					// cannot happen because this would have been a preRunError
					assert false;
				} catch (CoreException e) {
					// cannot happen because this would have been a preRunError
					assert false;
				} catch (InvalidConfigurationException e) {
					// cannot happen because this would have been a preRunError
					assert false;
				}
			}
		}
		// wont work
		//CPAcheckerPlugin.getPlugin().fireTasksFinished();
	}
	
	private static MessageConsole createMessageConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { myConsole });
		myConsole.activate();
		return myConsole;
	}
	
	private static class TaskRun implements Runnable {
		private ITranslationUnit source;
		private Configuration config;
		private OutputStream consoleStream;
		private LogManager logger;
		private Task task;
		
		TaskRun(ITranslationUnit source, Configuration config,
				OutputStream outStream, LogManager logger, Task t) {
			super();
			this.task = t;
			this.source = source;
			this.config = config;
			this.consoleStream = outStream;
			this.logger = logger;
		}
		
		@Override
		public void run() {
			try {
				CPAchecker cpachecker = new CPAchecker(config, logger);
				final CPAcheckerResult results = cpachecker.run(source.getLocation().toOSString());
				logger.flush();
				results.printStatistics(new PrintWriter(consoleStream));
				consoleStream.close();
				// finshedAnnouncement must be fired in Eclipse UI thread
				CPAcheckerPlugin.getPlugin().getWorkbench().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						CPAcheckerPlugin.getPlugin().fireTaskFinished(task, results);	
					}
				});
			} catch (Exception e) {
				System.err.println("Task \"" + task.getName() + "\" run has thrown an exception:");
				e.printStackTrace();
			}			
		}
	}
	
	public static class Task {
		private String name;
		private ITranslationUnit sourceTranslationUnit = null;
		private IFile configFile = null;
		private Configuration config = null;
		private boolean isDirty = true;

		/**
		 * One of the parameters configFile and source may be null.
		 * @param taskName
		 * @param configFile
		 * @param source
		 */
		public Task(String taskName, IFile configFile, ITranslationUnit source) {
			this.name = createUniqueName(taskName);
			this.configFile = configFile;
			this.sourceTranslationUnit = source;
		}
		
		public Task(String taskName, IFile configFile) {
			this.name = createUniqueName(taskName);
			this.configFile = configFile;
		}
		
		public Task(String taskName, ITranslationUnit source) {
			this.name = createUniqueName(taskName);
			this.sourceTranslationUnit = source;
		}
		
		private static String createUniqueName(String preferredName) {
			List<Task> tasks = CPAcheckerPlugin.getPlugin().getTasks();
			Set<String> takenNames = new HashSet<String>();
			for (Task t : tasks) {
				takenNames.add(t.getName());
			}
			if (takenNames.contains(preferredName)) {
				int i = 1;
				while (true) {
					if (takenNames.contains(preferredName + " (" + i + ")")) {
						i++;
					} else {
						return preferredName + " (" + i + ")";
					}
				}				
			} else {
				return preferredName;
			}
			
		}
		/**
		 * Assumes that configfile is not null
		 * @return
		 * @throws IOException
		 * @throws CoreException
		 */
		public Configuration loadConfig() throws IOException, CoreException {
			if (config == null) {
				config = new Configuration(configFile.getContents(), Collections.<String,String>emptyMap());
				String projectRoot = this.configFile.getProject().getLocation().toPortableString();
				//config.setProperty("automatonAnalyis.rootPath", projectRoot);
				String property = config.getProperty("automatonAnalysis.inputFile");
				if (property != null) {
					File file = new File(property);
					if (!file.isAbsolute()) {
						config.setProperty("automatonAnalysis.inputFile", projectRoot + property);
					}
				}
				property = config.getProperty("output.path");
				if (property != null) {
					File file = new File(property);
					if (!file.isAbsolute()) {
						config.setProperty("output.path", projectRoot + property);
					}
				} else {
					config.setProperty("output.path", projectRoot);
				}
			}
			return config;
		}
		
		public String getName() {
			return name;
		}
		public IFile getConfigFile() {
			return this.configFile;
		}
		public String getConfigFilePathProjRelative() {
			return configFile.getProjectRelativePath().toPortableString();
		}
		
		/**
		 * PreRunError == an error that does not allow the task to be run.
		 * @return
		 */
		public boolean hasPreRunError() {
			try {
				if (configFile == null || loadConfig() == null) {
					return true;
				} else if (this.getTranslationUnit() == null) {
					return true;
				}
			} catch (IOException e) {
				return true;
			} catch (CoreException e) {
				return true;
			}
			return false;
		}
		
		public String getErrorMessage() {
			if (this.configFile == null) {
				return "No configuration file was associated with this task!";
			} else if (this.config == null) {
				return "Could not parse the configuration file \"" + this.configFile.getProjectRelativePath() +" \".";
			} else if (this.getTranslationUnit() == null) {
				return "No Source file was associated with this Task!";
			} else {
				return "";
			}
		}
		
		public ITranslationUnit getTranslationUnit() {
			return this.sourceTranslationUnit;
		}

		public boolean hasConfigurationFile() {
			return this.configFile != null;
		}

		public void setName(String newName) {
			this.name = createUniqueName(newName);
		}

		public boolean isDirty() {
			return this.isDirty;
		}
		public void setDirty(boolean b) {
			this.isDirty = b;
		}

		public void setConfigFile(IFile member) {
			this.configFile = member;
			this.configFile = null;
		}

		public void setTranslationUnit(ITranslationUnit tu) {
			this.sourceTranslationUnit = tu;
		}
	}
}
