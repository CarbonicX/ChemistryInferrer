/*******************************************************************************
 * ChemistryInferrer ��Ҫԭ��
 * 
 * ��ʼ����
 * 1. ����ÿһ����ȷ���������ʵĽڵ���Ϊ��ʼ�ڵ� [1] �����û�У���ѡ��һ�������������Ľڵ���Ϊ��ʼ�ڵ㣻
 * 2. ����ó�ʼ�ڵ���������ڽڵ㶼�Ѿ�ȷ���˾������ʣ��������ó�ʼ�ڵ㣬����Ҫ���ʼ�ڵ���������нڵ�����Լ���
 * �ݹ飺
 * 3. ���һ���Ѿ�ȷ���˾������ʵĽڵ㱻��һ���ڵ� N Ҫ������Լ�����Ҫ����˽ڵ� N ���������û��ȷ���������ʵĽڵ�����Լ���
 *       ���û�������Ľڵ㣬����ֱ�ӷ��أ�
 * 4. ���һ��û��ȷ���������ʵĽڵ㱻��һ���ڵ� N Ҫ������Լ������ȸ������ڽڵ����Ϣ�ж��Լ���������Щ���ʣ�
 *       ��Ҫ����˽ڵ� N ������������ڽڵ�����Լ���
 * 5. ���һ���ڵ����ǰ���Լ����ܵ�����û�з����仯������ֱ�ӷ��أ�
 * ��β��
 * 6. ÿһ�εݹ�����󣬰��սڵ㷵��˳��Ӻ�ǰ���θ����Լ� [2] ��
 * 
 * [1] ��ֹ���־ֲ�����
 * [2] ��ֹ���ֵ����ƶ�ͼ��ĳЩ�ڵ��޷��õ�����
 *******************************************************************************/

package com.carbonicx.chemistryinferrer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// �������
public class Program {
	private static boolean debug = false;
	// �����������������У���ͬ�����ʺͽڵ�ֻ����һ��ʵ��
	public static Map<String, Node> nodes = new HashMap<>();
	public static Map<String, Substance> substances = new HashMap<>();
	public static Map<String, Set<Substance>> ions = new HashMap<>();
	public static Map<String, Set<Substance>> limiters = new HashMap<>();
	
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.out.println("ChemistryInferrer �÷���");
			System.out.println("\t[-d/--debug] <�ƶ��ļ�>");
			System.exit(-1);
		}
		String inferFile;
		if (args[0].equals("-d") || args[0].equals("--debug")) {
			Program.debug = true;
			if (args.length < 2) throw new Exception("��������");
			inferFile = args[1];
		} else {
			inferFile = args[0];
		}
		
		long startLoad = System.currentTimeMillis();
		Loader.analysisIons();
		Loader.analysisLimiters();
		Loader.analysisReactions();
		Loader.analysisNodes(inferFile);
		long endLoad = System.currentTimeMillis();
		System.out.println("�����ļ���ɣ���ʱ " + (endLoad - startLoad) / 1000F + " ��");
		
		// ����
		long startTime = System.currentTimeMillis();
		Set<Node> initialNodes = new HashSet<>();
		for (Node node : Program.nodes.values()) {
			if (node.possibles.size() == 1) initialNodes.add(node);
		}
		if (initialNodes.size() == 0) {
			for (Node node : Program.nodes.values()) {
				if (node.limiters.size() != 0) {
					initialNodes.add(node);
					break;
				}
			}
		}
		if (initialNodes.size() == 0) {
			initialNodes.add((Node)Program.nodes.values().toArray()[0]);
		}
		for (Node node : initialNodes) {
			node.startAsInitialNode();
		}
		long endTime = System.currentTimeMillis();
		// ����
		System.out.println("���н�������ʱ " + (endTime - startTime) / 1000F + " �룬���п��ܵĽ����");
		for (Node node : Program.nodes.values()) {
			System.out.print("\t" + node.name + " : ");
			if (node.possibles.size() != 0) {
				for (Substance substance : node.possibles) {
					System.out.print(substance.name + " ");
				}
			} else if (node.limiters.size() != 0) {
				for (String limiter : node.limiters) {
					System.out.print("[" + limiter + "] ");
				}
			} else {
				System.out.println("δ֪");
			}
			System.out.println();
		}
		
	}
	
	public static void debugln(String text) {
		if (Program.debug) System.out.println(text);
	}
	
	public static void debug(String text) {
		if (Program.debug) System.out.print(text);
	}
}
