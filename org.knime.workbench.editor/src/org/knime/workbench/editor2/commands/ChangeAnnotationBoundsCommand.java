/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   21.02.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class ChangeAnnotationBoundsCommand extends Command {

    private final Rectangle m_oldBounds;

    private final Rectangle m_newBounds;

    // TODO this must not be a hard reference (undo/redo problems when deleted)
    private final AnnotationEditPart m_annotationEditPart;

    /**
     *
     * @param portBar The workflow port bar to change
     * @param newBounds The new bounds
     */
    public ChangeAnnotationBoundsCommand(final AnnotationEditPart portBar,
            final Rectangle newBounds) {
        WorkflowAnnotation anno = (WorkflowAnnotation)portBar.getModel();
        m_oldBounds =
                new Rectangle(anno.getX(), anno.getY(), anno.getWidth(),
                        anno.getHeight());
        m_newBounds = newBounds;
        m_annotationEditPart = portBar;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
//        Dimension min = m_annotationEditPart.getFigure().getMinimumSize();
//        if (m_newBounds.width < min.width || m_newBounds.height < min.height) {
//            return false;
//        }
        return super.canExecute();
    }

    /**
     * Sets the new bounds.
     *
     * @see org.eclipse.gef.commands.Command#execute()
     */
    @Override
    public void execute() {
        WorkflowAnnotation annotation =
                (WorkflowAnnotation)m_annotationEditPart.getModel();
        annotation.setDimension(m_newBounds.x, m_newBounds.y,
                m_newBounds.width, m_newBounds.height);
        m_annotationEditPart.getFigure().setBounds(m_newBounds);
        m_annotationEditPart.getFigure().getLayoutManager()
                .layout(m_annotationEditPart.getFigure());
    }

    /**
     * Sets the old bounds.
     *
     * @see org.eclipse.gef.commands.Command#execute()
     */
    @Override
    public void undo() {
        WorkflowAnnotation annotation =
                (WorkflowAnnotation)m_annotationEditPart.getModel();
        annotation.setDimension(m_oldBounds.x, m_oldBounds.y,
                        m_oldBounds.width, m_oldBounds.height);
        // must set explicitly so that event is fired by container
        m_annotationEditPart.getFigure().setBounds(m_oldBounds);
        m_annotationEditPart.getFigure().getLayoutManager()
                .layout(m_annotationEditPart.getFigure());
    }

}
