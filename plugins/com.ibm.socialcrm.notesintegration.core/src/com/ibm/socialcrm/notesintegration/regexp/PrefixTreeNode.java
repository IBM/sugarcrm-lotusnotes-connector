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
 * This class serves as a node in a prefix tree that tracks the current key and if it marks he end of a word or the top level node.  
 * A ConcurrentHashMap is used internally to prevent exceptions from collapsing the nodes in the tree.  
 */
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PrefixTreeNode {
	private ConcurrentHashMap<String,PrefixTreeNode> children;
	private boolean wordEnd;
	private boolean top;
	private String nodeKey;
	
	public PrefixTreeNode() {
		wordEnd = false;
		nodeKey = "";
		top = false;
		children = new ConcurrentHashMap<String,PrefixTreeNode>();
	}
	public PrefixTreeNode(String aKey) {
		this();
		nodeKey = aKey;
		//children.put(aKey,new PrefixTreeNode());
	}
	public PrefixTreeNode(String aKey, boolean endOfWord) {
		this(aKey);
		wordEnd = endOfWord; 
	}
	public PrefixTreeNode(boolean topFlag) {
		this();
		top = topFlag;
	}
	public boolean isTop() {
		return top;
	}
	public boolean isWordEnd() {
		return wordEnd;
	}
	public void setWordEnd(boolean endOfWord) {
		wordEnd = endOfWord;
	}
	public ConcurrentHashMap<String,PrefixTreeNode> getChildren() {
		return children;
	}
	public ConcurrentHashMap<String,PrefixTreeNode> setChildren(ConcurrentHashMap<String,PrefixTreeNode> aHashMap) {
		return children = aHashMap;
	}
	public boolean hasChild(String aKey) {
		return children.containsKey(aKey);
	}
	public PrefixTreeNode getChild(String aKey) {
		return children.get(aKey);
	}
	public String getNodeKey() {
		return nodeKey;
	}
	public void setNodeKey(String nodeKey) {
		this.nodeKey = nodeKey;
	}
	public Set<String> getChildKeys() {
		return children.keySet();
	}
	public PrefixTreeNode removeChild(String aKey) {
		return children.remove(aKey);
	}
	/**
	 * Adds a string as a child of the current tree node.  If the child exists, then it is used and the flag for word end is updated. 
	 * @param aKey The string to add as a child.  It is expected this is a single "character" during the build phase.
	 * @param endOfWord Flag indicating if this string marks the end of a word
	 */
	public PrefixTreeNode addChild(String aKey, boolean endOfWord) {
		PrefixTreeNode aChild;
		if (hasChild(aKey)) {
			//have the child already?, OR in end of word flag
			aChild = getChild(aKey);
			boolean flag = aChild.isWordEnd() || endOfWord;
			aChild.setWordEnd(flag);
		} else {
			//add a new child
			aChild = new PrefixTreeNode(aKey,endOfWord);
			this.getChildren().put(aKey, aChild);
		}
		return aChild;
	}
	/**
	 * Provides a custom display of a PrefixTreeNode, including it's immediate children 
	 */
	public String toString() {
		String output = "PTN:" + nodeKey + " wordEnd:" + this.isWordEnd() + " isTop:" + this.isTop();
		output += " {" + getChildren().keySet() +"}";
		return output;
	}
}
