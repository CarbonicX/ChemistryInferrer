package com.carbonicx.chemistryinferrer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

// ���ڼ����ļ�
// ǰ��������Ժ������ع���
public class Loader {
	private static Set<IonInteraction> interactions = new HashSet<>();
	
	private static List<String> readFileByLine(String path) throws Exception {
		List<String> content = new ArrayList<>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
		String line;
		while ((line = br.readLine()) != null) {
			content.add(line);
		}
		br.close();
		return content;
	}

	private static List<String> getIonTokens(String line) {
		List<String> result = new ArrayList<>();
		StringBuilder token = new StringBuilder();
		for (char c : line.toCharArray()) {
			if ((Character.isWhitespace(c) || c == '~')) {
				if (!token.isEmpty()) {
					result.add(token.toString());
					token.setLength(0);
				}
				continue;
			}
			if (c == '{' || c == '}') continue;
			token.append(c);
		}
		result.add(token.toString());
		return result;
	}

	private static List<String> getLimiterTokens(String line) {
		List<String> result = new ArrayList<>();
		StringBuilder token = new StringBuilder();
		for (char c : line.toCharArray()) {
			if (Character.isWhitespace(c) || c == '~') {
				if (!token.isEmpty()) {
					result.add(token.toString());
					token.setLength(0);
				}
				continue;
			}
			if (c == '[' || c == ']') continue;
			token.append(c);
		}
		if (!token.isEmpty()) result.add(token.toString());
		return result;
	}
	
	private static ReactionLiteral getReactionLiteral(String line) throws Exception {
		ReactionLiteral reaction = new ReactionLiteral();
		StringBuilder token = new StringBuilder();
		ReactionLiteralObject object = null;
		boolean isReactant = true;
		boolean hasConditions = false;
		boolean hasCategories = false;
		int index = 0;
		for (int i = 0; i < 2; i++) {
			for (int j = index; j < line.length(); j++) {
				char c = line.charAt(j);
				if (c == '}') continue;
				if (c == '`') {
					hasConditions = true;
					index = j + 1;
					// ��ʱ�Ѿ�����������ⲿ for ѭ��
					break;
				}
				if (c == '@') {
					hasCategories = true;
					index = j + 1;
					break;
				}
				if (c == '>') {
					if (!token.isEmpty()) {
						object.name = token.toString();
						reaction.reactants.add(object);
						token.setLength(0);
						object = null;
					}
					isReactant = false;
					index = j + 1;
					// ��ʱ�Ѿ������һ���ⲿ for ѭ��
					break;
				}
				if (Character.isWhitespace(c)) {
					if (!token.isEmpty()) {
						object.name = token.toString();
						if (isReactant) {
							reaction.reactants.add(object);
						} else {
							reaction.products.add(object);
						}
						token.setLength(0);
						object = null;
					}
					continue;
				}
				if (token.isEmpty() && c == '{') {
					object = new ReactionLiteralIonic();
					// ��ʱ object != null ���� token.isEmpty() == true
					// ��������²����������
					continue;
				}
				if (object == null && token.isEmpty() && c != '{') {
					object = new ReactionLiteralNonionic();
					token.append(c);
					continue;
				}
				token.append(c);
				// ����ڴ˽��������Ѿ�����������ⲿ for ѭ��
			}
			if (!token.isEmpty()) {
				object.name = token.toString();
				if (isReactant) {
					reaction.reactants.add(object);
				} else {
					reaction.products.add(object);
				}
				token.setLength(0);
				object = null;
			}
		}
		if (hasConditions) {
			token = new StringBuilder();
			// �� ` ���ŵ���һλ��ʼ
			for (int i = index; i < line.length(); i++) {
				char c = line.charAt(i);
				if (c == '`') {
					if (token.length() != 0) {
						if (token.toString().equals("(Null)") || token.toString().equals("(Any)")) {
							throw new Exception("��Ӧ�Ĳ��Ϸ����� `(Null)` �� `(Any)`");
						}
						reaction.conditions.add(token.toString());
						token.setLength(0);
						continue;
					} else {
						continue;
					}
				}
				if (Character.isWhitespace(c)) continue;
				if (c == '@') {
					hasCategories = true;
					index = i + 1;
					break;
				}
				token.append(c);
			}
		} else {
			reaction.conditions.add("(Null)");
		}
		if (hasCategories) {
			token = new StringBuilder();
			for (int i = index; i < line.length(); i++) {
				char c = line.charAt(i);
				if (Character.isWhitespace(c)) {
					if (token.length() != 0) {
						reaction.categories.add(token.toString());
						token.setLength(0);
						continue;
					} else {
						continue;
					}
				}
				if (c == '@') continue;
				token.append(c);
			}
			if (token.length() != 0) reaction.categories.add(token.toString());
		}
		return reaction;
	}
	
