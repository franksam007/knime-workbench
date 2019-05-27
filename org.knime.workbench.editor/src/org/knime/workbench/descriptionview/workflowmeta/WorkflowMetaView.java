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
 *   Apr 28, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.workflowmeta;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.util.FileUtil;
import org.knime.core.util.SWTUtilities;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.descriptionview.workflowmeta.atoms.MetaInfoAtom;
import org.knime.workbench.editor2.directannotationedit.FlatButton;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.ui.workflow.metadata.MetaInfoFile;

/**
 * This is the view reponsible for displaying, and potentially allowing the editing of, the meta-information associated
 * with a workflow; for example:
 *      . description
 *      . tags
 *      . links
 *      . license
 *      . author
 *
 * The genesis for this view is https://knime-com.atlassian.net/browse/AP-11628
 *
 * @author loki der quaeler
 */
public class WorkflowMetaView extends Composite implements MetaInfoAtom.DeletionListener {
    /** Display font which the author read-only should use. **/
    public static final Font AUTHOR_FONT = JFaceResources.getFontRegistry().getItalic(JFaceResources.DIALOG_FONT);
    /** Display font which the read-only versions of metadata that is not the author should use. **/
    public static final Font NOT_AUTHOR_FONT = JFaceResources.getFont(JFaceResources.DIALOG_FONT);
    /** Font which should be used with the n-ary close character. **/
    public static final Font CLOSE_N_ARY_FONT = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
    /** The read-only text color. **/
    public static final Color TEXT_COLOR = new Color(PlatformUI.getWorkbench().getDisplay(), 128, 128, 128);
    /** The fill color for the header bar and other widgets (like tag chiclets.) **/
    public static final Color GENERAL_FILL_COLOR = new Color(PlatformUI.getWorkbench().getDisplay(), 240, 240, 241);

    private static final String NO_AUTHOR_TEXT = "by an uncited author";

