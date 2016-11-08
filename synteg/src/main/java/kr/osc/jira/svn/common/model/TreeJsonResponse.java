package kr.osc.jira.svn.common.model;

import java.util.List;

/**
 * @author Bong-Jin Kwon
 *
 */
public class TreeJsonResponse {

	private String text = "."; // maybe root
	private List<TreeNode> children;
	
	
	public TreeJsonResponse() {
		// TODO Auto-generated constructor stub
	}


	public String getText() {
		return text;
	}


	public void setText(String text) {
		this.text = text;
	}


	public List<TreeNode> getChildren() {
		return children;
	}


	public void setChildren(List<TreeNode> children) {
		this.children = children;
	}
	
	public void addChild(TreeNode child){
		this.children.add(child);
	}
	
	

}