	// ��������
	public static void analysisIons() throws Exception {
		List<String> content = Loader.readFileByLine(
				System.getProperty("user.dir") + File.separator + "references/ions.txt");
		
		for (int i = 0; i < content.size(); i++) {
			String line = content.get(i);
			if (line.startsWith("//")) continue;
			if (line.isBlank()) continue;
			List<String> tokens = getIonTokens(line);
			if (tokens.size() < 2) throw new Exception("�﷨����λ�� ions.txt �� " + i + " ��");
			Set<Substance> set = new HashSet<>();
			for (int j = 1; j < tokens.size(); j++) {
				Substance substance;
				if (!Program.substances.containsKey(tokens.get(j))) {
					substance = new Substance(tokens.get(j));
					set.add(substance);
					Program.substances.put(tokens.get(j), substance);
				} else {
					substance = Program.substances.get(tokens.get(j));
					set.add(substance);
				}
			}
			Program.ions.put(tokens.get(0), set);
		}
		Program.debugln("[Loader.analysisIons] ����������ɣ��������ӣ�");
		
		if (Program.ions.size() == 0) return;
		
		for (String key : Program.ions.keySet()) {
			Program.debug("\t" + key + "\t<-->");
			for (Substance substance : Program.ions.get(key)) {
				Program.debug("\t" + substance.name);
			}
			Program.debugln("");
		}
	}
	
	// �����޶���
	public static void analysisLimiters() throws Exception {
		List<String> content = Loader.readFileByLine(
				System.getProperty("user.dir") + File.separator + "references/limiters.txt");
		for (int i = 0; i < content.size(); i++) {
			String line = content.get(i);
			if (line.startsWith("//")) continue;
			if (line.isBlank()) continue;
			List<String> tokens = getLimiterTokens(line);
			if (tokens.size() < 2) throw new Exception("�﷨����λ�� limiters.txt �� " + i + " ��");
			Set<Substance> set = new HashSet<>();
			for (int j = 1; j < tokens.size(); j++) {
				Substance substance;
				if (!Program.substances.containsKey(tokens.get(j))) {
					substance = new Substance(tokens.get(j));
					set.add(substance);
					Program.substances.put(tokens.get(j), substance);
				} else {
					substance = Program.substances.get(tokens.get(j));
					set.add(substance);
				}
			}
			Program.limiters.put(tokens.get(0), set);
		}
		Program.debugln("[Loader.analysisLimiters] �����޶�����ɣ������޶�����");

		if (Program.limiters.size() == 0) return;
		
		for (String key : Program.limiters.keySet()) {
			Program.debug("\t[" + key + "]\t<-->");
			for (Substance substance : Program.limiters.get(key)) {
				Program.debug("\t" + substance.name);
			}
			Program.debugln("");
		}
	}
	
