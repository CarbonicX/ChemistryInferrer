package com.carbonicx.chemistryinferrer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 节点
public class Node {
	// 节点名
	public String name;
	// 可能的物质
	public Set<Substance> possibles = new HashSet<>();
	// 是否尚未确定可能的物质
	// 在初始化 Node 时，如果为其添加了 possibles 中的物质，则应该将 possiblesNotDetermined 设置为 false
	public boolean possiblesNotDetermined = true;
	// 与其他节点的关系
	public Set<NodeRelation> nodeRelations = new HashSet<>();
	// 限定器
	public Set<String> limiters = new HashSet<>();
	// 按最后一次返回顺序储存节点
	private static List<Node> returnSequence = new ArrayList<>();
	
	private static int debugTabCount = 0;
	
	
	// 以初始节点启动
	public void startAsInitialNode() throws Exception {
		Program.debugln("[Node.startAsInitialNode] 节点 " + this.name + " 作为初始节点启动");
		
		if (this.possibles.size() == 0 && this.limiters.size() != 0) {
			this.possibles = new HashSet<>(Program.substances.values());
			for (String l : this.limiters) {
				this.possibles.retainAll(Program.limiters.get(l));
			}
			this.debugThis('>');
		} else if (this.possibles.size() == 0 && this.limiters.size() == 0) {
			this.possibles = new HashSet<>(Program.substances.values());
			this.debugThis('>');
		}
		if (this.possibles.size() == 0) throw new Exception("无法推断节点 " + this.name);
		
		// 判断相邻节点是否已经确定了具体物质
		boolean notAllDetermined = false;
		for (NodeRelation relation : nodeRelations) {
			for (Node node : relation.with) {
				if (node.possibles.size() != 1) notAllDetermined = true;
			}
			for (Node node : relation.to) {
				if (node.possibles.size() != 1) notAllDetermined = true;
			}
		}
		if (!notAllDetermined) {
			Program.debugln("[Node.startAsInitialNode] 初始节点 " + this.name + 
					" 的所有相邻节点都已经确定了具体物质。初始节点返回");
			return;
		}
		
		// 更新相邻节点
		this.operateAdjacentNodes((Node node) -> {
			Program.debugln("初始节点 " + this.name + " 触发节点 " + node.name + " 更新");
			node.update(this);
		});
		
		Program.debugln("[Node.startAsInitialNode] 以 " + this.name + " 为初始节点的递归结束");
		
		// 按照节点返回顺序从后到前依次更新自己
		this.addToReturnList();
		for (Node node : Node.returnSequence) {
			Program.debugln("[Node.startAsInitialNode] 初始节点 " + this.name + 
					" 要求 " + node.name + " 进行收尾更新");
			Set<Substance> newPossibles = node.measure();
			if (newPossibles.size() == 0) throw new Exception("无法推断节点 " + node.name);
			node.tryRemoveConstant();
			node.possibles = newPossibles;
		}
	}
	
	// 更新自己
	private void update(Node sender) throws Exception {
		Node.debugTabCount++;
		if (this.possibles.size() == 1) {
			Node.debug("节点 " + this.name + " 已经确定了具体物质");
			this.operateAdjacentNodes((Node node) -> {
				if (node != sender && node.possibles.size() != 1) {
					Node.debug(" - 节点 " + node.name + " 尚未确定具体物质。触发其更新");
					node.update(this);
				}
			});
			Node.debug("节点 " + this.name + " 返回");
			this.addToReturnList();
			Node.debugTabCount--;
			return;
		}
		
		Set<Substance> newPossibles = this.measure();
		if (newPossibles.size() == 0) {
			throw new Exception("无法推断节点 " + this.name);
		}
		if (this.possibles.equals(newPossibles)) {
			Node.debug("节点 " + this.name + " 更新前后未发生变化。节点返回");
			this.addToReturnList();
			Node.debugTabCount--;
			return;
		}
		this.possibles = newPossibles;
		// 两个不同节点不能是同一个物质
		this.tryRemoveConstant();
		this.debugThis('>');
		

		Node.debug("节点 " + this.name + " 的更新结束");
		// 更新相邻节点
		this.operateAdjacentNodes((Node node) -> {
			if (node != sender) {
				Node.debug(" + 节点 " + this.name + " 触发节点 " + node.name + " 更新");
				node.update(this);
			}
		});

		this.addToReturnList();
		Node.debug("节点 " + this.name + " 返回");
		Node.debugTabCount--;
	}
	
