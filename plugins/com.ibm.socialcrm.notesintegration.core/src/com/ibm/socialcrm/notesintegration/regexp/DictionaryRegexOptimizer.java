package com.ibm.socialcrm.notesintegration.regexp;
/****************************************************************
 * IBM Confidential
 *
 * SFA050-Collaboration Source Materials
 *
 * (C) Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has been
 * deposited with the U.S. Copyright Office
 *
 ***************************************************************/
/**
 * This class produces an optimized regular expression matching a dictionary of words.  Each entry may consist of multiple words,
 * and may also include Unicode characters.  The words are added a "character" at a time, and the tree is collapsed into the fewest
 * possible nodes before the regular expression is output.
 */
import java.util.ArrayList;
import java.util.Iterator;

import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;

public class DictionaryRegexOptimizer {
	private PrefixTreeNode prefixTree;

	private PrefixTreeNode getPrefixTree() {
		return prefixTree;
	}
	private void setPrefixTree(PrefixTreeNode prefixTree) {
		this.prefixTree = prefixTree;
	}
	public DictionaryRegexOptimizer() {
		setPrefixTree(new PrefixTreeNode(true));
	}
	public DictionaryRegexOptimizer(ArrayList<String> words) {
		this();
		for (int i = 0; i < words.size(); i++) {
			this.addEntry(words.get(i));
		}
	}
	/**
	 * Adds a string to the dictionary to be optimized. The string is lowercased, broken into "characters" and traversed down the tree, adding nodes as needed.
	 * 
	 * @param aMixedCaseString
	 *        The string to add to the dictionary. This may contain multiple words and extended characters (ie names from all over the world).
	 */
	public void addEntry(String aMixedCaseString) {
		int i;
		String aChar;
		String aString = aMixedCaseString.toLowerCase(); // we process the regex with the case insensitive flag, so ensure we don't duplicate nodes unnecessarily
		PrefixTreeNode topLevel = getPrefixTree();
		PrefixTreeNode currentLevel = topLevel;
		PrefixTreeNode parentLevel;
		for (i = 0; i < aString.length(); i++) {
			aChar = aString.substring(i, i + 1); // parse off a string length 1 since these will have unicode and some char methods can have issues
			if (currentLevel.hasChild(aChar)) {
				parentLevel = currentLevel;
				currentLevel = currentLevel.getChild(aChar); // go down a level
				if (i == aString.length() - 1) {
					// this is the end of the word;
					currentLevel.setWordEnd(true);
				}
			} else {
				// add a child
				PrefixTreeNode newChild;
				if (i == aString.length() - 1) {
					// this is the end of the word;
					newChild = currentLevel.addChild(aChar, true);
				} else {
					newChild = currentLevel.addChild(aChar, false);
				}
				parentLevel = currentLevel;
				currentLevel = newChild;
			}
		}
	}
	/**
	 * Returns the optimized regular expression generated from the prefix tree. The tree is collapsed, then the regex is constructed from the remaining nodes
	 * 
	 * @return String The regular expression
	 */
	public String getRegex() {
		// make sure the prefixes are collapsed before turning them into a regex
		collapseChildNodes(getPrefixTree());

		// 86950
		// String regex = "(?-xism:((?<!\\S)|\\b)(?ui:"; //negative look behind, non-whitespace word boundary. Unicode case insensitive
		// regex = appendChildToRegex(regex,getPrefixTree()) + "))";
		String regex = ConstantStrings.EMPTY_STRING;
		String regex1 = ConstantStrings.EMPTY_STRING;
		if (getPrefixTree() != null && getPrefixTree().getChildren() != null && getPrefixTree().getChildren().size() > 0 && getPrefixTree().isTop()) {
			regex = "(?-xism:((?<!\\S)|\\b)(?ui:"; //negative look behind, non-whitespace word boundary.  Unicode case insensitive  //$NON-NLS-1$
			regex1 = "))"; //$NON-NLS-1$
		}
		regex = appendChildToRegex(regex, getPrefixTree()) + regex1;
		// System.out.println("\nregex:" + regex);
		
		return regex;
	}
	/**
	 * Recursive method to append the current node to the regular expression being built. The tree is traversed downward and match groups are built from siblings at each level.
	 * 
	 * @param currentRegex
	 *        A string containing the regular expression prefix that has been built so far
	 * @param aChild
	 *        The PrefixTreeNode representing the current level to process
	 * @return String The regular expression constructed from this node
	 */
	private String appendChildToRegex(String currentRegex, PrefixTreeNode aChild) {
		String updatedRegex = currentRegex;
		String nodeKey = aChild.getNodeKey();
		if ((nodeKey != null && !nodeKey.equals("")) || aChild.isTop()) {
			if (!aChild.isTop()) {
				updatedRegex += "(?:" + preg_quote(nodeKey); // add the current node in a new group
			}
			boolean multiChildren = aChild.getChildKeys().size() > 0;
			if (multiChildren && !aChild.isTop()) {
				updatedRegex += "(?:"; // if multi-children, start a new match group where the siblings are ORed
			}
			Iterator<String> i = aChild.getChildKeys().iterator();
			boolean firstTime = true;
			while (i.hasNext()) {
				if (!firstTime) {
					updatedRegex += "|"; // add the OR
				}
				PrefixTreeNode node = aChild.getChild(i.next());
				updatedRegex = appendChildToRegex(updatedRegex, node);
				firstTime = false;
			}
			if (multiChildren && !aChild.isTop()) {
				updatedRegex += ")";
				if (aChild.isWordEnd()) {
					// tack on a * for "0 or more" matching of the child group, since they are optional
					updatedRegex += "*";
				}
			}
			if (!aChild.isTop()) {
				updatedRegex += ")"; // close the group
			}

		}
		return updatedRegex;
	}
	/**
	 * Recursive method to collapse the children of the current PrefixTreeNode. This ensures we have the longest prefixes of words to OR together in the resulting regular expression aNode The
	 * PrefixTreeNode to collapse the children of
	 * 
	 * @return String The regular expression
	 */
	private String collapseChildNodes(PrefixTreeNode aNode) {
		String aKey;
		String nodeSuffix, newKey;
		PrefixTreeNode aChildNode, aGrandChildNode;
		if (aNode.getChildKeys().isEmpty()) {
			// nothing to do. collapse with nothing added
			return null;
		} else {
			// iterate over the keys traversing down
			Iterator<String> i = aNode.getChildKeys().iterator();
			// iterate over each node's keys
			while (i.hasNext()) {
				aKey = i.next();
				nodeSuffix = collapseChildNodes(aNode.getChild(aKey));
				if (nodeSuffix != null) {
					// append lower suffix to current key and move the grandchild up
					aChildNode = aNode.getChild(aKey);
					aGrandChildNode = aChildNode.getChild(nodeSuffix);
					newKey = aKey + nodeSuffix;
					aGrandChildNode.setNodeKey(newKey);
					aNode.getChildren().put(newKey, aGrandChildNode);
					aNode.getChildren().remove(aKey);
				}
			}
			// deal with the current level
			if (!aNode.isWordEnd() && aNode.getChildKeys().size() == 1 && !aNode.isTop()) {
				// return the key string for the level above to collapse
				aKey = aNode.getChildKeys().iterator().next();
				return aKey;
			}
			return null; // there are multiple siblings, so nothing to collapse
		}
	}
	/**
	 * Method that escapes the special characters in a regular expression as does PHP's preg_quote from the remaining nodes
	 * 
	 * @return String The regular expression
	 */
	private String preg_quote(String aString) {
		return aString.replaceAll("[.\\\\+*?\\[\\^\\]$(){}=!<>|:\\-]", "\\\\$0");
	}
	/**
	 * Method that escapes the special characters in a regular expression as does PHP's preg_quote from the remaining nodes
	 * 
	 * @return String The regular expression
	 */
	private String escapeRegexChars(String input) {
		// escape these chars like php preg_quote: . \ + * ? [ ^ ] $ ( ) { } = ! < > | : -
		String out = input.replace("\\", "\\\\"); // do the backslashes first
		out = out.replace(".", "\\.");
		out = out.replace("+", "\\+");
		out = out.replace("*", "\\*");
		out = out.replace("?", "\\?");
		out = out.replace("[", "\\[");
		out = out.replace("^", "\\^");
		out = out.replace("]", "\\]");
		out = out.replace("$", "\\$");
		out = out.replace("(", "\\(");
		out = out.replace(")", "\\)");
		out = out.replace("{", "\\{");
		out = out.replace("}", "\\}");
		out = out.replace("=", "\\=");
		out = out.replace("!", "\\!");
		out = out.replace("<", "\\<");
		out = out.replace(">", "\\>");
		out = out.replace("|", "\\|");
		out = out.replace(":", "\\:");
		out = out.replace("-", "\\-");
		return out;
	}
}