	// ������Ӧ
	public static void analysisReactions() throws Exception {
		List<String> content = Loader.readFileByLine(
				System.getProperty("user.dir") + File.separator + "references/reactions.txt");
		Set<ReactionLiteral> reactions = new HashSet<>();
		for (int i = 0; i < content.size(); i++) {
			String line = content.get(i);
			if (line.startsWith("//")) continue;
			if (line.isBlank()) continue;
			ReactionLiteral reaction;
			try {
				reaction = Loader.getReactionLiteral(line);
			} catch (Exception e) {
				throw new Exception("�﷨����λ�� reactions.txt �� " + i + " ��");
			}
			reactions.add(reaction);
		}
		
		for (ReactionLiteral reaction : reactions) {
			for (ReactionLiteralObject object : reaction.reactants) {
				if (object instanceof ReactionLiteralNonionic && !Program.substances.containsKey(object.name)) {
					Program.substances.put(object.name, new Substance(object.name));
				}
			}
			for (ReactionLiteralObject object : reaction.products) {
				if (object instanceof ReactionLiteralNonionic && !Program.substances.containsKey(object.name)) {
					Program.substances.put(object.name, new Substance(object.name));
				}
			}
			if (Loader.isDoubeDecomp(reaction)) {
				Iterator<ReactionLiteralObject> reactants = reaction.reactants.iterator();
				IonInteraction interaction = new IonInteraction();
				interaction.ionA = reactants.next().name;
				interaction.ionB = reactants.next().name;
				for (ReactionLiteralObject p : reaction.products) {
					interaction.substances.add(p.name);
				}
				interactions.add(interaction);
			}
		}
		
		for (ReactionLiteral reaction : reactions) {
			// �����ӷ�Ӧ���������ڱ��ּ򵥵Ļ����Ϸ�ֹ���� H2CO3 �� HCl �������
			if (!reaction.isIonicReaction()) {
				for (ReactionLiteralObject source : reaction.reactants) {
					for (ReactionLiteralObject target : reaction.reactants) {
						if (target == source) continue;
						Program.substances.get(source.name).towardWith.add(new Reaction(
								Program.substances.get(target.name), reaction.conditions, reaction.categories
						));
					}
					for (ReactionLiteralObject target : reaction.products) {
						Program.substances.get(source.name).towardTo.add(new Reaction(
								Program.substances.get(target.name), reaction.conditions, reaction.categories
						));
					}
				}
				for (ReactionLiteralObject source : reaction.products) {
					for (ReactionLiteralObject target : reaction.products) {
						if (target == source) continue;
						Program.substances.get(source.name).backwardWith.add(new Reaction(
								Program.substances.get(target.name), reaction.conditions, reaction.categories
						));
					}
					for (ReactionLiteralObject target : reaction.reactants) {
						Program.substances.get(source.name).backwardTo.add(new Reaction(
								Program.substances.get(target.name), reaction.conditions, reaction.categories
						));
					}
				}
			}
			/*
			 * һ����û���Ӧ�͸��ֽⷴӦ����Ӧ�����ֻ������
			 * ���ֽⷴӦ XA BY > XY... BA ���û���Ӧ XA B(Y) > AB X(Y)
			 * ���ֽⷴӦ X Y > XY         ���û���Ӧ X Bn > B Xn (n: nonionic)
			 * ���� XA����֪ XA �� BY ��Ӧ���������� XY...���������� BA
			 * ���� BY����֪ BY �� XA ��Ӧ���������� XY...���������� BA
			 * ���� XY...����֪ XY... �ɱ� XA ���ɣ��ɱ� BY ���ɣ����� BA ���� XY...����ͬ������
			 * ���� BA����֪ BA �ɱ� XA ���ɣ��ɱ� BY ���ɣ����� XY... ��ͬ������
			 * �ڸ��ֽⷴӦ�У�X �� Y ȷ����A ���� XA ������ˮ��B ���� BY ������ˮ
			 * ���û���Ӧ�У�X �� B ȷ����Y �� null��A ���� XA �� AB ��������ˮ
			 * X A B һ�������ӣ�Y �����ӻ� null��B + null -> Bn��X + null -> Xn
			 */
			else {
				if (reaction.reactants.size() != 2) throw new Exception("��Ӧ�����������ӷ�Ӧ�ķ�Ӧ��ֻ��������");
				Iterator<ReactionLiteralObject> riterator = reaction.reactants.iterator();
				Iterator<ReactionLiteralObject> piterator = reaction.products.iterator();
				// ���ֽⷴӦ
				if (isDoubeDecomp(reaction)) {
					reaction.categories.add("double-decomposition");
					String strX = riterator.next().name;
					String strY = riterator.next().name;
					Set<Substance> XYs = new HashSet<>();
					for (ReactionLiteralObject object : reaction.products) {
						XYs.add(Program.substances.get(object.name));
					}
					Set<String> As = Loader.getSolubles(strX);
					Set<String> Bs = Loader.getSolubles(strY);
					// ��ÿһ�� forAll ���棬���������Ϊ��
					// substance �Ѿ����˶�Ӧ�� A�����������Է�Ӧ�� / �������ɵ������е� A �͹̶���
					Loader.forAll(strX, As, (Substance substance, String A) -> {
						substance.towardWith.addAll(Loader.getAllReactions(strY, Bs, reaction.conditions, reaction.categories));
						substance.towardTo.addAll(Loader.turnToReactions(XYs, reaction.conditions, reaction.categories));
						substance.towardTo.addAll(Loader.getAllReactionsAllowInsoluble(A, Bs, reaction.conditions, reaction.categories));
					});
					Loader.forAll(strY, Bs, (Substance substance, String B) -> {
						substance.towardWith.addAll(Loader.getAllReactions(strX, As, reaction.conditions, reaction.categories));
						substance.towardTo.addAll(Loader.turnToReactions(XYs, reaction.conditions, reaction.categories));
						substance.towardTo.addAll(Loader.getAllReactionsAllowInsoluble(B, As, reaction.conditions, reaction.categories));
					});
					for (Substance substance : XYs) {
						substance.backwardTo.addAll(Loader.getAllReactions(strX, As, reaction.conditions, reaction.categories));
						substance.backwardTo.addAll(Loader.getAllReactions(strY, Bs, reaction.conditions, reaction.categories));
						substance.backwardWith.addAll(Loader.getAllReactionsAllowInsoluble(As, Bs, reaction.conditions, reaction.categories));
						Set<Substance> exceptSelf = new HashSet<Substance>(XYs);
						exceptSelf.remove(substance);
						substance.backwardWith.addAll(Loader.turnToReactions(exceptSelf, reaction.conditions, reaction.categories));
					}
					Loader.forAllAllowInsoluble(As, Bs, (Substance substance, String A, String B) -> {
						substance.backwardTo.add(new Reaction(strX, A, reaction.conditions, reaction.categories));
						substance.backwardTo.add(new Reaction(strY, B, reaction.conditions, reaction.categories));
						substance.backwardWith.addAll(Loader.turnToReactions(XYs, reaction.conditions, reaction.categories));
					});
				} else {
					// �û���Ӧ
					reaction.categories.add("replacement");
					ReactionLiteralObject temp = riterator.next();
					String strX;
					String strBn;
					if (temp instanceof ReactionLiteralIonic) {
						strX = temp.name;
						strBn = riterator.next().name;
					} else {
						strX = riterator.next().name;
						strBn = temp.name;
					}
					String strXn;
					String strB;
					temp = piterator.next();
					if (temp instanceof ReactionLiteralIonic) {
						strB = temp.name;
						strXn = piterator.next().name;
					} else {
						strB = piterator.next().name;
						strXn = temp.name;
					}
					Set<String> As = Loader.getSolubles(strX);
					As.retainAll(Loader.getSolubles(strB));
					Loader.forAll(strX, As, (Substance substance, String A) -> {
						substance.towardWith.add(new Reaction(Program.substances.get(strBn), reaction.conditions, reaction.categories));
						substance.towardTo.add(new Reaction(Program.substances.get(strXn), reaction.conditions, reaction.categories));
						substance.towardTo.add(new Reaction(strB, A, reaction.conditions, reaction.categories));
					});
					Program.substances.get(strBn).towardWith.addAll(Loader.getAllReactions(strX, As, reaction.conditions, reaction.categories));
					Program.substances.get(strBn).towardTo.add(new Reaction(Program.substances.get(strXn), reaction.conditions, reaction.categories));
					Program.substances.get(strBn).towardTo.addAll(Loader.getAllReactions(strB, As, reaction.conditions, reaction.categories));
					
					Program.substances.get(strXn).backwardTo.addAll(Loader.getAllReactions(strX, As, reaction.conditions, reaction.categories));
					Program.substances.get(strXn).backwardTo.add(new Reaction(Program.substances.get(strBn), reaction.conditions, reaction.categories));
					Program.substances.get(strXn).backwardWith.addAll(Loader.getAllReactions(strB, As, reaction.conditions, reaction.categories));
					Loader.forAll(strB, As, (Substance substance, String A) -> {
						substance.backwardTo.add(new Reaction(strX, A, reaction.conditions, reaction.categories));
						substance.backwardTo.add(new Reaction(Program.substances.get(strBn), reaction.conditions, reaction.categories));
						substance.backwardWith.add(new Reaction(Program.substances.get(strXn), reaction.conditions, reaction.categories));
					});
				}
			}
		}
		
		Program.debugln("[Loader.analysisReactions] ������Ӧ��ɣ����з�Ӧ��");
		for (Substance substance : Program.substances.values()) {
			Program.debugln("\t" + substance.name);
			Program.debugln("\t\t����...��");
			for (Reaction reaction : substance.towardTo) {
				Program.debug("\t\t\t" + reaction.substance.name);
				for (String c : reaction.conditions) Program.debug(",`" + c + "`");
				for (String c : reaction.categories) Program.debug(",@" + c);
				Program.debugln("");
			}
			Program.debugln("\n\t\t��...��Ӧ��");
			for (Reaction reaction : substance.towardWith) {
				Program.debug("\t\t\t" + reaction.substance.name);
				for (String c : reaction.conditions) Program.debug(",`" + c + "`");
				for (String c : reaction.categories) Program.debug(",@" + c);
				Program.debugln("");
			}
			Program.debugln("\n\t\t��...���ɣ�");
			for (Reaction reaction : substance.backwardTo) {
				Program.debug("\t\t\t" + reaction.substance.name);
				for (String c : reaction.conditions) Program.debug(",`" + c + "`");
				for (String c : reaction.categories) Program.debug(",@" + c);
				Program.debugln("");
			}
			Program.debugln("\n\t\t��...��ͬ�����ɣ�");
			for (Reaction reaction : substance.backwardWith) {
				Program.debug("\t\t\t" + reaction.substance.name);
				for (String c : reaction.conditions) Program.debug(",`" + c + "`");
				for (String c : reaction.categories) Program.debug(",@" + c);
				Program.debugln("");
			}
			Program.debugln("");
		}
	}
	
