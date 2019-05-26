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
package org.knime.workbench.descriptionview.workflowmeta;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.knime.core.node.NodeLogger;
import org.knime.workbench.descriptionview.workflowmeta.atoms.ComboBoxMetaInfoAtom;
import org.knime.workbench.descriptionview.workflowmeta.atoms.LinkMetaInfoAtom;
import org.knime.workbench.descriptionview.workflowmeta.atoms.MetaInfoAtom;
import org.knime.workbench.descriptionview.workflowmeta.atoms.TagMetaInfoAtom;
import org.knime.workbench.descriptionview.workflowmeta.atoms.TextAreaMetaInfoAtom;
import org.knime.workbench.descriptionview.workflowmeta.atoms.TextFieldMetaInfoAtom;
import org.knime.workbench.ui.workflow.metadata.MetadataItemType;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * This class provides a UI supported form of the metadata representation, as a wrapper with augmented functionality.
 *
 * @author loki der quaeler
 */
public class MetadataModelFacilitator implements MetaInfoAtom.DeletionListener {
    /** This is the label which has been historically used to denote the author **/
    public static final String AUTHOR_LABEL = "Author";
    /** This is the label which has been historically used to denote the description **/
    public static final String DESCRIPTION_LABEL = "Comments";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(MetadataModelFacilitator.class);


    private MetaInfoAtom m_authorAtom;              // 1 TextFieldMetaInfoAtom
    private MetaInfoAtom m_descriptionAtom;         // 1 TextAreaMetaInfoAtom
    private final ArrayList<MetaInfoAtom> m_tagAtoms;     // 1-N TagMetaInfoAtom
    private final ArrayList<MetaInfoAtom> m_linkAtoms;    // 1-N LinkMetaInfoAtom
    private MetaInfoAtom m_licenseAtom;             // 1 ComboBoxMetaInfoAtom

    private MetaInfoAtom.DeletionListener m_deletionListener;

    // for edit state store
    private final ArrayList<MetaInfoAtom> m_savedTagAtoms;
    private final ArrayList<MetaInfoAtom> m_savedLinkAtoms;

    MetadataModelFacilitator() {
        m_tagAtoms = new ArrayList<>();
        m_linkAtoms = new ArrayList<>();

        m_savedTagAtoms = new ArrayList<>();
        m_savedLinkAtoms = new ArrayList<>();
    }

    /**
     * @param label the display label preferencing the UI widget
     * @param type this will be null for historical (pre-3.8.0) metadata in which case it we consult the label; if the
     *            label is Author or Comments, then we keep, however we discard anything else (which consists of only
     *            creation date.)
     * @param value the content of the metadata (e.g type == AUTHOR_TYPE, value == "Albert Camus")
     * @param isReadOnly this has never been observed, and we don't currently have a use case in which we allow the user
     *            to mark something as read-only, so consider this future-proofing.
     * @param otherAttributes key-value pairs of non-universal attributes of the metadata element
     * @throws SAXException if something gets throw in an anticipatable location, we'll wrap it in a SAXException and
     *             re-throw it.
     */
    void processElement(final String label, final String type, final String value, final boolean isReadOnly,
        final Map<String, String> otherAttributes) throws SAXException {
        final MetadataItemType typeToUse;
        if (type == null) {
            // we've read in metadata created in a version prior to 3.8.0 (and which has not been resaved since.)
            if (AUTHOR_LABEL.equals(label)) {
                typeToUse = MetadataItemType.AUTHOR;
            } else if (DESCRIPTION_LABEL.equals(label)) {
                typeToUse = MetadataItemType.DESCRIPTION;
            } else {
                return;
            }
        } else {
            typeToUse = MetadataItemType.getInfoTypeForType(type);
        }

        if (typeToUse != null) {
            MetaInfoAtom mia = null;

            switch (typeToUse) {
                case TAG:
                    mia = new TagMetaInfoAtom(label, value, isReadOnly);
                    m_tagAtoms.add(mia);
                    break;
                case LINK:
                    mia = new LinkMetaInfoAtom(label, value, isReadOnly, otherAttributes);
                    m_linkAtoms.add(mia);
                    break;
                case AUTHOR:
                    m_authorAtom = new TextFieldMetaInfoAtom(label, value, isReadOnly);
                    mia = m_authorAtom;
                    break;
                case DESCRIPTION:
                    m_descriptionAtom = new TextAreaMetaInfoAtom(label, value, isReadOnly);
                    mia = m_descriptionAtom;
                    break;
                case LICENSE:
                    m_licenseAtom = new ComboBoxMetaInfoAtom(label, value, isReadOnly);
                    mia = m_licenseAtom;
                    break;
            }

            if (mia != null) {
                mia.addChangeListener(this);
            }
        }
    }

