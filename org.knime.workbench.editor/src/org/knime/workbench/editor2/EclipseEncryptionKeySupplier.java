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
 *   Nov 3, 2006 (sieb): created
 */
package org.knime.workbench.editor2;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.knime.core.util.EncryptionKeySupplier;

/**
 * This class implements a decryption key supplier that is registered at the
 * {@link org.knime.core.util.KnimeEncryption} static class for encryption
 * purpose. The class asks the user to input a key to encrypt and decrypt
 * passwords, keys, etc. If a decryption should take place the key given by the
 * user must be the same that was used for encryption.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class EclipseEncryptionKeySupplier implements EncryptionKeySupplier {

    private String m_pw;

    /**
     * This method opens a window to which the user can input the encryption
     * key. The key is then returned to the invoker (which is normaly the
     * {@link org.knime.core.util.KnimeEncryption} static class).
     * 
     * @see org.knime.core.util.EncryptionKeySupplier#getEncryptionKey()
     */
    public String getEncryptionKey() {

        final KeyReaderShell keyReaderShell =
                new KeyReaderShell("KNIME encryption key",
                        "An encryption/decryption key is needed but "
                                + "not available for this session.\nPlease "
                                + "type in your encryption key.\nNote: "
                                + "Previously encrypted strings can only be "
                                + "decrypted with the former key.\n"
                                + "The key must be at least 8 "
                                + "characters long.");

        m_pw = keyReaderShell.readPW();

        return m_pw;
    }

    private final class KeyReaderShell {

        private String m_title;

        private String m_text;

        private boolean m_finished;

        private String m_key;

        private KeyReaderShell(final String title, final String text) {
            m_title = title;
            m_text = text;

        }

        private String readPW() {
            Display display;
            try {
                display = new Display();
            } catch (Throwable t) {
                display = Display.getDefault();
            }

            Shell shell = new Shell(display, SWT.ON_TOP | SWT.SHELL_TRIM);
            
            try {
                shell.setImage(ImageRepository.getImageDescriptor(
                        "icons/knime.bmp").createImage());
            } catch (Throwable e) {
                // do nothing, is just the icon
            }
            shell.setText(m_title);
            shell.setSize(300, 200);
            shell.forceActive();
            shell.forceFocus();

            GridLayout gridLayout = new GridLayout();

            gridLayout.numColumns = 2;

            shell.setLayout(gridLayout);

            GridData gridData = new GridData();
            gridData.horizontalAlignment = GridData.FILL;
            gridData.horizontalSpan = 2;

            final Label label = new Label(shell, SWT.NONE);
            label.setText(m_text + "\n\n");
            label.setBounds(20, 15, 380, 260);
            label.setLayoutData(gridData);

            gridData = new GridData();
            gridData.horizontalAlignment = GridData.FILL;
            gridData.horizontalSpan = 1;
            final Label keyLable = new Label(shell, SWT.NONE);
            keyLable.setText("Encryption key:");
            keyLable.setLayoutData(gridData);

            gridData = new GridData();
            gridData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
            gridData.horizontalSpan = 1;
            gridData.widthHint = 280;
            final Text text = new Text(shell, SWT.PASSWORD | SWT.BORDER);
            // text.setBounds(140, 270, 200, 20);
            text.setSize(200, 20);
            text.setLayoutData(gridData);

            gridData = new GridData();
            gridData.horizontalAlignment = GridData.FILL;
            gridData.horizontalSpan = 2;
            final Label spaceLabel = new Label(shell, SWT.NONE);
            spaceLabel.setText("\n");
            spaceLabel.setLayoutData(gridData);

            gridData = new GridData();
            gridData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_END;
            gridData.widthHint = 80;
            gridData.horizontalSpan = 1;
            final Button button = new Button(shell, SWT.PUSH);
            button.setText("OK");
            // button.setBounds(20, 270, 180, 20);
            button.setSize(180, 20);
            button.setLayoutData(gridData);

            gridData = new GridData();
            gridData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_END;
            gridData.widthHint = 80;
            gridData.horizontalSpan = 1;
            final Button cancel = new Button(shell, SWT.PUSH);
            cancel.setText("Cancel");
            // cancel.setBounds(20, 270, 180, 20);
            cancel.setSize(180, 20);
            cancel.setLayoutData(gridData);

            gridData = new GridData();
            gridData.horizontalAlignment = GridData.FILL;
            gridData.horizontalSpan = 2;

            final Label info = new Label(shell, SWT.NONE);
            info.setText("");
            // label.setBounds(20, 15, 380, 260);
            info.setLayoutData(gridData);

            Composite parent = shell.getParent();
            if (parent != null) {
                shell.setBounds(parent.getBounds().width / 2, parent
                        .getBounds().height / 2, 400, 200);
            } else {
                shell.setBounds(display.getBounds().width / 2, display
                        .getBounds().height / 2, 400, 200);
            }

            shell.open();

            text.setFocus();

            // Listener listener = new Listener() {
            // public void handleEvent(final Event event) {
            // if (event.widget == buttonOK) {
            //
            // } else {
            //
            // }
            // dialog.close();
            // }
            // };

            final Listener buttonListener = new Listener() {
                public void handleEvent(final Event event) {

                    // check if the key is valid
                    if (text.getText().length() < 8) {
                        info.setText("Key must be at least 8 characters long.");
                        return;
                    }
                    if (text.getText().indexOf(" ") >= 0) {
                        info.setText("Key must not contain a blank.");
                        return;
                    }

                    m_key = text.getText();
                    m_finished = true;
                }

            };

            button.addListener(SWT.Selection, buttonListener);

            KeyListener keyListener = new KeyListener() {

                public void keyPressed(final KeyEvent e) {

                    if (e.character == '\r') {
                        buttonListener.handleEvent(null);
                    }

                }

                public void keyReleased(final KeyEvent e) {
                    // do nothing

                }
            };

            text.addKeyListener(keyListener);
            shell.addKeyListener(keyListener);

            Listener cancelListener = new Listener() {
                public void handleEvent(final Event event) {

                    m_finished = true;
                }

            };

            cancel.addListener(SWT.Selection, cancelListener);

            while (!shell.isDisposed() && !m_finished) {
                if (!display.readAndDispatch())
                    display.sleep();
            }
            shell.close();
            shell.dispose();

            return m_key;
        }
    }

    public static void main(String[] args) {
        EclipseEncryptionKeySupplier supplier =
                new EclipseEncryptionKeySupplier();
        String key = supplier.getEncryptionKey();
        System.out.println("Key: " + key);
    }
}