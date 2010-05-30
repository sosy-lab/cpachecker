package org.sosy_lab.cpachecker.plugin.eclipse;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.StreamHandler;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.progress.IProgressService;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.LogManager.ConsoleLogFormatter;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.plugin.eclipse.preferences.PreferenceConstants;


public class TaskRunner {

	public static class CloseCurrentConsoleAction extends Action {
		static final String ID = "org.sosy_lab.cpachecker.plugin.eclipse.TaskView.CloseConsoleAction";
		private IConsoleView consoleView;
		public CloseCurrentConsoleAction(IConsoleView view) {
			this.consoleView = view;
		}
		@Override
		public String getId() {
			return ID;
		}
		@Override
		public ImageDescriptor getImageDescriptor() {
			return CPAcheckerPlugin.getImageDescriptor("icons/rem_co.gif");
		}
		
		@Override
		public String getText() {
			return "close Console";
		}
		@Override
		public void run() {
			ConsolePlugin.getDefault().getConsoleManager().removeConsoles(new IConsole[] {consoleView.getConsole()});
			super.run();
		}
	}

	public static void run(final List<Task> tasks) {
		//CPAcheckerPlugin.getPlugin().getWorkbench().getProgressService()
		CPAcheckerPlugin.getPlugin().fireTasksStarted(tasks.size());
		IProgressService service = CPAcheckerPlugin.getPlugin().getWorkbench().getProgressService();
		for (Task t : tasks) {
			try {
				service.run(false, true, new TaskRun(t));
			} catch (InvocationTargetException e) {
				CPAcheckerPlugin.logError(e);
			} catch (InterruptedException e) {
				CPAcheckerPlugin.logError(e);
			}
		}
		CPAcheckerPlugin.getPlugin().fireTasksFinished();
		CPAcheckerPlugin.getPlugin().fireTasksChanged(tasks);
		
	}

	/*public static void startSingleTask(Task t) {
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
	}*/
/*
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
	}*/

	private static class TaskRun implements IRunnableWithProgress {
		private MessageConsoleStream consoleStream;
		private LogManager logger;
		private Task task;

		TaskRun(Task t) {
			super();
			this.task = t;
			// Sync Exec !
			CPAcheckerPlugin.getPlugin().getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					ConsolePlugin plugin = ConsolePlugin.getDefault();
					final IConsoleManager conMan = plugin.getConsoleManager();
					final MessageConsole console = new MessageConsole(task.getName(), null) {
						@Override
						public IPageBookViewPage createPage(IConsoleView view) {
							IToolBarManager toolBarManager = view.getViewSite().getActionBars().getToolBarManager();
							if (toolBarManager.find(CloseCurrentConsoleAction.ID) == null) {
								IContributionItem firstItem = toolBarManager.getItems()[0];
								if (firstItem == null) {
									toolBarManager.add(new CloseCurrentConsoleAction(view));
								} else {
									toolBarManager.insertBefore(firstItem.getId(), new CloseCurrentConsoleAction(view));
								}
								
							}
							return super.createPage(view);							
						}
						
					};
					conMan.addConsoles(new IConsole[] { console });
					//myConsole.activate();
					consoleStream = console.newMessageStream();
				}
			});
		}
		
		@Override
		public void run(IProgressMonitor monitor) {
			fireTaskStarted();
			try {
				task.getOutputDirectory(false).delete(false, monitor);
			} catch (CoreException e2) {
				CPAcheckerPlugin.logError("OCould not delete output directory prior to running task " + task.getName(), e2);
			}
			
			monitor.subTask("Running: " + task.getName());
			if (monitor.isCanceled()) {
				fireTaskFinished(null);
				return;
			} else if (task.hasPreRunError()) {
				firePreRunError();
				return;
			} else {
				try {
					Configuration config = task.createConfig();
					logger = new LogManager(config, new StreamHandler(
							consoleStream, new ConsoleLogFormatter()));
					CPAchecker cpachecker = new CPAchecker(config, logger);
					final CPAcheckerResult results = cpachecker.run(task.getTranslationUnit().getLocation().toOSString());
					logger.flush();
					if (CPAcheckerPlugin.getPlugin().getPreferenceStore().getBoolean(PreferenceConstants.P_STATS)) {
						results.printStatistics(new PrintWriter(consoleStream, true));					
					} else {
						// cannot avoid this, because i have to generate the (log)- files
						results.printStatistics(new PrintWriter(consoleStream, true));
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
					fireTaskFinished(results);
				} catch (Exception e) {
					if (consoleStream!= null) {
						consoleStream.println("Evaluation of Task "+ task.getName() + " has thrown an exception");
						e.printStackTrace(new PrintStream(consoleStream));
						try {
							consoleStream.close();
						} catch (IOException e1) {
							CPAcheckerPlugin.logError("OutputStream of the console could not be closed", e);
						}
					}
					CPAcheckerPlugin.logError("Evaluation of Task "+ task.getName() + " has thrown an exception", e);
				} finally {
					fireTaskFinished(null);
					monitor.worked(1);
				}
			}
		}
		private void firePreRunError() {
			CPAcheckerPlugin.getPlugin().getWorkbench().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					CPAcheckerPlugin.getPlugin().firePreRunError(task, task.getErrorMessage());	
				}
			});
		}

		void fireTaskStarted() {
			CPAcheckerPlugin.getPlugin().getWorkbench().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					CPAcheckerPlugin.getPlugin().fireTaskStarted(task);	
				}
			});
		}
		void fireTaskFinished(final CPAcheckerResult results) {
			CPAcheckerPlugin.getPlugin().getWorkbench().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					CPAcheckerPlugin.getPlugin().fireTaskFinished(task, results);	
				}
			});
		}
	}
}
