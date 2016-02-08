package jserver;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class contains methods to read and write code snippets to a XML file. 
 * The default file name is codes.xml and can be changed by calling setXmlFile.
 * Additionally, the class contains some methods for reading the predefined color names.
 * 
 * @author Euler
 *
 */
public class CodeDB {
	private Document document;
	private Element lastEditElement;
	private File xmlFile = new File("codes.xml");
	private List<Color> colors = new ArrayList<Color>();
	private Map<String, Integer> colorValues = new HashMap<String, Integer>();
	private IOException lastException;

	public File getXmlFile() {
		return xmlFile;
	}

	public void setXmlFile(File xmlFile) {
		this.xmlFile = xmlFile;
	}

	void readXML() throws ParserConfigurationException, SAXException,
			IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		builder = factory.newDocumentBuilder();
		document = builder.parse(xmlFile);
		setLastEditElement((Element) document.getElementsByTagName("autosave")
				.item(0));
	}

	void writeXML() {
		Transformer tf;
		try {
			tf = TransformerFactory.newInstance().newTransformer();
			tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			tf.setOutputProperty(OutputKeys.INDENT, "yes");
			tf.transform(new DOMSource(document), new StreamResult(xmlFile));
		} catch (TransformerFactoryConfigurationError | TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void saveAsSnippet(String name, String code, String authorName) {
		Element snip = document.createElement("snippet");
		snip.setAttribute("name", name);
		Element created = document.createElement("created");
		created.setTextContent(Calendar.getInstance().getTime().toString());
		Element c2 = document.createElement("codeA");
		CDATASection cdata = document.createCDATASection("\n" + code + "\n");
		Element author = document.createElement("author");
		author.setTextContent(authorName);

		c2.appendChild(cdata);
		snip.appendChild(author);
		snip.appendChild(created);
		snip.appendChild(c2);

		Element root = (Element) document.getElementsByTagName("codes").item(0);
		root.appendChild(snip);
		writeXML();
	}

	public void overwriteSnippet(String snippetName, String code) {
		Element s = getSnippetByName(snippetName);
		Node updated = null;

		NodeList children = s.getChildNodes();
		for (int j = 0; j < children.getLength(); j++) {
			Node child = children.item(j);
			if ("codeA".equals(child.getNodeName())) {
				CDATASection section = (CDATASection) child.getLastChild();
				section.replaceWholeText(code);
			}
			if ("code".equals(child.getNodeName())) {
				child.setTextContent(code);
			}
			if ("updated".equals(child.getNodeName())) {
				updated = child;
			}
		}

		if (updated == null) {
			updated = document.createElement("updated");
			s.appendChild(updated);
		}
		updated.setTextContent(Calendar.getInstance().getTime().toString());

		writeXML();
		return;

	}

	Element getSnippetByName(String snippetName) {
		NodeList snippets = document.getElementsByTagName("snippet");
		for (int i = 0; i < snippets.getLength(); i++) {
			Element s = (Element) snippets.item(i);
			if (snippetName.equals(s.getAttribute("name"))) {
				return s;
			}
		}
		return null;
	}

	public void convertOldCode() {
		NodeList snippets = document.getElementsByTagName("snippet");
		for (int i = 0; i < snippets.getLength(); i++) {
			Element s = (Element) snippets.item(i);
			System.out.println(s.getAttribute("name"));
			NodeList children = s.getChildNodes();
			for (int j = 0; j < children.getLength(); j++) {
				Node c = children.item(j);
				if ("code".equals(c.getNodeName())) {
					System.out.println("   -> old");
					Element c2 = document.createElement("codeA");
					CDATASection cdata = document.createCDATASection("\n"
							+ c.getTextContent() + "\n");
					c2.appendChild(cdata);
					s.appendChild(c2);
					s.removeChild(c);
				}
			}
		}

	}

	public boolean hasSnippet(String name) {
		return getSnippetByName(name) != null;
	}

	public String getSnippetCode(String name) {
		Element s = getSnippetByName(name);
		if (s == null) {
			return null;
		}
		NodeList children = s.getChildNodes();
		for (int j = 0; j < children.getLength(); j++) {
			Node c = children.item(j);
			if ("codeA".equals(c.getNodeName())) {
				Node n = c.getLastChild();
				System.out.println("Node type: " + n.getNodeType() + " "
						+ n.getNodeName());
				CDATASection section = (CDATASection) c.getLastChild();
				return section.getTextContent();
				// geht auch
				// return n.getNodeValue();
			}
			if ("code".equals(c.getNodeName())) {
				return c.getTextContent();
			}
		}
		return null;
	}

	public void saveAsLast(String code) {
		getLastEditElement().setTextContent(code);
		writeXML();
	}

	public List<String> getSnippetNames() {
		List<String> names = new ArrayList<String>();

		NodeList snippets = document.getElementsByTagName("snippet");
		for (int i = 0; i < snippets.getLength(); i++) {
			Element s = (Element) snippets.item(i);
			names.add(s.getAttribute("name"));
		}

		return names;
	}

	/**
	 * Read the color names from file colors.h
	 * 
	 * @return a list of color names. The list is empty if the file does not
	 *         exists or does not contain required format
	 */
	public List<String> getColorNames() {
		List<String> list = new ArrayList<String>();

		try {
			BufferedReader reader = new BufferedReader(new FileReader(
					"colors.h"));
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}
				String[] parts = line.split(" +");
				list.add(parts[1]);
				int value = Board.parseColor(parts[2].trim());
				colors.add(new Color(value));
				colorValues.put(parts[1], value);
			}
			reader.close();
		} catch (IOException e) {
			// e.printStackTrace();
			lastException = e;
			return null;
		}

		return list;
	}

	public Integer getColorValue(String s) {
		return colorValues.get(s);
	}

	public List<Color> getColors() {
		return colors;
	}

	public IOException getLastException() {
		return lastException;
	}

	public Element getLastEditElement() {
		return lastEditElement;
	}

	public void setLastEditElement(Element lastEditElement) {
		this.lastEditElement = lastEditElement;
	}


}
