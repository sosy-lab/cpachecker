package org.sosy_lab.cpachecker.plugin.eclipse;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.StreamHandler;

import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.LogManager.ConsoleLogFormatter;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.plugin.eclipse.preferences.PreferenceConstants;

public class TaskRunner {

	public static void run(final List<Task> tasks) {
		CPAcheckerPlugin.getPlugin().fireTasksStarted(tasks.size());

		CPAcheckerPlugin.getPlugin().addTestListener(
				new ITestListener.DefaultImplementation() {
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
			CPAcheckerPlugin.getPlugin()
					.firePreRunError(t, t.getErrorMessage());
		} else {
			MessageConsole con = createMessageConsole("CPAchecker : "
					+ t.getName());
			Configuration config;
			try {
				config = t.createConfig();
				MessageConsoleStream outStream = con.newMessageStream();
				LogManager logger = new LogManager(config, new StreamHandler(
						outStream, new ConsoleLogFormatter()));
				Thread run = new Thread(new TaskRun(t.getTranslationUnit(),
						config, outStream, logger, t));
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
				CPAcheckerPlugin.getPlugin().firePreRunError(t,
						t.getErrorMessage());
			} else {
				MessageConsole con = createMessageConsole("CPAchecker : "
						+ t.getName());
				Configuration config;
				try {
					config = t.createConfig();
					MessageConsoleStream outStream = con.newMessageStream();
					LogManager logger = new LogManager(config,
							new StreamHandler(outStream,
									new ConsoleLogFormatter()));
					Thread run = new Thread(new TaskRun(t.getTranslationUnit(),
							config, outStream, logger, t));
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
		// CPAcheckerPlugin.getPlugin().fireTasksFinished();
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
		private MessageConsoleStream consoleStream;
		private LogManager logger;
		private Task task;

		TaskRun(ITranslationUnit source, Configuration config,
				MessageConsoleStream outStream, LogManager logger, Task t) {
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
				if (CPAcheckerPlugin.getPlugin().getPreferenceStore().getBoolean(PreferenceConstants.P_STATS)) {
					results.printStatistics(new PrintWriter(consoleStream));					
				} else {
					switch (results.getResult()) {
					case SAFE:
						//color: green, doesnt work, threading issues
						//consoleStream.setColor(new Color(CPAcheckerPlugin.getPlugin().getWorkbench().getDisplay(), 0, 255,0));
						consoleStream.println("\nCPA run was safe. No Error locations found.");
						break;
					case UNKNOWN:
						//color: blue, doesnt work, threading issues
						//consoleStream.setColor(new Color(CPAcheckerPlugin.getPlugin().getWorkbench().getDisplay(), 0, 0,255));
						consoleStream.println("\nThe CPA run could not be terminated correctly. The result is unknown.");
						break;
					case UNSAFE:
						// color: red, doesnt work, threading issues
						//consoleStream.setColor(new Color(CPAcheckerPlugin.getPlugin().getWorkbench().getDisplay(), 255, 0,0));
						consoleStream.println("\nCPA found a reachable error location. The program is UNSAFE!");
						break;
					}
				}
				consoleStream.close();
				task.setLastResult(results.getResult());
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
		//private Configuration config = null;
		private boolean isDirty = true;
		private Result lastResult = Result.UNKNOWN;

		/**
		 * One of the parameters configFile and source may be null.
		 * 
		 * @param taskName
		 * @param configFile
		 * @param source
		 */
		public Task(String taskName, IFile configFile, ITranslationUnit source) {
			this.name = createUniqueName(taskName);
			this.configFile = configFile;
			this.sourceTranslationUnit = source;
		}

		public void setLastResult(Result result) {
			this.lastResult = result;
		}

		public Result getLastResult() {
			return this.lastResult;
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

		
		public Configuration createConfig() throws IOException, CoreException {
				Configuration config = new Configuration(configFile.getContents(),
						Collections.<String, String> emptyMap());
				String projectRoot = this.configFile.getProject().getLocation()
						.toPortableString() + "/";
				// config.setProperty("automatonAnalyis.rootPath", projectRoot);
				String property = config.getProperty("automatonAnalysis.inputFile");
				// TODO: handle multiple automaton files
				if (property != null) {
					File file = new File(property);
					if (!file.isAbsolute()) {
						config.setProperty("automatonAnalysis.inputFile",
								projectRoot + property);
					}
				}
				config.setProperty("output.path",  
						this.getOutputDirectory().getAbsolutePath());
				/*property = config.getProperty("output.path");
				if (property != null) {
					File file = new File(property);
					if (!file.isAbsolute()) {
						//config.setProperty("output.path", projectRoot + property);
					}
				} else {
					config.setProperty("output.path", projectRoot);
				}*/
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
		 * 
		 * @return
		 */
		public boolean hasPreRunError() {
			try {
				if (configFile == null || createConfig() == null) {
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
		}

		public void setTranslationUnit(ITranslationUnit tu) {
			this.sourceTranslationUnit = tu;
		}
		
		/**Returns the File (a Directory) where the results of this Task should be saved.
		 * @return
		 */
		public File getOutputDirectory() {
			/*IPreferencesService service = Platform.getPreferencesService();
			String value = service.getString(CPAcheckerPlugin.PLUGIN_ID,
					PreferenceConstants.P_RESULT_DIR,
					PreferenceConstants.P_RESULT_DIR_DEFAULT_VALUE, null);
			IFolder outDir = ResourcesPlugin.getWorkspace().getRoot().getFolder(new Path(value));
			assert outDir.exists() : "OutputDirectory of CPAchecker does not exist! Could not create Task Output dir.";
			outDir = outDir.getFolder(this.getName());
			if (create) {
				try {
					outDir.create(true, true, null);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
			// TODO: as this will be a directory we should escape characters that can not occur in directory paths
			
			return outDir;
			*/
			File file = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toPortableString() + "/.metadata/.plugins/" 
					+ CPAcheckerPlugin.PLUGIN_ID + "/results/" + this.getName() + "/");
			file.mkdirs();
			return file;
		}
		
	}
}
