/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
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
 *   25.05.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;

import de.unikn.knime.core.node.workflow.WorkflowManager;
import de.unikn.knime.workbench.editor2.WorkflowEditor;
import de.unikn.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Abstract base class for actions that do something with a
 * <code>NodeContainer</code> inside the <code>WorkflowEditor</code>. Note
 * that this hooks as a workflow listener as soon as the
 * <code>WorkflowManager</code> is available. This is needed, because
 * enablement of an action may change not only on selection changes but also on
 * workflow changes.
 * 
 * @author Florian Georg, University of Konstanz
 */
public abstract class AbstractNodeAction extends SelectionAction {

    private WorkflowEditor m_editor;

    /**
     * 
     * @param editor The editor that is associated with this action
     */
    public AbstractNodeAction(final WorkflowEditor editor) {
        super((IWorkbenchPart)editor);
        setLazyEnablementCalculation(true);

        m_editor = editor;

    }

    // /**
    // * Unhook workflow listener when disposed
    // *
    // * @see org.eclipse.gef.Disposable#dispose()
    // */
    // public void dispose() {
    // super.dispose();
    // if (getManager() != null) {
    // getManager().removeListener(this);
    // m_registered = false;
    // }
    // }

    /**
     * @return The manager that is edited by the current editor. Subclasses may
     *         want to have a reference to this.
     * 
     * Note that this value may be <code>null</code> if the editor has not
     * already been created completly !
     * 
     */
    protected final WorkflowManager getManager() {
        return m_editor.getWorkflowManager();

    }

    /**
     * Calls <code>runOnNodes</code> with the current selected
     * <code>NodeContainerEditPart</code>s.
     * 
     * @see org.eclipse.jface.action.IAction#run()
     */
    public final void run() {

        // get selected parts...
        final NodeContainerEditPart[] parts = getSelectedParts();

        // call implementation of this action in the SWT UI thread
        Display.getCurrent().syncExec(new Runnable() {

            public void run() {
                runOnNodes(parts);
            }

        });
    }

    /**
     * @return The selected <code>NodeContainerEditParts</code>, may be empty
     */
    protected NodeContainerEditPart[] getSelectedParts() {
        ArrayList<NodeContainerEditPart> objects = new ArrayList<NodeContainerEditPart>(
                getSelectedObjects());

        // clean list, that is, remove all objects that are not edit
        // parts for a NodeContainer
        for (Iterator iter = objects.iterator(); iter.hasNext();) {
            Object element = iter.next();
            if (!(element instanceof NodeContainerEditPart)) {
                iter.remove();
                continue;
            }
        }

        final NodeContainerEditPart[] parts = (NodeContainerEditPart[])objects
                .toArray(new NodeContainerEditPart[objects.size()]);

        return parts;
    }

    /**
     * @see org.eclipse.gef.ui.actions.SelectionAction#getSelectedObjects()
     */
    protected List getSelectedObjects() {
        ISelectionProvider provider = m_editor.getEditorSite()
                .getSelectionProvider();
        if (provider == null) {
            return Collections.EMPTY_LIST;
        }
        ISelection sel = provider.getSelection();
        if (!(sel instanceof IStructuredSelection)) {
            return Collections.EMPTY_LIST;
        }

        return ((IStructuredSelection)sel).toList();
    }

    // /**
    // * On first update we try to hook into as workflow listener.
    // *
    // * @see org.eclipse.gef.ui.actions.UpdateAction#update()
    // */
    // public final void update() {
    // if (!m_registered) {
    // if (getManager() != null) {
    // getManager().addListener(this);
    // m_registered = true;
    // }
    // // register correct selection provider !
    // setSelectionProvider(m_editor.getSite().getSelectionProvider());
    // }
    // // call implementation of this action in the SWT UI thread
    // Display.getDefault().syncExec(new Runnable() {
    // public void run() {
    // AbstractNodeAction.super.update();
    // }
    // });
    //        
    // }

    // /**
    // * We must update the action on every change
    // *
    // * @see de.unikn.knime.core.node.workflow.WorkflowListener
    // * #workflowChanged(de.unikn.knime.core.node.workflow.WorkflowEvent)
    // */
    // public void workflowChanged(final WorkflowEvent event) {
    // update();
    // }

    /**
     * This normally needs to be overridden by subclasses.
     * 
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    protected boolean calculateEnabled() {
        return getSelectedParts().length > 0;
    }

    /**
     * Clients must provide a unique action ID.
     * 
     * @see org.eclipse.jface.action.IAction#getId()
     */
    public abstract String getId();

    /**
     * Clients must implement action code here.
     * 
     * @param nodeParts The parts that the action should be executed on.
     */
    public abstract void runOnNodes(final NodeContainerEditPart[] nodeParts);

}
