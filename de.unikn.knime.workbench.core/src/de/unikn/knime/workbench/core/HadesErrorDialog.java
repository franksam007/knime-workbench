/* @(#)$$RCSfile$$ 
 * $$Revision$$ $$Date$$ $$Author$$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   ${date} (${user}): created
 */
package de.unikn.knime.workbench.core;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * JFace dialog presenting an (internal) error to the user.
 * 
 * TODO add a "details" section
 * 
 * @author Florian Georg, University of Konstanz
 */
public class HadesErrorDialog extends ErrorDialog {

    /**
     * Main constructor, takes all possible arguments.
     * 
     * @param parentShell The parent shell
     * @param dialogTitle Title to display
     * @param text The message text
     * @param status Statuscode, like <code>IStatus.ERROR</code>
     * @param displayMask mask for filtering children
     */
    public HadesErrorDialog(final Shell parentShell, final String dialogTitle,
            final String text, final IStatus status, final int displayMask) {
        super(parentShell, dialogTitle, text, status, displayMask);
    }

    /**
     * Opens an error dialog.
     * 
     * @param parentShell The Shell
     * @param title The title
     * @param message the message
     * @param status the status code
     * @param displayMask display mask for filtering
     * 
     * @return <code>Window.OK</code> or <code>Window.CANCEL</code>
     */
    public static int openError(final Shell parentShell, final String title,
            final String message, final IStatus status, final int displayMask) {
        HadesErrorDialog dialog = new HadesErrorDialog(parentShell, title,
                message, status, displayMask);
        return dialog.open();
    }

    /**
     * Opens an error dialog with standard filtering (displayMask).
     * 
     * @param parentShell The Shell
     * @param title The title
     * @param message the message
     * @param status the status code
     * 
     * @return <code>Window.OK</code> or <code>Window.CANCEL</code>
     */
    public static int openError(final Shell parentShell, final String title,
            final String message, final IStatus status) {
        return HadesErrorDialog.openError(parentShell, title, message, status,
                IStatus.OK | IStatus.INFO | IStatus.WARNING | IStatus.ERROR);
    }

    /**
     * Opens an error dialog with standard filtering (displayMask) and standard
     * title on the active shell on the current display.
     * 
     * @param message the message
     * @param status the status code
     * 
     * @return <code>Window.OK</code> or <code>Window.CANCEL</code>
     */
    public static int openError(final String message, final IStatus status) {
        return HadesErrorDialog.openError(
                Display.getCurrent().getActiveShell(),
                "Hades Workbench: Error occured", message, status);
    }
}
