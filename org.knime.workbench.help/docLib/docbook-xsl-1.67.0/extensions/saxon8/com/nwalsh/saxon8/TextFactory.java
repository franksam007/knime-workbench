// TextFactory - Saxon extension element factory

package com.nwalsh.saxon8;

import net.sf.saxon.style.ExtensionElementFactory;
import org.xml.sax.SAXException;

/**
 * <p>Saxon extension element factory
 *
 * <p>$Id: TextFactory.java 173 2006-02-14 16:13:53Z ohl $</p>
 *
 * <p>Copyright (C) 2000 Norman Walsh.</p>
 *
 * <p>This class provides a
 * <a href="http://users.iclway.co.uk/mhkay/saxon/">Saxon</a>
 * extension element factory for the Text extension element
 * family.</p>
 *
 * <p><b>Change Log:</b></p>
 * <dl>
 * <dt>1.0</dt>
 * <dd><p>Initial release.</p></dd>
 * </dl>
 *
 * @author Norman Walsh
 * <a href="mailto:ndw@nwalsh.com">ndw@nwalsh.com</a>
 *
 * @version $Id: TextFactory.java 173 2006-02-14 16:13:53Z ohl $
 *
 * @see Text
 *
 */
public class TextFactory implements ExtensionElementFactory {
  /**
   * <p>Constructor for TextFactory</p>
   *
   * <p>Does nothing.</p>
   */
  public TextFactory() {
  }

  /**
   * <p>Return the class that implements a particular extension element.</p>
   *
   * @param localname The local name of the extension element.
   *
   * @return The class that handles that extension element.
   *
   * @exception SAXException("Unknown Text extension element")
   */
  public Class getExtensionClass(String localname) {
    if (localname.equals("insertfile")) {
      try {
	return Class.forName("com.nwalsh.saxon8.Text");
      } catch (ClassNotFoundException e) {
	return null;
      }
    }
    return null;
  }
}




