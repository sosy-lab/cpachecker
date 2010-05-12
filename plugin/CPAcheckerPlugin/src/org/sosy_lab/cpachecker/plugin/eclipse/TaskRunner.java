package org.sosy_lab.cpachecker.plugin.eclipse;

import java.io.BufferedOutputStream;
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
				CPAcheckerPlugin.getPlugin().fireTaskFinished(t, false);
			} else {
				CPAcheckerResult results = execute(t);
				boolean success = results.getResult().equals(CPAcheckerResult.Result.SAFE);
				CPAcheckerPlugin.getPlugin().fireTaskFinished(t, success);
			}
		}
		CPAcheckerPlugin.getPlugin().fireTasksFinished();
	}
	
	void activateConsole() {
		this.findConsole("CPACHECKER").activate();
	}
	
	private CPAcheckerResult execute(final Task task) {
		
		Configuration config = null;
		try {
			config = task.getConfig();
		} catch (IOException e1) { // exceptions will be caught in the next statement
		} catch (CoreException e1) {
		}
		if (config == null) {
			// TODO shouldn't an Eclipse Dialog be used here?
			JOptionPane.showMessageDialog(null, "Configuration could not be created"
					, "unable to perform task",
					JOptionPane.ERROR_MESSAGE);
			return null;
		}

		// Lets set up a console to write to
		
		MessageConsole myConsole = findConsole("CPACHECKER");

		IOConsoleOutputStream outStream = myConsole.newOutputStream();
		IOConsoleOutputStream errStream = myConsole.newOutputStream();
		errStream.setColor(new org.eclipse.swt.graphics.Color(Display.getDefault(), 255, 0, 0));
		System.setOut(new PrintStream(outStream));
		System.setErr(new PrintStream(errStream));
		LogManager logManager;
		try {
			logManager = new LogManager(config, new StreamHandler(outStream, new ConsoleLogFormatter()));
		} catch (InvalidConfigurationException e) {
			JOptionPane.showMessageDialog(null, "Invalid configuration: "
					+ e.getMessage(), "Invalid configuration",
					JOptionPane.ERROR_MESSAGE);
			return null;
		} finally {
			closeStreams(outStream, errStream);
		}
		logManager.log(Level.INFO, "Running Task " + task.getName() + " on Program " + task.getTranslationUnit().getLocation());
		logManager.log(Level.INFO, "Program Started");
		try {
			// Now grab its attention and display
			String id = IConsoleConstants.ID_CONSOLE_VIEW;
			
			IWorkbenchPage workbenchPage = CPAcheckerPlugin.getPlugin().getWorkbench().getActiveWorkbenchWindow().getActivePage();
			IConsoleView view;
			
			view = (IConsoleView) workbenchPage.showView(id);
			
			view.display(myConsole);
			// Now run analysis
			final CPAchecker cpachecker = new CPAchecker(config, logManager);
			// TODO: insert CIL here somewhere
			CPAcheckerResult result = cpachecker.run(task.getTranslationUnit().getLocation().toOSString());					
			
			OutputStream outStream2 = myConsole.newOutputStream();
			result.printStatistics(new PrintWriter(outStream2));
			try {
				outStream2.flush();
				outStream2.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return result;
		} catch (PartInitException e) {
			logManager.log(Level.WARNING, e);
			JOptionPane.showMessageDialog(null, "Exception during console initialization: "
					+ e.getMessage(), "Exception during console initialization",
					JOptionPane.ERROR_MESSAGE);
		} catch (InvalidConfigurationException e) {
			logManager.log(Level.WARNING, e);
			JOptionPane.showMessageDialog(null, "Invalid configuration: "
					+ e.getMessage(), "Invalid configuration",
					JOptionPane.ERROR_MESSAGE);
		} finally {
			closeStreams(outStream, errStream);
		}
		closeStreams(outStream, errStream);
		return null;
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
		// no console found, so create a new one
		MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { myConsole });
		return myConsole;
	}
	
	public static class Task {
		private String name;
		private ITranslationUnit sourceTranslationUnit;
		private IFile configFile;
		private Configuration config;

		public Task(String taskName, IFile configFile, ITranslationUnit source) {
			this.name = taskName;
			this.configFile = configFile;
			sourceTranslationUnit = source;
		}
		
		public Task(String string, ITranslationUnit selected) {
			this.name = string;
			this.sourceTranslationUnit = selected;
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
				return getConfig() == null;
			} catch (IOException e) {
				return true;
			} catch (CoreException e) {
				return true;
			}
		}
		public String getErrorMessage() {
			if (this.config == null) {
				return "Could not parse the configuration file \"" + this.configFile.getProjectRelativePath() +" \".";
			} else {
				return "";
			}
		}
		public ITranslationUnit getTranslationUnit() {
			return this.sourceTranslationUnit;
		}
		
		
		
	}
}