	public static void analysisNodes(String path) throws Exception {
		List<String> content = Loader.readFileByLine(
				System.getProperty("user.dir") + File.separator + path);
		for (int i = 0; i < content.size(); i++) {
			String line = content.get(i);
			if (line.startsWith("//")) continue;
			if (line.isBlank()) continue;
			List<String> tokens = Loader.getStringTokens(line);
			String command = tokens.get(0);
			// node N...
			if (command.equals("node")) {
				for (int j = 1; j < tokens.size(); j++) {
					Node node = new Node();
					node.name = tokens.get(j);
					Program.nodes.put(tokens.get(j), node);
				}
			}
			// set
			else if (command.equals("set")) {
				String nodeName = tokens.get(1);
				String setType = tokens.get(2);
				// set N substance SUBSTANCE
				if (setType.equals("substance")) {
					Node node = Program.nodes.get(nodeName);
					node.possiblesNotDetermined = false;
					node.possibles.clear();
					node.possibles.add(Program.substances.get(tokens.get(3)));
				}
				// set N possibles SUBSTANCE...
				else if (setType.equals("possibles")) {
					Node node = Program.nodes.get(nodeName);
					node.possiblesNotDetermined = false;
					node.possibles.clear();
					for (int j = 3; j < tokens.size(); j++) {
						node.possibles.add(Program.substances.get(tokens.get(j)));
					}
				}
				// set N possible-ions ION...
				else if (setType.equals("possible-ions")) {
					Node node = Program.nodes.get(nodeName);
					node.possiblesNotDetermined = false;
					node.possibles.clear();
					for (int j = 3; j < tokens.size(); j++) {
						String ionString = tokens.get(j).substring(1, tokens.get(j).length() - 1);
						node.possibles.addAll(Program.ions.get(ionString));
					}
				}
				// set N limiters LIMITER...
				else if (setType.equals("limiters")) {
					Node node = Program.nodes.get(nodeName);
					node.limiters.clear();
					for (int j = 3; j < tokens.size(); j++) {
						String limiterString = tokens.get(j).substring(1, tokens.get(j).length() - 1);
						node.limiters.add(limiterString);
					}
				}
			}
			// add
			else if (command.equals("add")) {
				boolean forceConditions = tokens.contains("!");
				if (forceConditions) tokens.remove("!");
				boolean hasAngleBracket = tokens.contains(">");
				List<String> removes = new ArrayList<>();
				Set<String> categories = new HashSet<>();
				for (String token : tokens) {
					if (token.startsWith("@")) {
						categories.add(token.substring(1));
						removes.add(token);
					}
				}
				tokens.removeAll(removes);
				removes.clear();
				Set<String> conditions = new HashSet<>();
				for (String token : tokens) {
					if (token.startsWith("`") && token.endsWith("`")) {
						String condition = token.substring(1, token.length() - 1);
						if (condition.equals("(Any)")) {
							throw new Exception("�ڵ��ϵ�Ĳ��Ϸ����� `(Any)`");
						}
						conditions.add(condition);
						removes.add(token);
					}
				}
				tokens.removeAll(removes);
				removes = null;
				if (conditions.size() == 0) {
					if (forceConditions) {
						throw new Exception("��û��Ϊ�ڵ��ϵ�������������±����ǿ���������� `!`");
					}
					conditions.add("(Any)");
				}
				if (!forceConditions && conditions.contains("(Null)")) {
					throw new Exception("��û�б��ǿ���������� `!` �������Ϊ�ڵ��ϵ������� `(Null)` û������");
				}
				// add N...
				if (!hasAngleBracket) {
					for (int j = 1; j < tokens.size(); j++) {
						Node node = Program.nodes.get(tokens.get(j));
						Set<Node> with = new HashSet<>();
						for (int k = 1; k < tokens.size(); k++) {
							if (tokens.get(k).equals(tokens.get(j))) continue;
							with.add(Program.nodes.get(tokens.get(k)));
						}
						NodeRelation relation = new NodeRelation(
								NodeRelationType.TOWARD, new HashSet<>(), with, conditions, forceConditions, categories
						);
						node.nodeRelations.add(relation);
					}
					continue;
				}
				// add > N...
				if (tokens.get(1).equals(">")) {
					for (int j = 1; j < tokens.size(); j++) {
						Node node = Program.nodes.get(tokens.get(j));
						node.nodeRelations.add(Loader.generateNodeRelation(
								NodeRelationType.TOWARD, null, tokens.subList(2, tokens.size()), 
								conditions, forceConditions, categories, tokens.get(j)
						));
					}
					continue;
				}
				// add N1... > N2... `CONDITION`
				else {
					List<String> reactants = new ArrayList<>();
					List<String> products = new ArrayList<>();
					boolean inReactants = true;
					for (String string : tokens.subList(1, tokens.size())) {
						if (string.equals(">")) {
							inReactants = false;
							continue;
						}
						if (inReactants) reactants.add(string);
						else products.add(string);
					}
					for (String r : reactants) {
						Node node = Program.nodes.get(r);
						node.nodeRelations.add(Loader.generateNodeRelation(
								NodeRelationType.TOWARD, products, reactants, conditions, forceConditions, categories, r
						));
					}
					for (String p : products) {
						Node node = Program.nodes.get(p);
						node.nodeRelations.add(Loader.generateNodeRelation(
								NodeRelationType.BACKWARD, reactants, products, conditions, forceConditions, categories, p
						));
					}
				}
			}
		}
		
		for (Node node : Program.nodes.values()) {
			if (node.possibles.size() != 0 && node.limiters.size() != 0) {
				for (String l : node.limiters) {
					node.possibles.retainAll(Program.limiters.get(l));
				}
				if (node.possibles.size() == 0) throw new Exception("�޷��ƶϽڵ� " + node.name);
			}
		}
		
		Program.debugln("[Loader.analysisNodes] �����ڵ���ɣ����нڵ㣺");
		for (Node node : Program.nodes.values()) {
			Program.debugln("\t" + node.name);
			Program.debug("\t\t���ܵ����ʣ�");
			for (Substance substance : node.possibles) {
				Program.debug(substance.name + " ");
			}
			Program.debug("\n\t\t�޶�����");
			for (String limiter : node.limiters) {
				Program.debug("[" + limiter + "] ");
			}
			Program.debugln("");
			for (NodeRelation relation : node.nodeRelations) {
				Program.debug("\t\t(�Լ�)");
				for (Node n : relation.with) {
					Program.debug(" " + n.name);
				}
				if (relation.type == NodeRelationType.TOWARD) Program.debug("    -->   ");
				else Program.debug("    <--   ");
				for (Node n : relation.to) {
					Program.debug(" " + n.name);
				}
				if (relation.forceConditions) Program.debug(" !");
				for (String c : relation.condition) Program.debug(" `" + c + "`");
				for (String c : relation.categories) Program.debug(" @" + c);
				Program.debugln("");
			}
		}
	}
	
