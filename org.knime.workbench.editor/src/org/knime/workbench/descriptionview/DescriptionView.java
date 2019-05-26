/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   May 9, 2019 (loki): created
 */
package org.knime.workbench.descriptionview;

import java.lang.ref.WeakReference;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.knime.workbench.descriptionview.node.HelpView;
import org.knime.workbench.descriptionview.workflowmeta.MetanodeMetaView;
import org.knime.workbench.descriptionview.workflowmeta.WorkflowMetaView;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;

/**
 * The genesis for this view is https://knime-com.atlassian.net/browse/AP-11628
 *
 * @author loki der quaeler
 */
public class DescriptionView extends ViewPart implements ISelectionListener {
    private StackLayout m_stackLayout;

    private HelpView m_nodeDescriptionView;
    private WorkflowMetaView m_workflowMetaView;
    private MetanodeMetaView m_metanodeMetaView;

    private WeakReference<IStructuredSelection> m_lastSelection;

    private Composite m_control;

    /**
     * {@inheritDoc}
     */
    @Override
    public void createPartControl(final Composite parent) {
        getViewSite().getPage().addSelectionListener(this);

        parent.setLayout(new GridLayout());

        m_control = new Composite(parent, SWT.NONE);

        m_stackLayout = new StackLayout();
        m_control.setLayout(m_stackLayout);

        final GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        m_control.setLayoutData(gd);

        m_workflowMetaView = new WorkflowMetaView(m_control);
        m_metanodeMetaView = new MetanodeMetaView(m_control);
        m_nodeDescriptionView = new HelpView(m_control);

        m_stackLayout.topControl = m_nodeDescriptionView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose () {
        getViewSite().getPage().removeSelectionListener(this);
    }

    private void moveControlToTop(final Composite c) {
        if (m_stackLayout.topControl != c) {
            m_stackLayout.topControl = c;

            m_control.layout();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFocus() {
        if (m_stackLayout.topControl == m_nodeDescriptionView) {
            m_nodeDescriptionView.setFocus();
        } else if (m_stackLayout.topControl == m_workflowMetaView) {
            m_workflowMetaView.setFocus();
        } else {
            m_metanodeMetaView.setFocus();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structuredSelection = (IStructuredSelection)selection;
            final IStructuredSelection lastSelection = (m_lastSelection == null) ? null : m_lastSelection.get();

            // we do not clear our content if nothing is selected.
            if ((structuredSelection.size() < 1) || structuredSelection.equals(lastSelection)) {
                return;
            }

            m_lastSelection = new WeakReference<>(structuredSelection);

            if (structuredSelection.getFirstElement() instanceof WorkflowRootEditPart) {
                final IEditorInput iei = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                    .getActiveEditor().getEditorInput();

                if (iei instanceof IURIEditorInput) {
                    moveControlToTop(m_workflowMetaView);
                    m_workflowMetaView.selectionChanged(structuredSelection);
                } else {
                    moveControlToTop(m_metanodeMetaView);
                    // this is not a thing (yet?) : m_metanodeMetaView.selectionChanged(structuredSelection);
                }
            } else {
                moveControlToTop(m_nodeDescriptionView);
                m_nodeDescriptionView.selectionChanged(structuredSelection);
            }
        }
    }
}
