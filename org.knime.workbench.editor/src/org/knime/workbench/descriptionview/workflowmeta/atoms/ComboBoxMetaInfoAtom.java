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
package org.knime.workbench.descriptionview.workflowmeta.atoms;

import javax.xml.transform.sax.TransformerHandler;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.knime.workbench.descriptionview.workflowmeta.LicenseType;
import org.knime.workbench.ui.workflow.metadata.MetadataItemType;
import org.knime.workbench.ui.workflow.metadata.MetadataXML;
import org.xml.sax.SAXException;

/**
 * Currently this atom is always typed to {@link MetadataItemType#LICENSE} as we have no other combobox reliant
 * types; i'm reluctant to call it something like <code>LicenseMetaInfoAtom</code> as, like {@link TextAreaMetaInfoAtom}, it
 * seems plausible that there may be other combo-box-UI dependent metadata in the future.
 *
 * @author loki der quaeler
 */
public class ComboBoxMetaInfoAtom extends MetaInfoAtom {
    private static final LicenseLabelProvider LABEL_PROVIDER = new LicenseLabelProvider();


    private String m_selectedURL;
    private ComboViewer m_editComboViewer;

    /**
     * @param label the label displayed with the value of this atom in some UI widget; this is historical and unused.
     * @param value the displayed value of this atom.
     * @param readOnly this has never been observed, and we don't currently have a use case in which we allow the user
     *            to mark something as read-only, so consider this future-proofing.
     */
    public ComboBoxMetaInfoAtom (final String label, final String value, final boolean readOnly) {
        super(MetadataItemType.LICENSE, label, value, readOnly);

        final int index = LicenseType.getIndexForLicenseWithName(value);
        final LicenseType license;
        if (index == -1) {
            license = LicenseType.getAvailableLicenses().get(0);
            m_value = license.getDisplayName();
        } else {
            license = LicenseType.getAvailableLicenses().get(index);
        }
        m_selectedURL = license.getURL();
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
        final StructuredSelection selection = (StructuredSelection)m_editComboViewer.getSelection();
        final LicenseType license = (LicenseType)selection.getFirstElement();

        m_value = license.getDisplayName();
        m_selectedURL = license.getURL();

        m_editComboViewer = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void populateContainerForDisplay(final Composite parent) {
        final HyperLinkLabel hll = new HyperLinkLabel(parent, false, m_selectedURL);
        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        gd.verticalAlignment = SWT.BOTTOM;
        gd.grabExcessHorizontalSpace = true;
        hll.setLayoutData(gd);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void populateContainerForEdit(final Composite parent) {
        m_editComboViewer = new ComboViewer(parent, SWT.READ_ONLY);

        m_editComboViewer.setContentProvider(ArrayContentProvider.getInstance());
        m_editComboViewer.setLabelProvider(LABEL_PROVIDER);
        m_editComboViewer.setInput(LicenseType.getAvailableLicenses());

        int index = LicenseType.getIndexForLicenseWithName(m_value);
        if (index == -1) {
            index = 0;
        }
        m_editComboViewer.setSelection(new StructuredSelection(m_editComboViewer.getElementAt(index)), true);
        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        gd.grabExcessHorizontalSpace = true;
        m_editComboViewer.getCombo().setLayoutData(gd);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final TransformerHandler parentElement) throws SAXException {
        if (hasContent()) {
            save(parentElement, MetadataXML.COMBOBOX);
        }
    }


    private static class LicenseLabelProvider extends LabelProvider {
        /**
         * {@inheritDoc}
         */
        @Override
        public String getText(final Object o) {
            if (o instanceof LicenseType) {
                return ((LicenseType)o).getDisplayName();
            }

            return null;
        }
    }
}
