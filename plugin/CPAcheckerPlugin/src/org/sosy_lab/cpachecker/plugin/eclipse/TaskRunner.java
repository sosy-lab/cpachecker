package org.sosy_lab.cpachecker.plugin.eclipse;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.StreamHandler;

import javax.swing.JOptionPane;

import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.LogManager.ConsoleLogFormatter;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;


public class TaskRunner {

	public void run(List<Task> tasks) {
		CPAcheckerPlugin.getPlugin().fireTasksStarted(tasks.size());
		
		for (Task t : tasks) {
			CPAcheckerPlugin.getPlugin().fireTaskStarted(t);
			if (t.hasPreRunError()) {
				CPAcheckerPlugin.getPlugin().firePreRunError(t, t.getErrorMessage());
			} else {
				
				MessageConsole con = createMessageConsole("CPAchecker : " + t.getName());
				
				Configuration config;
				try {
					config = t.getConfig();
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
		CPAcheckerPlugin.getPlugin().fireTasksFinished();
	}
	
	private MessageConsole createMessageConsole(String name) {
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
				CPAcheckerResult results = cpachecker.run(source.getLocation().toOSString());
				CPAcheckerPlugin.getPlugin().fireTaskFinished(task, results);
				logger.flush();
				results.printStatistics(new PrintWriter(consoleStream));
				consoleStream.close();
			} catch (Exception e) {
				System.err.println("Task \"" + task.getName() + "\" run has thrown an exception:");
				e.printStackTrace();
			}
			
		}
	}
	
	
	
	private void closeStreams(OutputStream outStream, OutputStream errStream) {
		try {
			if (outStream!= null) {
				outStream.flush();	
				outStream.close();
			}
			if (errStream != null) {
				errStream.flush();
				errStream.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private MessageConsole findConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++)
			if (name.equals(existing[i].getName()))
				return (MessageConsole) existing[i];
		// no consoleStream found, so create a new one
		MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { myConsole });
		return myConsole;
	}
	
	public static class Task {
		private String name;
		private ITranslationUnit sourceTranslationUnit = null;
		private IFile configFile = null;
		private Configuration config = null;

		public Task(String taskName, IFile configFile, ITranslationUnit source) {
			this.name = taskName;
			this.configFile = configFile;
			sourceTranslationUnit = source;
		}
		
		public Task(String taskName, IFile configFile) {
			this.name = taskName;
			this.configFile = configFile;
		}
		
		public Task(String taskName, ITranslationUnit source) {
			this.name = taskName;
			this.sourceTranslationUnit = source;
		}
		
		public Configuration getConfig() throws IOException, CoreException {
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
		
		public String getConfigFilePath() {
			return configFile.getProjectRelativePath().toPortableString();
		}
		
		/**
		 * PreRunError == an error that does not allow the task to be run.
		 * @return
		 */
		public boolean hasPreRunError() {
			try {
				if (getConfig() == null) {
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
			if (this.config == null) {
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
	}
}