    void parsingHasFinished() {
        if (m_authorAtom == null) {
            m_authorAtom = new TextFieldMetaInfoAtom("legacy-author", null, true);
            m_authorAtom.addChangeListener(this);
        }

        if (m_descriptionAtom == null) {
            m_descriptionAtom = new TextAreaMetaInfoAtom("legacy-comments", null, true);
            m_descriptionAtom.addChangeListener(this);
        }

        if (m_licenseAtom == null) {
            m_licenseAtom =
                new ComboBoxMetaInfoAtom("legacy", LicenseType.getAvailableLicenses().get(0).getDisplayName(), false);
            m_licenseAtom.addChangeListener(this);
        }
    }

    void writeMetadata(final File metadataFile) throws IOException {
        final int elementCount = totalElementCount();

        try (final OutputStream os = new FileOutputStream(metadataFile)) {
            final SAXTransformerFactory fac = (SAXTransformerFactory)TransformerFactory.newInstance();
            final TransformerHandler handler = fac.newTransformerHandler();
            final Transformer t = handler.getTransformer();
            t.setOutputProperty(OutputKeys.METHOD, "xml");
            t.setOutputProperty(OutputKeys.INDENT, "yes");

            handler.setResult(new StreamResult(os));

            handler.startDocument();
            final AttributesImpl atts = new AttributesImpl();
            // We write this nrOfElements value though it's not ever parsed in our codebase that i can find;
            //      continuing to do this to respect history.
            atts.addAttribute(null, null, "nrOfElements", "CDATA", Integer.toString(elementCount));
            handler.startElement(null, null, "KNIMEMetaInfo", atts);

            m_authorAtom.save(handler);
            m_descriptionAtom.save(handler);
            m_licenseAtom.save(handler);
            for (final MetaInfoAtom mia : m_tagAtoms) {
                mia.save(handler);
            }
            for (final MetaInfoAtom mia : m_linkAtoms) {
                mia.save(handler);
            }

            handler.endElement(null, null, "KNIMEMetaInfo");
            handler.endDocument();
        } catch (final SAXException | TransformerConfigurationException e) {
            throw new IOException("Caught exception while writing metadata.", e);
        }
    }

    /**
     * @return the number of non-empty atoms held
     */
    public int totalElementCount() {
        int elementCount = (m_authorAtom.hasContent() ? 1 : 0) + (m_descriptionAtom.hasContent() ? 1 : 0)
            + (m_licenseAtom.hasContent() ? 1 : 0);

        if (m_tagAtoms.size() == 1) {
            elementCount += m_tagAtoms.get(0).hasContent() ? 1 : 0;
        } else {
            elementCount += m_tagAtoms.size();
        }

        if (m_linkAtoms.size() == 1) {
            elementCount += m_linkAtoms.get(0).hasContent() ? 1 : 0;
        } else {
            elementCount += m_linkAtoms.size();
        }

        return elementCount;
    }

    /**
     * @param listener an implementor which wishes to know when an atom has been deleted
     */
    public void setDeletionListener(final MetaInfoAtom.DeletionListener listener) {
        m_deletionListener = listener;
    }