    private static final Image CANCEL_IMAGE = ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "/icons/meta-view-cancel.png");
    private static final Image EDIT_IMAGE = ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "/icons/meta-view-edit.png");
    private static final Image SAVE_IMAGE = ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "/icons/meta-view-save.png");

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowMetaView.class);

    private static Text addLabelTextFieldCouplet(final Composite parent, final String labelText) {
        final Label l = new Label(parent, SWT.LEFT);
        l.setText(labelText);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        l.setLayoutData(gd);

        final Text textField = new Text(parent, SWT.BORDER);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        textField.setLayoutData(gd);

        return textField;
    }


    private final Composite m_headerBar;
    private final Label m_headerLabel;
    private final Composite m_headerButtonPane;

    private final Composite m_descriptionSection;
    private final Composite m_descriptionContentPane;

    private final Composite m_tagsSection;
    private final Composite m_tagsContentPane;
    private final Composite m_tagsAddContentPane;
    private final Composite m_tagsTagsContentPane;
    private Text m_tagAddTextField;
    private Button m_tagsAddButton;

    private final Composite m_linksSection;
    private final Composite m_linksContentPane;
    private final Composite m_linksAddContentPane;
    private final Composite m_linksLinksContentPane;
    private Text m_linksAddURLTextField;
    private Text m_linksAddTitleTextField;
    private Text m_linksAddTypeTextField;
    private Button m_linksAddButton;

    private final Composite m_licenseSection;
    private final Composite m_licenseContentPane;

    private final Composite m_authorSection;
    private final Composite m_authorContentPane;

    // resources to dispose
    private final Font m_headerLabelFont;
    private final Color m_headerLabelFontColor;
    private final Color m_headerBarBorder;

    private File m_metadataFile;
    private MetadataModelFacilitator m_modelFacilitator;
    // we cache this to avoid needlessly reparsing the metadata file each time someone jumps from node description
    //      to workflow description in the same workflow.
    private URI m_lastDisplayedURISource;

    private final AtomicBoolean m_inEditMode;

    /**
     * @param parent
     */
    public WorkflowMetaView(final Composite parent) {
        super(parent, SWT.NONE);

        m_inEditMode = new AtomicBoolean(false);

        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 3;
        gl.marginWidth = 3;
        setLayout(gl);

        m_headerBar = new Composite(this, SWT.NONE);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        m_headerBar.setLayoutData(gd);
        gl = new GridLayout(2, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        gl.marginRight = 9;
        m_headerBar.setLayout(gl);
        m_headerBarBorder = new Color(parent.getDisplay(), 228, 228, 228);
        m_headerBar.setBackground(GENERAL_FILL_COLOR);

        m_headerLabel = new Label(m_headerBar, SWT.LEFT);
        m_headerLabel.setText("");
        gd = new GridData();
        gd.horizontalIndent = 9;
        m_headerLabel.setLayoutData(gd);
        final FontData[] baseFD = CLOSE_N_ARY_FONT.getFontData();
        final FontData headerFD = new FontData(baseFD[0].getName(), baseFD[0].getHeight() + 9, baseFD[0].getStyle());
        m_headerLabelFont = new Font(parent.getDisplay(), headerFD);
        m_headerLabel.setFont(m_headerLabelFont);
        m_headerLabelFontColor = new Color(parent.getDisplay(), 88, 88, 88);
        m_headerLabel.setForeground(m_headerLabelFontColor);

        m_headerButtonPane = new Composite(m_headerBar, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.RIGHT;
        gd.grabExcessHorizontalSpace = true;
        gd.heightHint = 24;
        gd.widthHint = 48;
        gd.horizontalIndent = 9;
        m_headerButtonPane.setLayoutData(gd);
        gl = new GridLayout(2, true);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        m_headerButtonPane.setLayout(gl);



        Composite[] sectionAndContentPane = createVerticalSection("Description", CLOSE_N_ARY_FONT);
        m_descriptionSection = sectionAndContentPane[0];
        m_descriptionContentPane = sectionAndContentPane[1];



        sectionAndContentPane = createVerticalSection("Tags", CLOSE_N_ARY_FONT);
        m_tagsSection = sectionAndContentPane[0];
        m_tagsContentPane = sectionAndContentPane[1];
        m_tagsAddContentPane = new Composite(m_tagsContentPane, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.heightHint = 27;
        m_tagsAddContentPane.setLayoutData(gd);
        gl = new GridLayout(2, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        gl.horizontalSpacing = 3;
        m_tagsAddContentPane.setLayout(gl);
        m_tagsTagsContentPane = new Composite(m_tagsContentPane, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        m_tagsTagsContentPane.setLayoutData(gd);
        final RowLayout rl = new RowLayout();
        rl.wrap = true;
        rl.pack = true;
        rl.type = SWT.HORIZONTAL;
        rl.marginWidth = 3;
        rl.marginHeight = 2;
        m_tagsTagsContentPane.setLayout(rl);



        sectionAndContentPane = createVerticalSection("Links", CLOSE_N_ARY_FONT);
        m_linksSection = sectionAndContentPane[0];
        m_linksContentPane = sectionAndContentPane[1];
        m_linksAddContentPane = new Composite(m_linksContentPane, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        m_linksAddContentPane.setLayoutData(gd);
        gl = new GridLayout(2, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        gl.horizontalSpacing = 3;
        m_linksAddContentPane.setLayout(gl);
        m_linksLinksContentPane = new Composite(m_linksContentPane, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        m_linksLinksContentPane.setLayoutData(gd);
        gl = new GridLayout(1, false);
        gl.marginHeight = 0;
        m_linksLinksContentPane.setLayout(gl);



        m_licenseSection = new Composite(this, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        m_licenseSection.setLayoutData(gd);
        gl = new GridLayout(2, false);
        gl.marginTop = 5;
        gl.marginBottom = 12;
        gl.marginWidth = 0;
        m_licenseSection.setLayout(gl);
        Label l = new Label(m_licenseSection, SWT.LEFT);
        l.setText("License");
        l.setFont(CLOSE_N_ARY_FONT);
        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        l.setLayoutData(gd);
        m_licenseContentPane = new Composite(m_licenseSection, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalIndent = 9;
        gd.heightHint = 24;
        m_licenseContentPane.setLayoutData(gd);
        gl = new GridLayout(1, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        m_licenseContentPane.setLayout(gl);



        m_authorSection = new Composite(this, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.RIGHT;
        gd.verticalAlignment = SWT.BOTTOM;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        m_authorSection.setLayoutData(gd);
        gl = new GridLayout(1, false);
        gl.marginTop = 5;
        gl.marginBottom = 12;
        gl.marginWidth = 0;
        m_authorSection.setLayout(gl);
        m_authorContentPane = new Composite(m_authorSection, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.RIGHT;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalIndent = 3;
        m_authorContentPane.setLayoutData(gd);
        gl = new GridLayout(1, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        m_authorContentPane.setLayout(gl);

        setHeaderBarButtons();

        SWTUtilities.spaceReclaimingSetVisible(m_descriptionSection, false);
        SWTUtilities.spaceReclaimingSetVisible(m_tagsSection, false);
        SWTUtilities.spaceReclaimingSetVisible(m_linksSection, false);
        SWTUtilities.spaceReclaimingSetVisible(m_licenseSection, false);
        SWTUtilities.spaceReclaimingSetVisible(m_authorSection, false);

        pack();
    }

    @Override
    public void dispose() {
        m_headerLabelFont.dispose();
        m_headerLabelFontColor.dispose();

        m_headerBarBorder.dispose();

        super.dispose();
    }

    /**
     * @return whether the view is currently in edit mode
     */
    public boolean inEditMode() {
        return m_inEditMode.get();
    }

    /**
     * If the view is currently in edit mode, the mode is ended with either a save or cancel.
     *
     * @param shouldSave if true, then the model state is committed, otherwise restored.
     */
    public void endEditMode(final boolean shouldSave) {
        if (m_inEditMode.getAndSet(false)) {
            if (shouldSave) {
                performSave();
            } else {
                performDiscard();
            }
        }
    }

    /**
     * @param selection the selection passed along from the ISelectionListener
     */
    public void selectionChanged(final IStructuredSelection selection) {
        final IEditorInput iei =
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor().getEditorInput();

        if (iei instanceof IURIEditorInput) {
            final IURIEditorInput uriEI = (IURIEditorInput)iei;
            final URI uri = uriEI.getURI();

            if (!uri.equals(m_lastDisplayedURISource)) {
                final WorkflowRootEditPart wrep = (WorkflowRootEditPart)selection.getFirstElement();
                final WorkflowManagerUI wmUI = wrep.getWorkflowManager();
                final String workflowName = wmUI.getName();

                m_headerLabel.getDisplay().asyncExec(() -> {
                    m_headerLabel.setText(workflowName);
                    m_headerBar.layout();
                });

                final SAXInputHandler handler = new SAXInputHandler();
                try {
                    final URL u = uri.toURL();
                    final File f = FileUtil.getFileFromURL(u);

                    final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
                    parserFactory.setNamespaceAware(true);

                    final SAXParser parser = parserFactory.newSAXParser();
                    m_metadataFile = MetaInfoFile.createOrGetMetaInfoFile(f.getParentFile(), true);
                    parser.parse(m_metadataFile, handler);
                } catch (Exception e) {
                    LOGGER.error("Failed to parse the workflow metadata file.", e);
                }

                m_modelFacilitator = handler.getModelFacilitator();
                m_modelFacilitator.setDeletionListener(this);
            }

            m_lastDisplayedURISource = uri;

            getDisplay().asyncExec(() -> {
                updateDisplay();
            });
        }
    }

    private void updateDisplay() {
        final boolean editMode = m_inEditMode.get();

        SWTUtilities.removeAllChildren(m_descriptionContentPane);
        MetaInfoAtom mia = m_modelFacilitator.getDescription();
        if (editMode || mia.hasContent()) {
            if (editMode) {
                mia.populateContainerForEdit(m_descriptionContentPane);
            } else {
                mia.populateContainerForDisplay(m_descriptionContentPane);
            }

            SWTUtilities.spaceReclaimingSetVisible(m_descriptionSection, true);
        } else {
            SWTUtilities.spaceReclaimingSetVisible(m_descriptionSection, false);
        }

        SWTUtilities.removeAllChildren(m_tagsAddContentPane);
        SWTUtilities.removeAllChildren(m_tagsTagsContentPane);
        List<MetaInfoAtom> atoms = m_modelFacilitator.getTags();
        if (editMode || (atoms.size() > 0)) {
            if (editMode) {
                SWTUtilities.spaceReclaimingSetVisible(m_tagsAddContentPane, true);
                createTagsAddUI();
            } else {
                SWTUtilities.spaceReclaimingSetVisible(m_tagsAddContentPane, false);
            }

            atoms.stream().forEach((atom) -> {
                if (editMode) {
                    atom.populateContainerForEdit(m_tagsTagsContentPane);
                } else {
                    atom.populateContainerForDisplay(m_tagsTagsContentPane);
                }
            });

            SWTUtilities.spaceReclaimingSetVisible(m_tagsSection, true);
        } else {
            SWTUtilities.spaceReclaimingSetVisible(m_tagsSection, false);
        }

        SWTUtilities.removeAllChildren(m_linksAddContentPane);
        SWTUtilities.removeAllChildren(m_linksLinksContentPane);
        atoms = m_modelFacilitator.getLinks();
        if (editMode || (atoms.size() > 0)) {
            if (editMode) {
                SWTUtilities.spaceReclaimingSetVisible(m_linksAddContentPane, true);
                createLinksAddUI();
            } else {
                SWTUtilities.spaceReclaimingSetVisible(m_linksAddContentPane, false);
            }

            atoms.stream().forEach((atom) -> {
                if (editMode) {
                    atom.populateContainerForEdit(m_linksLinksContentPane);
                } else {
                    atom.populateContainerForDisplay(m_linksLinksContentPane);
                }
            });

            SWTUtilities.spaceReclaimingSetVisible(m_linksSection, true);
        } else {
            SWTUtilities.spaceReclaimingSetVisible(m_linksSection, false);
        }

        SWTUtilities.removeAllChildren(m_licenseContentPane);
        mia = m_modelFacilitator.getLicense();
        // We currently *always* have a license - this if block is 'just in case'
        if (editMode || mia.hasContent()) {
            if (editMode) {
                mia.populateContainerForEdit(m_licenseContentPane);
            } else {
                mia.populateContainerForDisplay(m_licenseContentPane);
            }

            SWTUtilities.spaceReclaimingSetVisible(m_licenseSection, true);
        } else {
            SWTUtilities.spaceReclaimingSetVisible(m_licenseSection, false);
        }

        SWTUtilities.removeAllChildren(m_authorContentPane);
        if (!m_authorSection.isVisible()) { // is true for the initial display of the view after KAP launch
            SWTUtilities.spaceReclaimingSetVisible(m_authorSection, true);
        }
        mia = m_modelFacilitator.getAuthor();
        if (editMode || mia.hasContent()) {
            if (editMode) {
                mia.populateContainerForEdit(m_authorContentPane);
            } else {
                mia.populateContainerForDisplay(m_authorContentPane);
            }
        } else {
            // We currently *always* have an author - but in case we didn't, and didn't have anything else, still
            //      provide something to let the user know the panel is actually populated with some information.
            final Label l = new Label(m_authorContentPane, SWT.RIGHT);
            l.setFont(AUTHOR_FONT);
            l.setForeground(TEXT_COLOR);
            l.setText(NO_AUTHOR_TEXT);
        }

        layout(true, true);
    }

    private void performSave() {
        m_inEditMode.set(false);

        // we must commit prior to updating display, else atoms may not longer have their UI elements
        //      available to query
        m_modelFacilitator.commitEdit();

        performPostEditModeTransitionActions();

        final Job job = new WorkspaceJob("Saving workflow metadata...") {
            /**
             * {@inheritDoc}
             */
            @Override
            public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException {
                try {
                    m_modelFacilitator.writeMetadata(m_metadataFile);
                } catch (final IOException e) {
                    throw new CoreException(new Status(IStatus.ERROR, KNIMEEditorPlugin.PLUGIN_ID, -1,
                        "Failed to save metadata file.", e));
                }
                return Status.OK_STATUS;
            }
        };
        job.setRule(ResourcesPlugin.getWorkspace().getRoot());
        job.setUser(true);
        job.schedule();
    }

    private void performDiscard() {
        m_inEditMode.set(false);

        // must restore state before updating display to have a display synced to the restored model
        m_modelFacilitator.restoreState();

        performPostEditModeTransitionActions();
    }

    private void performPostEditModeTransitionActions() {
        updateDisplay();

        setHeaderBarButtons();

        m_tagAddTextField = null;
        m_tagsAddButton = null;

        m_linksAddURLTextField = null;
        m_linksAddTitleTextField = null;
        m_linksAddTypeTextField = null;
        m_linksAddButton = null;
    }

    private void setHeaderBarButtons() {
        SWTUtilities.removeAllChildren(m_headerButtonPane);

        if (m_inEditMode.get()) {
            FlatButton fb = new FlatButton(m_headerButtonPane, SWT.PUSH, SAVE_IMAGE, new Point(20, 20), true);
            fb.addClickListener((source) -> {
                performSave();
            });

            fb = new FlatButton(m_headerButtonPane, SWT.PUSH, CANCEL_IMAGE, new Point(20, 20), true);
            fb.addClickListener((source) -> {
                performDiscard();
            });

            m_headerBar.layout(true, true);
        } else {
            final Label l = new Label(m_headerButtonPane, SWT.LEFT);
            l.setLayoutData(new GridData(20, 20));

            final FlatButton fb = new FlatButton(m_headerButtonPane, SWT.PUSH, EDIT_IMAGE, new Point(20, 20), true);
            fb.addClickListener((source) -> {
                m_inEditMode.set(true);

                m_modelFacilitator.storeStateForEdit();

                updateDisplay();

                setHeaderBarButtons();
            });

            m_headerBar.layout(true, true);
        }
    }

    private void createTagsAddUI() {
        m_tagAddTextField = new Text(m_tagsAddContentPane, SWT.BORDER);
        m_tagAddTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent ke) {
                m_tagsAddButton.setEnabled(m_tagAddTextField.getText().length() > 0);

                if (ke.character == SWT.CR) {
                    processTagAdd();
                }
            }
        });
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        m_tagAddTextField.setLayoutData(gd);

        m_tagsAddButton = new Button(m_tagsAddContentPane, SWT.PUSH);
        m_tagsAddButton.setText("Add");
        m_tagsAddButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent se) {
                processTagAdd();
            }
        });
        gd = new GridData();
        gd.horizontalAlignment = SWT.RIGHT;
        m_tagsAddButton.setLayoutData(gd);
        m_tagsAddButton.setEnabled(false);
    }

    private void processTagAdd() {
        final MetaInfoAtom tag = m_modelFacilitator.addTag(m_tagAddTextField.getText());
        m_tagAddTextField.setText("");
        m_tagsAddButton.setEnabled(false);
        tag.populateContainerForEdit(m_tagsTagsContentPane);

        m_tagAddTextField.setFocus();

        layout(true, true);
    }

    private void createLinksAddUI() {
        m_linksAddURLTextField = addLabelTextFieldCouplet(m_linksAddContentPane, "URL:");
        m_linksAddURLTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent ke) {
                m_linksAddButton.setEnabled(m_linksAddURLTextField.getText().length() > 0);

                if ((ke.character == SWT.CR) && m_linksAddButton.isEnabled()) {
                    processLinkAdd();
                }
            }
        });
        m_linksAddTitleTextField = addLabelTextFieldCouplet(m_linksAddContentPane, "Title:");
        m_linksAddTitleTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent ke) {
                if ((ke.character == SWT.CR) && m_linksAddButton.isEnabled()) {
                    processLinkAdd();
                }
            }
        });
        m_linksAddTypeTextField = addLabelTextFieldCouplet(m_linksAddContentPane, "Type:");
        m_linksAddTypeTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent ke) {
                if ((ke.character == SWT.CR) && m_linksAddButton.isEnabled()) {
                    processLinkAdd();
                }
            }
        });

        m_linksAddButton = new Button(m_linksAddContentPane, SWT.PUSH);
        m_linksAddButton.setText("Add");
        m_linksAddButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent se) {
                processLinkAdd();
            }
        });
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.RIGHT;
        gd.horizontalSpan = 2;
        m_linksAddButton.setLayoutData(gd);
        m_linksAddButton.setEnabled(false);
    }

    private void processLinkAdd() {
        final String url = m_linksAddURLTextField.getText();

        String title = m_linksAddTitleTextField.getText();
        if (title.length() == 0) {
            title = url;
        }

        final String type = m_linksAddTypeTextField.getText();

        final MetaInfoAtom link = m_modelFacilitator.addLink(url, title, type);
        m_linksAddURLTextField.setText("");
        m_linksAddTitleTextField.setText("");
        m_linksAddTypeTextField.setText("");
        m_linksAddButton.setEnabled(false);
        link.populateContainerForEdit(m_linksLinksContentPane);

        m_linksAddURLTextField.setFocus();

        layout(true, true);
    }

    private Composite[] createVerticalSection(final String label, final Font labelFont) {
        final Composite[] sectionAndContentPane = new Composite[2];

        sectionAndContentPane[0] = new Composite(this, SWT.NONE);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        sectionAndContentPane[0].setLayoutData(gd);
        GridLayout gl = new GridLayout(1, false);
        gl.marginTop = 5;
        gl.marginBottom = 12;
        gl.marginWidth = 0;
        sectionAndContentPane[0].setLayout(gl);

        final Label l = new Label(sectionAndContentPane[0], SWT.LEFT);
        l.setText(label);
        l.setFont(labelFont);
        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        l.setLayoutData(gd);

        sectionAndContentPane[1] = new Composite(sectionAndContentPane[0], SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        sectionAndContentPane[1].setLayoutData(gd);
        gl = new GridLayout(1, false);
        gl.marginTop = 5;
        gl.marginBottom = 0;
        sectionAndContentPane[1].setLayout(gl);

        return sectionAndContentPane;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void metaInfoAtomDeleted(final MetaInfoAtom deletedAtom) {
        layout(true, true);
    }
}
