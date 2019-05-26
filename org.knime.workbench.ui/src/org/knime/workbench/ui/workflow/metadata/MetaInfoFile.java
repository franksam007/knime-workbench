/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.workbench.ui.workflow.metadata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.util.FileUtil;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Helper class for the meta info file which contains the meta information entered by the user for workflow groups and
 * workflows, such as author, date, comments.
 *
 * Fabian Dill wrote the original version off which this class is based.
 */
public final class MetaInfoFile {
    /** Preference key for a workflow template. */
    public static final String PREF_KEY_META_INFO_TEMPLATE_WF = "org.knime.ui.metainfo.template.workflow";

    /** Preference key for a workflow group template. */
    public static final String PREF_KEY_META_INFO_TEMPLATE_WFS = "org.knime.ui.metainfo.template.workflowset";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(MetaInfoFile.class);

    /**
     * If there is a template for workflow metadata, it is attempted to be used to create the metadata for parent
     * directory; otherwise, if the metadata file already exists, nothing is none and if it does not exist a default
     * (author only) one is created.
     *
     * @param parent parent file
     * @param isWorkflow true if it is a meta info for a workflow
     * @return a handle to the metainfo file
     */
    public static File createOrGetMetaInfoFile(final File parent, final boolean isWorkflow) {
        final File meta = new File(parent, WorkflowPersistor.METAINFO_FILE);

        // look into preference store
        final File f = getFileFromPreferences(isWorkflow);
        if (f != null) {
            writeFileFromPreferences(meta, f);
        } else if (!meta.exists() || (meta.length() == 0)) { // Future TODO better detection of a corrupt file
            createDefaultFileFallback(meta);
        }

        return meta;
    }

    private static void writeFileFromPreferences(final File meta, final File f) {
        try {
            FileUtil.copy(f, meta);
        } catch (final IOException io) {
            LOGGER.error(
                "Error while creating meta info from template for " + meta.getParentFile().getName()
                    + ". Creating default file...",
                io);

            createDefaultFileFallback(meta);
        }
    }

    private static File getFileFromPreferences(final boolean isWorkflow) {
        String key = PREF_KEY_META_INFO_TEMPLATE_WFS;
        if (isWorkflow) {
            key = PREF_KEY_META_INFO_TEMPLATE_WF;
        }
        final String fileName = KNIMEUIPlugin.getDefault().getPreferenceStore().getString(key);
        if ((fileName == null) || fileName.isEmpty()) {
            return null;
        }

        final File f = new File(fileName);
        if (!f.exists() || (f.length() == 0)) {
            return null;
        }
        return f;
    }

    private static void createDefaultFileFallback(final File meta) {
        try {
            final SAXTransformerFactory fac = (SAXTransformerFactory)TransformerFactory.newInstance();
            final TransformerHandler handler = fac.newTransformerHandler();

            final Transformer t = handler.getTransformer();
            t.setOutputProperty(OutputKeys.METHOD, "xml");
            t.setOutputProperty(OutputKeys.INDENT, "yes");

            try (final OutputStream out = new FileOutputStream(meta)) {
                handler.setResult(new StreamResult(out));

                handler.startDocument();
                AttributesImpl atts = new AttributesImpl();
                atts.addAttribute(null, null, "nrOfElements", "CDATA", "1");
                handler.startElement(null, null, "KNIMEMetaInfo", atts);

                // author
                atts = new AttributesImpl();
                atts.addAttribute(null, null, MetadataXML.FORM, "CDATA", MetadataXML.TEXT);
                atts.addAttribute(null, null, MetadataXML.NAME, "CDATA", "Author");
                atts.addAttribute(null, null, MetadataXML.TYPE, "CDATA", MetadataItemType.AUTHOR.getType());
                handler.startElement(null, null, MetadataXML.ELEMENT, atts);
                final String userName = System.getProperty("user.name");
                if ((userName != null) && (userName.length() > 0)) {
                    final char[] value = userName.toCharArray();
                    handler.characters(value, 0, value.length);
                }
                handler.endElement(null, null, MetadataXML.ELEMENT);

                handler.endElement(null, null, "KNIMEMetaInfo");
                handler.endDocument();
            }
        } catch (final Exception e) {
            LOGGER.error("Error while trying to create default meta info file for " + meta.getParentFile().getName(),
                e);
        }
    }


    private MetaInfoFile() { }
}
