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
 *   May 13, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.workflowmeta.atoms;

import javax.xml.transform.sax.TransformerHandler;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.knime.workbench.descriptionview.workflowmeta.WorkflowMetaView;
import org.knime.workbench.ui.workflow.metadata.MetadataItemType;
import org.knime.workbench.ui.workflow.metadata.MetadataXML;
import org.xml.sax.SAXException;

/**
 * Currently this atom is always typed to {@link MetadataItemType#AUTHOR} as we have no other text field
 * reliant types; i'm reluctant to call it something like <code>AuthorMetaInfoAtom</code> as, like
 * {@link ComboBoxMetaInfoAtom}, it seems plausible that there may be other text-field-UI dependent metadata in the
 * future.
 *
 * @author loki der quaeler
 */
public class TextFieldMetaInfoAtom extends MetaInfoAtom {
    private Text m_editTextField;

    /**
     * A class for atoms whose edit-representation utilize a text field.
     *
     * @param label the label displayed with the value of this atom in some UI widget.
     * @param value the displayed value of this atom.
     * @param readOnly this has never been observed, and we don't currently have a use case in which we allow the user
     *            to mark something as read-only, so consider this future-proofing.
     */
    public TextFieldMetaInfoAtom(final String label, final String value, final boolean readOnly) {
        super(MetadataItemType.AUTHOR, label, value, readOnly);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeStateForEdit() { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreState() { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitEdit() {
        m_value = m_editTextField.getText();
        m_editTextField = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void populateContainerForDisplay(final Composite parent) {
        final Label l = new Label(parent, SWT.RIGHT);
        l.setFont(WorkflowMetaView.AUTHOR_FONT);
        l.setForeground(WorkflowMetaView.TEXT_COLOR);

        if (MetadataItemType.AUTHOR.equals(getType())) {
            l.setText("by " + m_value);
        } else {
            l.setText(m_value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void populateContainerForEdit(final Composite parent) {
        m_editTextField = new Text(parent, SWT.BORDER);

        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        final Point greatGrandparentSize = parent.getParent().getParent().getSize();
        gd.minimumWidth = greatGrandparentSize.x / 2;

        m_editTextField.setLayoutData(gd);
        m_editTextField.setText((m_value == null) ? "" : m_value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final TransformerHandler parentElement) throws SAXException {
        if (hasContent()) {
            save(parentElement, MetadataXML.TEXT);
        }
    }
}
