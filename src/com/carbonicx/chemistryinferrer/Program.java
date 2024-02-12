/*******************************************************************************
 * ChemistryInferrer 简要原理
 * 
 * 初始化：
 * 1. 遍历每一个已确定具体物质的节点作为初始节点 [1] ，如果没有，则选定一个所得条件最多的节点作为初始节点；
 * 2. 如果该初始节点的所有相邻节点都已经确定了具体物质，则跳过该初始节点，否则要求初始节点的相邻所有节点更新自己；
 * 递归：
 * 3. 如果一个已经确定了具体物质的节点被另一个节点 N 要求更新自己，则要求除了节点 N 以外的所有没有确定具体物质的节点更新自己，
 *       如果没有这样的节点，则函数直接返回；
 * 4. 如果一个没有确定具体物质的节点被另一个节点 N 要求更新自己，则先根据相邻节点等信息判断自己可能是哪些物质，
 *       再要求除了节点 N 以外的所有相邻节点更新自己；
 * 5. 如果一个节点更新前后自己可能的物质没有发生变化，则函数直接返回；
 * 收尾：
 * 6. 每一次递归结束后，按照节点返回顺序从后到前依次更新自己 [2] 。
 * 
 * [1] 防止出现局部堵塞
 * [2] 防止出现单线推断图中某些节点无法得到更新
 *******************************************************************************/

package com.carbonicx.chemistryinferrer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// 程序入口
public class Program {
	private static boolean debug = false;
	// 整个程序运行周期中，相同的物质和节点只存在一个实例
	public static Map<String, Node> nodes = new HashMap<>();
	public static Map<String, Substance> substances = new HashMap<>();
	public static Map<String, Set<Substance>> ions = new HashMap<>();
	public static Map<String, Set<Substance>> limiters = new HashMap<>();
	
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.out.println("ChemistryInferrer 用法：");
			System.out.println("\t[-d/--debug] <推断文件>");
			System.exit(-1);
		}
		String inferFile;
		if (args[0].equals("-d") || args[0].equals("--debug")) {
			Program.debug = true;
			if (args.length < 2) throw new Exception("参数错误");
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
		System.out.println("加载文件完成，用时 " + (endLoad - startLoad) / 1000F + " 秒");
		
		// 启动
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
		// 结束
		System.out.println("运行结束，用时 " + (endTime - startTime) / 1000F + " 秒，所有可能的结果：");
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
				System.out.println("未知");
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
