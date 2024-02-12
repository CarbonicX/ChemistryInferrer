package com.carbonicx.chemistryinferrer;

// 反应类型
public enum NodeRelationType {
	// 正向，如节点 N 之于 A N >> B C
	TOWARD, 
	// 反向，如节点 N 之于 A B >> N C
	BACKWARD
}