    /**
     * Invoking this allows this instance to store a copy of its state, and alert all atoms to store theirs.
     */
    public void storeStateForEdit() {
        m_savedTagAtoms.addAll(m_tagAtoms);
        m_savedLinkAtoms.addAll(m_linkAtoms);

        m_authorAtom.storeStateForEdit();
        m_descriptionAtom.storeStateForEdit();
        m_licenseAtom.storeStateForEdit();
        m_tagAtoms.stream().forEach((tag) -> {
            tag.storeStateForEdit();
        });
        m_linkAtoms.stream().forEach((link) -> {
            link.storeStateForEdit();
        });
    }

    /**
     * Invoking this restores this instance's state to the one stored during the call to {@link #storeStateForEdit()}
     * and alerts all atoms to do the same - the implementation of this action being subjective to the type of atom.
     */
    public void restoreState() {
        m_tagAtoms.clear();
        m_linkAtoms.clear();
        m_tagAtoms.addAll(m_savedTagAtoms);
        m_linkAtoms.addAll(m_savedLinkAtoms);
        m_savedTagAtoms.clear();
        m_savedLinkAtoms.clear();

        m_authorAtom.restoreState();
        m_descriptionAtom.restoreState();
        m_licenseAtom.restoreState();
        m_tagAtoms.stream().forEach((tag) -> {
            tag.restoreState();
        });
        m_linkAtoms.stream().forEach((link) -> {
            link.restoreState();
        });
    }

    /**
     * Invoking this releases this instance's copied state to maintain the modified-during-edit changes; it also alerts
     * all atoms to also commit their edits - the implementation of this action being subjective to the type of atom.
     */
    public void commitEdit() {
        m_savedTagAtoms.clear();
        m_savedLinkAtoms.clear();

        m_authorAtom.commitEdit();
        m_descriptionAtom.commitEdit();
        m_licenseAtom.commitEdit();
        m_tagAtoms.stream().forEach((tag) -> {
            tag.commitEdit();
        });
        m_linkAtoms.stream().forEach((link) -> {
            link.commitEdit();
        });
    }

    /**
     * @return the atom representing the author
     */
    public MetaInfoAtom getAuthor() {
        return m_authorAtom;
    }

    /**
     * @return the atom representing the description
     */
    public MetaInfoAtom getDescription() {
        return m_descriptionAtom;
    }

    /**
     * @return a mutable list of tags
     */
    public List<MetaInfoAtom> getTags() {
        return m_tagAtoms;
    }

    /**
     * @param tagText the text of the tag
     * @return the created instance which was added to the internal store
     */
    public MetaInfoAtom addTag(final String tagText) {
        final MetaInfoAtom mia = new TagMetaInfoAtom("legacy", tagText, false);

        mia.addChangeListener(this);
        m_tagAtoms.add(mia);

        return mia;
    }

    /**
     * @return a mutable list of links
     */
    public List<MetaInfoAtom> getLinks() {
        return m_linkAtoms;
    }

    /**
     * @param url the fully formed URL for the link
     * @param title the display text for the link
     * @param type the type for the link
     * @return the created instance which was added to the internal store
     */
    public MetaInfoAtom addLink(final String url, final String title, final String type) {
        final MetaInfoAtom mia = new LinkMetaInfoAtom("legacy", title, type, url, false);

        mia.addChangeListener(this);
        m_linkAtoms.add(mia);

        return mia;
    }

    /**
     * @return the atom representing the license type
     */
    public MetaInfoAtom getLicense() {
        return m_licenseAtom;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void metaInfoAtomDeleted(final MetaInfoAtom deletedAtom) {
        switch (deletedAtom.getType()) {
            case TAG:
                if (!m_tagAtoms.remove(deletedAtom)) {
                    LOGGER.warn("Could not find tag [" + deletedAtom.getValue() + "] for removal.");
                }
                break;
            case LINK:
                if (!m_linkAtoms.remove(deletedAtom)) {
                    LOGGER.warn("Could not find link [" + deletedAtom.getValue() + "] for removal.");
                }
                break;
            default:
                LOGGER.error("Info atom of type " + deletedAtom.getType()
                    + " reports itself as deleted which should not be possible.");
                break;
        }

        if (m_deletionListener != null) {
            m_deletionListener.metaInfoAtomDeleted(deletedAtom);
        }
    }
}
