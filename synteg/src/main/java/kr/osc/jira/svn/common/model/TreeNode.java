package kr.osc.jira.svn.common.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Bong-Jin Kwon
 *
 */
public class TreeNode extends HashMap<String, Object> {

	private static final long serialVersionUID = 1L;

	private static final String KEY_CHILDREN = "children";

	/**
	 * 
	 */
	public TreeNode() {
		put(KEY_CHILDREN, new ArrayList<TreeNode>());
	}

	/**
	 * @param initialCapacity
	 */
	public TreeNode(int initialCapacity) {
		super(initialCapacity);
		put(KEY_CHILDREN, new ArrayList<TreeNode>());
	}

	/**
	 * @param m
	 */
	public TreeNode(TreeNode m) {
		super(m);
	}

	/**
	 * @param initialCapacity
	 * @param loadFactor
	 */
	public TreeNode(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		put(KEY_CHILDREN, new ArrayList<TreeNode>());
	}

	@SuppressWarnings("unchecked")
	public void addChild(TreeNode node) {
		List<TreeNode> children = (List<TreeNode>) get(KEY_CHILDREN);

		children.add(node);
	}

}