	private static NodeRelation generateNodeRelation(NodeRelationType type, List<String> to, 
			List<String> with, Set<String> conditions, boolean forceConditions, Set<String> categories, String self) {
		Set<Node> toSet = new HashSet<>();
		Set<Node> withSet = new HashSet<>();
		if (to != null) {
			for (String string : to) toSet.add(Program.nodes.get(string));
		}
		if (with != null) {
			for (String string : with) {
				if (string.equals(self)) continue;
				withSet.add(Program.nodes.get(string));
			}
		}
		return new NodeRelation(type, toSet, withSet, conditions, forceConditions, categories);
	}
	
	private static List<String> getStringTokens(String line) {
		List<String> tokens = new ArrayList<>();
		StringBuilder token = new StringBuilder();
		for (char c : line.toCharArray()) {
			if (!Character.isWhitespace(c)) token.append(c);
			if (Character.isWhitespace(c) && !token.isEmpty()) {
				tokens.add(token.toString());
				token.setLength(0);
			}
		}
		if (!token.isEmpty()) tokens.add(token.toString());
		return tokens;
	}
	
	private static Set<String> getSolubles(String str) {
		Set<String> result = new HashSet<>();
		for (Substance substance : Program.ions.get(str)) {
			for (String key : Program.ions.keySet()) {
				if (Program.ions.get(key).contains(substance) && !key.equals(str)) result.add(key);
			}
		}
		return result;
	}
	
