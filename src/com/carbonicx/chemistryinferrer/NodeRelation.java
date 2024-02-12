package com.carbonicx.chemistryinferrer;

import java.util.Set;

// 节点关系，用于在 Node 中储存相互关系
// 物质之间的反应储存在具体的 Substance 中
public class NodeRelation {
	// 反应类型
	public NodeRelationType type;
	// 反应生成或由其反应得来的节点
	public Set<Node> to;
	// 与之共同的节点
	public Set<Node> with;
	// 反应条件
	public Set<String> condition;
	public boolean forceConditions = false;
	// 反应类别
	public Set<String> categories;
	
	public NodeRelation(NodeRelationType type, Set<Node> to, Set<Node> with, Set<String> condition, 
			boolean forceConditions, Set<String> categories) {
		this.type = type;
		this.to = to;
		this.with = with;
		this.condition = condition;
		this.forceConditions = forceConditions;
		this.categories = categories;
	}
}
