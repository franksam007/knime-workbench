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
 *   26.05.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.editparts;

import java.util.Arrays;
import java.util.EventObject;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LayoutManager;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.commands.CommandStackListener;

import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.workflow.WorkflowEvent;
import de.unikn.knime.core.node.workflow.WorkflowListener;
import de.unikn.knime.core.node.workflow.WorkflowManager;
import de.unikn.knime.workbench.editor2.editparts.policy.
       NewWorkflowContainerEditPolicy;
import de.unikn.knime.workbench.editor2.editparts.policy.
       NewWorkflowXYLayoutPolicy;
import de.unikn.knime.workbench.editor2.figures.WorkflowFigure;
import de.unikn.knime.workbench.editor2.figures.WorkflowLayout;

/**
 * Root controller for the <code>WorkflowManager</code> model object. Consider
 * this as the controller for the "background" of the editor. It always has a
 * <code>WorkflowManager</code> as its model object.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class WorkflowRootEditPart extends AbstractWorkflowEditPart implements
        WorkflowListener, CommandStackListener {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(WorkflowRootEditPart.class);

    /**
     * @return The <code>WorkflowManager</code> that is used as model for this
     *         edit part
     */
    public WorkflowManager getWorkflowManager() {
        return (WorkflowManager) getModel();
    }

    /**
     * Returns the model chidlren, that is, the <code>NodeConatiner</code>s
     * that are stored in the workflow manager.
     * 
     * @see org.eclipse.gef.editparts.AbstractEditPart#getModelChildren()
     */
    protected List getModelChildren() {
        return Arrays.asList(getWorkflowManager().getNodes());
    }

    /**
     * Activate controller, register as workflow listener.
     * 
     * @see org.eclipse.gef.EditPart#activate()
     */
    public void activate() {
        super.activate();
        LOGGER.debug("WorkflowRootEditPart activated");

        // register as listener on model object
        getWorkflowManager().addListener(this);

        // add as listener on the command stack
        getViewer().getEditDomain().getCommandStack().addCommandStackListener(
                this);

    }

    /**
     * Deactivate controller.
     * 
     * @see org.eclipse.gef.EditPart#deactivate()
     */
    public void deactivate() {
        super.deactivate();
        LOGGER.debug("WorkflowRootEditPart deactivated");

        getWorkflowManager().removeListener(this);
        getViewer().getEditDomain().getCommandStack()
                .removeCommandStackListener(this);

    }

    /**
     * Creates the root(="background") figure and sets the appropriate lazout
     * manager.
     * 
     * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart#createFigure()
     */
    protected IFigure createFigure() {
        IFigure backgroundFigure = new WorkflowFigure();

        LayoutManager l = new WorkflowLayout();
        backgroundFigure.setLayoutManager(l);

        return backgroundFigure;
    }

    /**
     * This installes the edit policies for the root EditPart:
     * <ul>
     * <li><code>EditPolicy.CONTAINER_ROLE</code> - this serves as a
     * container for nodes</li>
     * <li><code>EditPolicy.LAYOUT_ROLE</code> - this edit part a layout that
     * allows children to be moved</li>.
     * </ul>
     * 
     * @see org.eclipse.gef.editparts.AbstractEditPart#createEditPolicies()
     */
    protected void createEditPolicies() {

        // install the CONTAINER_ROLE
        installEditPolicy(EditPolicy.CONTAINER_ROLE,
                new NewWorkflowContainerEditPolicy());

        // install the LAYOUT_ROLE
        installEditPolicy(EditPolicy.LAYOUT_ROLE,
                new NewWorkflowXYLayoutPolicy());

    }

    /**
     * Controller is getting notified about model changes. This invokes
     * <code>refreshChildren</code> keep in sync with the model.
     * 
     * @see de.unikn.knime.core.node.workflow.WorkflowListener
     *      #workflowChanged(de.unikn.knime.core.node.workflow.WorkflowEvent)
     */
    public void workflowChanged(final WorkflowEvent event) {
        LOGGER.debug("WorkflowRoot: workflow changed, refreshing "
                + "children/connections..");

        // refreshing the children
        refreshChildren();

        // refresing connections
        refreshSourceConnections();
        refreshTargetConnections();

    }

    /**
     * 
     * @see org.eclipse.gef.commands.CommandStackListener
     *      #commandStackChanged(java.util.EventObject)
     */
    public void commandStackChanged(final EventObject event) {
        LOGGER.debug("WorkflowRoot: command stack changed");

    }

}