	private static boolean isDoubeDecomp(ReactionLiteral reaction) {
		for (ReactionLiteralObject object : reaction.reactants) {
			if (object instanceof ReactionLiteralNonionic) {
				return false;
			}
		}
		return true;
	}
	
	private static void forAll(String known, Set<String> all, OneUnknownOperator action) throws Exception {
		for (String str : all) {
			String name = Loader.getSubstanceNameByIons(known, str);
			if (name == null) throw new Exception("�޷����������ҵ���Ӧ������");
			action.accept(Program.substances.get(name), str);
		}
	}
	
	private static void forAllAllowInsoluble(Set<String> allA, Set<String> allB, 
			TwoUnknownOperator action) throws Exception {
		for (String strA : allA) {
			for (String strB : allB) {
				String name = Loader.getSubstanceNameByIons(strA, strB);
				if (name == null) {
					Set<String> substances = null;
					for (IonInteraction i : Loader.interactions) {
						if ((i.ionA.equals(strA) && i.ionB.equals(strB)) || 
								(i.ionA.equals(strB) && i.ionB.equals(strA))) {
							substances = i.substances;
							break;
						}
					}
					if (substances == null) continue;
					for (String s : substances) {
						action.accept(Program.substances.get(s), strA, strB);
					}
					continue;
				}
				action.accept(Program.substances.get(name), strA, strB);
			}
		}
	}
	
