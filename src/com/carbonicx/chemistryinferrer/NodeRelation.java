package com.carbonicx.chemistryinferrer;

import java.util.Set;

// �ڵ��ϵ�������� Node �д����໥��ϵ
// ����֮��ķ�Ӧ�����ھ���� Substance ��
public class NodeRelation {
	// ��Ӧ����
	public NodeRelationType type;
	// ��Ӧ���ɻ����䷴Ӧ�����Ľڵ�
	public Set<Node> to;
	// ��֮��ͬ�Ľڵ�
	public Set<Node> with;
	// ��Ӧ����
	public Set<String> condition;
	public boolean forceConditions = false;
	// ��Ӧ���
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