	// 判断自己
	private Set<Substance> measure() throws Exception {
		if (this.possibles.size() == 1) {
			return this.possibles;
		}
		
		this.operateAdjacentNodes((Node node) -> {
			if (node.possibles.size() == 0 && node.limiters.size() != 0) node.updateBasedOnLimiter();
		});
		
		Set<Substance> newPossibles = new HashSet<>(this.possibles);
		
		for (NodeRelation relation : nodeRelations) {
			for (Node node : relation.to) {
				if (node.possibles.size() == 0) continue;
				if (this.possiblesNotDetermined) {
					newPossibles = Substance.diverge(
						node.possibles, relation.type, NodeRelationForm.TO, relation.condition, relation.forceConditions, relation.categories
					);
					this.possiblesNotDetermined = false;
				} else {
					newPossibles.retainAll(Substance.diverge(
						node.possibles, relation.type, NodeRelationForm.TO, relation.condition, relation.forceConditions, relation.categories
					));
				}
			}
			for (Node node : relation.with) {
				if (node.possibles.size() == 0) continue;
				if (this.possiblesNotDetermined) {
					newPossibles = Substance.diverge(
						node.possibles, relation.type, NodeRelationForm.WITH, relation.condition, relation.forceConditions, relation.categories
					);
					this.possiblesNotDetermined = false;
				} else {
					newPossibles.retainAll(Substance.diverge(
						node.possibles, relation.type, NodeRelationForm.WITH, relation.condition, relation.forceConditions, relation.categories
					));
				}
			}
		}
		
		// 根据限定器限定
		for (String l : this.limiters) {
			newPossibles.retainAll(Program.limiters.get(l));
		}
		
		return newPossibles;
	}
	
	// 根据限定器更新所有可能的物质，只在需要时使用
	private void updateBasedOnLimiter() {
		this.possibles = new HashSet<>(Program.substances.values());
		for (String l : this.limiters) {
			this.possibles.retainAll(Program.limiters.get(l));
		}
	}
	
	private void tryRemoveConstant() {
		if (this.possibles.size() != 1) return;
		Substance substance = (Substance)this.possibles.toArray()[0];
		for (Node node : Program.nodes.values()) {
			if (node != this && node.possibles.contains(substance)) {
				Node.debug(" - （更新）节点 " + node.name + " 移除物质 " + substance.name + 
						"，因为节点 " + this.name + " 已经确定了该物质");
				node.possibles.remove(substance);
				node.debugThis('-');
			}
		}
	}
	
	// 操作所有相邻节点
	private void operateAdjacentNodes(NodeOperator action) throws Exception {
		for (NodeRelation relation : nodeRelations) {
			for (Node node : relation.to) {
				action.accept(node);
			}
			for (Node node : relation.with) {
				action.accept(node);
			}
		}
	}
	
	// 将自己添加到返回顺序列表中
	private void addToReturnList() {
		if (Node.returnSequence.size() != 0 && 
				Node.returnSequence.get(Node.returnSequence.size() - 1) == this) {
			return;
		}
		Node.returnSequence.add(this);
	}
	
	private static void debug(String text) {
		for (int i = 0; i < Node.debugTabCount; i++) {
			Program.debug("\t");
		}
		Program.debugln(text);
	}
	
	private void debugThis(char c) {
		StringBuilder string = new StringBuilder();
		string.append(" " + c + " (更新) 节点 " + this.name + " 可能的物质：");
		for (Substance substance : this.possibles) {
			string.append(substance.name + " ");
		}
		Node.debug(string.toString());
		string.setLength(0);
		string.append(" " + c + " (更新) 节点 " + this.name + " 限定器：");
		for (String limiter : this.limiters) {
			string.append("[" + limiter + "] ");
		}
		Node.debug(string.toString());
	}
}

@FunctionalInterface
interface NodeOperator {
	void accept(Node node) throws Exception;
}