	private static Set<Reaction> getAllReactions(String known, Set<String> all, 
			Set<String> conditions, Set<String> categories) throws Exception {
		Set<Reaction> reactions = new HashSet<>();
		for (String str: all) {
			String name = Loader.getSubstanceNameByIons(known, str);
			if (name == null) throw new Exception("�޷����������ҵ���Ӧ������");
			reactions.add(new Reaction(Program.substances.get(name), conditions, categories));
		}
		return reactions;
	}
	
	private static Set<Reaction> getAllReactionsAllowInsoluble(String known, Set<String> all, 
			Set<String> conditions, Set<String> categories) throws Exception {
		Set<Reaction> reactions = new HashSet<>();
		for (String str: all) {
			String name = Loader.getSubstanceNameByIons(known, str);
			if (name == null) {
				Set<String> substances = null;
				for (IonInteraction i : Loader.interactions) {
					if ((i.ionA.equals(known) && i.ionB.equals(str)) || 
							(i.ionA.equals(str) && i.ionB.equals(known))) {
						substances = i.substances;
						break;
					}
				}
				if (substances == null) continue;
				for (String s : substances) {
					reactions.add(new Reaction(Program.substances.get(s), conditions, categories));
				}
				continue;
			}
			reactions.add(new Reaction(Program.substances.get(name), conditions, categories));
		}
		return reactions;
	}
	
