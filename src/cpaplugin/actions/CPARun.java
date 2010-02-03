/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpaplugin.actions;

import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;

import cmdline.CPAMain;
import cpa.common.CPAConfiguration;
import cpa.common.LogManager;

public class CPARun implements IWorkbenchWindowActionDelegate
{
	private boolean init = false;
	private int numberOfRuns = 0;
	private IWorkbenchWindow window;

	/**
	 * The constructor.
	 */
	public CPARun ()
	{

	}
	private void init()
	{
		if(init == false)
		{
			MessageConsole myConsole = findConsole("CPACHECKER");
			IOConsoleOutputStream outStream = myConsole.newOutputStream();
			IOConsoleOutputStream errStream = myConsole.newOutputStream();
			errStream.setColor(new org.eclipse.swt.graphics.Color(Display.getDefault(), 255,0,0));
			System.setOut(new PrintStream(outStream));
			System.setErr(new PrintStream(errStream));
			init = true;
		}
		return;
	}
	private MessageConsole findConsole(String name) {
	      ConsolePlugin plugin = ConsolePlugin.getDefault();
	      IConsoleManager conMan = plugin.getConsoleManager();
	      IConsole[] existing = conMan.getConsoles();
	      for (int i = 0; i < existing.length; i++)
	         if (name.equals(existing[i].getName()))
	            return (MessageConsole) existing[i];
	      //no console found, so create a new one
	      MessageConsole myConsole = new MessageConsole(name, null);
	      conMan.addConsoles(new IConsole[]{myConsole});
	      return myConsole;
	   }
	/**
	 * The action has been activated. The argument of the method represents the
	 * 'real' action sitting in the workbench UI.
	 *
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run (IAction action)
	{
		numberOfRuns++;
    try {
      String configFile = cpaplugin.PreferencesActivator.getDefault().getPreferenceStore().getString(cpaplugin.preferences.PreferenceConstants.P_PATH);
      CPAMain.cpaConfig = new CPAConfiguration(configFile);
    } catch (IOException e) {
      // TODO shouldn't an Eclipse Dialog be used here?
      JOptionPane.showMessageDialog(null, "Could not read config file " + e.getMessage(), "Could not read config file", JOptionPane.ERROR_MESSAGE);
      return;
    }
    CPAMain.logManager = LogManager.getInstance();
    
    //Lets set up a console to write to
    init();
		MessageConsole myConsole = findConsole("CPACHECKER");
		CPAMain.logManager.log(Level.INFO, "Run #:" + numberOfRuns);
		CPAMain.logManager.log(Level.INFO, "Program Started");

		MessageDialog.openInformation (window.getShell (), "CPAPlugin Plug-in", "Launching CPAChecker Eclipse Plugin");

		try
		{
			// Get the current document
			IWorkbench workbench = PlatformUI.getWorkbench ();
			if (workbench == null)
			{
				MessageDialog.openInformation (window.getShell (), "CPAPlugin Plug-in", "Workbench cannot be found");
				return;
			}

			IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow ();
			if (workbenchWindow == null)
			{
				MessageDialog.openInformation (window.getShell (), "CPAPlugin Plug-in", "No active workbench window");
				return;
			}

			IWorkbenchPage workbenchPage = workbenchWindow.getActivePage ();
			if (workbenchPage == null)
			{
				MessageDialog.openInformation (window.getShell (), "CPAPlugin Plug-in", "No active page in active workbench window");
				return;
			}

			IEditorPart editorPart = workbenchPage.getActiveEditor ();
			if (editorPart == null)
			{
				MessageDialog.openInformation (window.getShell (), "CPAPlugin Plug-in", "No active editor in the active workbench");
				return;
			}

			IEditorInput editorInput = editorPart.getEditorInput ();
			if (editorInput == null)
			{
				MessageDialog.openInformation (window.getShell (), "CPAPlugin Plug-in", "No active editor input in active editor part");
				return;
			}

			IFile currentFile = (IFile) editorInput.getAdapter (IFile.class);
			if (currentFile == null)
			{
				MessageDialog.openInformation (window.getShell (), "CPAPlugin Plug-in", "No file associated with current editor input");
				return;
			}
			String extension = currentFile.getFileExtension();
			if(extension.compareTo("c") != 0)
			{
				MessageDialog.openInformation (window.getShell (), "CPAPlugin Plug-in", "Cannot parse non-c file");
				return;
			}
			
			//Now grab its attention and display
      String id = IConsoleConstants.ID_CONSOLE_VIEW;
      IConsoleView view = (IConsoleView) workbenchPage.showView(id);
      view.display(myConsole);
			
      //Now run analysis
      CPAMain.CPAchecker(currentFile);
		}
		catch (Exception e)
		{
      CPAMain.logManager.logException(Level.WARNING, e, "");
		}
	}

	/**
	 * Selection in the workbench has been changed. We can change the state of
	 * the 'real' action here if we want, but this can only happen after the
	 * delegate has been created.
	 *
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged (IAction action, ISelection selection)
	{
	}

	/**
	 * We can use this method to dispose of any system resources we previously
	 * allocated.
	 *
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose ()
	{
	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 *
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init (IWorkbenchWindow window)
	{
		this.window = window;
	}
}