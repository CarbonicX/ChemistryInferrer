package com.carbonicx.chemistryinferrer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// �ڵ�
public class Node {
	// �ڵ���
	public String name;
	// ���ܵ�����
	public Set<Substance> possibles = new HashSet<>();
	// �Ƿ���δȷ�����ܵ�����
	// �ڳ�ʼ�� Node ʱ�����Ϊ������� possibles �е����ʣ���Ӧ�ý� possiblesNotDetermined ����Ϊ false
	public boolean possiblesNotDetermined = true;
	// �������ڵ�Ĺ�ϵ
	public Set<NodeRelation> nodeRelations = new HashSet<>();
	// �޶���
	public Set<String> limiters = new HashSet<>();
	// �����һ�η���˳�򴢴�ڵ�
	private static List<Node> returnSequence = new ArrayList<>();
	
	private static int debugTabCount = 0;
	
	
	// �Գ�ʼ�ڵ�����
	public void startAsInitialNode() throws Exception {
		Program.debugln("[Node.startAsInitialNode] �ڵ� " + this.name + " ��Ϊ��ʼ�ڵ�����");
		
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
		if (this.possibles.size() == 0) throw new Exception("�޷��ƶϽڵ� " + this.name);
		
		// �ж����ڽڵ��Ƿ��Ѿ�ȷ���˾�������
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
			Program.debugln("[Node.startAsInitialNode] ��ʼ�ڵ� " + this.name + 
					" ���������ڽڵ㶼�Ѿ�ȷ���˾������ʡ���ʼ�ڵ㷵��");
			return;
		}
		
		// �������ڽڵ�
		this.operateAdjacentNodes((Node node) -> {
			Program.debugln("��ʼ�ڵ� " + this.name + " �����ڵ� " + node.name + " ����");
			node.update(this);
		});
		
		Program.debugln("[Node.startAsInitialNode] �� " + this.name + " Ϊ��ʼ�ڵ�ĵݹ����");
		
		// ���սڵ㷵��˳��Ӻ�ǰ���θ����Լ�
		this.addToReturnList();
		for (Node node : Node.returnSequence) {
			Program.debugln("[Node.startAsInitialNode] ��ʼ�ڵ� " + this.name + 
					" Ҫ�� " + node.name + " ������β����");
			Set<Substance> newPossibles = node.measure();
			if (newPossibles.size() == 0) throw new Exception("�޷��ƶϽڵ� " + node.name);
			node.tryRemoveConstant();
			node.possibles = newPossibles;
		}
	}
	
	// �����Լ�
	private void update(Node sender) throws Exception {
		Node.debugTabCount++;
		if (this.possibles.size() == 1) {
			Node.debug("�ڵ� " + this.name + " �Ѿ�ȷ���˾�������");
			this.operateAdjacentNodes((Node node) -> {
				if (node != sender && node.possibles.size() != 1) {
					Node.debug(" - �ڵ� " + node.name + " ��δȷ���������ʡ����������");
					node.update(this);
				}
			});
			Node.debug("�ڵ� " + this.name + " ����");
			this.addToReturnList();
			Node.debugTabCount--;
			return;
		}
		
		Set<Substance> newPossibles = this.measure();
		if (newPossibles.size() == 0) {
			throw new Exception("�޷��ƶϽڵ� " + this.name);
		}
		if (this.possibles.equals(newPossibles)) {
			Node.debug("�ڵ� " + this.name + " ����ǰ��δ�����仯���ڵ㷵��");
			this.addToReturnList();
			Node.debugTabCount--;
			return;
		}
		this.possibles = newPossibles;
		// ������ͬ�ڵ㲻����ͬһ������
		this.tryRemoveConstant();
		this.debugThis('>');
		

		Node.debug("�ڵ� " + this.name + " �ĸ��½���");
		// �������ڽڵ�
		this.operateAdjacentNodes((Node node) -> {
			if (node != sender) {
				Node.debug(" + �ڵ� " + this.name + " �����ڵ� " + node.name + " ����");
				node.update(this);
			}
		});

		this.addToReturnList();
		Node.debug("�ڵ� " + this.name + " ����");
		Node.debugTabCount--;
	}
	
	// �ж��Լ�
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
		
		// �����޶����޶�
		for (String l : this.limiters) {
			newPossibles.retainAll(Program.limiters.get(l));
		}
		
		return newPossibles;
	}
	
	// �����޶����������п��ܵ����ʣ�ֻ����Ҫʱʹ��
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
				Node.debug(" - �����£��ڵ� " + node.name + " �Ƴ����� " + substance.name + 
						"����Ϊ�ڵ� " + this.name + " �Ѿ�ȷ���˸�����");
				node.possibles.remove(substance);
				node.debugThis('-');
			}
		}
	}
	
	// �����������ڽڵ�
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
	
	// ���Լ���ӵ�����˳���б���
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
		string.append(" " + c + " (����) �ڵ� " + this.name + " ���ܵ����ʣ�");
		for (Substance substance : this.possibles) {
			string.append(substance.name + " ");
		}
		Node.debug(string.toString());
		string.setLength(0);
		string.append(" " + c + " (����) �ڵ� " + this.name + " �޶�����");
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