	private static Set<Reaction> getAllReactionsAllowInsoluble(Set<String> allA, Set<String> allB, 
			Set<String> conditions, Set<String> categories) throws Exception {
		Set<Reaction> reactions = new HashSet<>();
		for (String strA : allA) {
			for (String strB : allB) {
				String name = Loader.getSubstanceNameByIons(strA, strB);
				if (name == null) {
					Set<String> substances = null;
					for (IonInteraction i : Loader.interactions) {
						if ((i.ionA.equals(strA) && i.ionB.equals(strB)) || 
								(i.ionA.equals(strB) && i.ionB.equals(strA))) {
							substances = i.substances;
							break;
						}
					}
					if (substances == null) continue;
					for (String s : substances) {
						reactions.add(new Reaction(Program.substances.get(s), conditions, categories));
					}
					continue;
				}
				reactions.add(new Reaction(Program.substances.get(name), conditions, categories));
			}
		}
		return reactions;
	}
	
	private static Set<Reaction> turnToReactions(Set<Substance> substances, Set<String> conditions, 
			Set<String> categories) {
		Set<Reaction> reactions = new HashSet<>();
		for (Substance s : substances) {
			reactions.add(new Reaction(s, conditions, categories));
		}
		return reactions;
	}
	
	public static String getSubstanceNameByIons(String ionA, String ionB) throws Exception {
		Set<Substance> set = new HashSet<>(Program.ions.get(ionA));
		set.retainAll(Program.ions.get(ionB));
		if (set.size() == 0) return null;
		if (set.size() > 1) throw new Exception("������������Ӧ�����ʳ���һ��");
		Substance substance = (Substance)set.toArray()[0];
		for (String key : Program.substances.keySet()) {
			if (Program.substances.get(key) == substance) return key;
		}
		return null;
	}
}

// ���ڽ�����Ӧ�ķ�Ӧ��
class ReactionLiteral {
	Set<ReactionLiteralObject> reactants = new HashSet<>();
	Set<ReactionLiteralObject> products = new HashSet<>();
	Set<String> conditions = new HashSet<>();
	Set<String> categories = new HashSet<>();
	
	// �ж��Ƿ������ӷ�Ӧ
	public boolean isIonicReaction() {
		for (ReactionLiteralObject object : this.reactants) {
			if (object instanceof ReactionLiteralIonic) return true;
		}
		for (ReactionLiteralObject object : this.products) {
			if (object instanceof ReactionLiteralIonic) return true;
		}
		return false;
	}
}

class ReactionLiteralObject {
	String name;
}

class ReactionLiteralIonic extends ReactionLiteralObject {}
class ReactionLiteralNonionic extends ReactionLiteralObject {}

class IonInteraction {
	String ionA;
	String ionB;
	Set<String> substances = new HashSet<>();
}

@FunctionalInterface
interface OneUnknownOperator {
	void accept(Substance substance, String current) throws Exception;
}

@FunctionalInterface
interface TwoUnknownOperator {
	void accept(Substance substance, String currentA, String currentB) throws Exception;
}

