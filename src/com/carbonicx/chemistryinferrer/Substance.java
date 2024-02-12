package com.carbonicx.chemistryinferrer;

import java.util.HashSet;
import java.util.Set;

// 物质
public class Substance {
	public String name;
	
	public Set<Reaction> towardTo = new HashSet<>();
	public Set<Reaction> towardWith = new HashSet<>();
	public Set<Reaction> backwardTo = new HashSet<>();
	public Set<Reaction> backwardWith = new HashSet<>();
	
	public Substance(String name) {
		this.name = name;
	}

	// 通过原节点提供的 NodeRelationType 和 NodeRelationForm 提供可能的物质
	// 即：通过目标节点反推原节点
	// 注意：possibles 是目标节点的，而不是原节点的；NodeRelationType 和 NodeRelationForm 是原节点的
	// N 生成 X (N TOWARD, to X) 是 X 被 N 生成 (X BACKWARD, to N) 充要
	// N 与 X 反应 (N TOWARD, with X) 是 X 与 N 反应 (X TOWARD, with N) 充要
	// N 与 X 共同被生成 (N BACKWARD, with X) 是 X 与 N 共同被生成 (X BACKWARD, with N) 充要
	public static Set<Substance> diverge(
			Set<Substance> possibles, NodeRelationType type, 
			NodeRelationForm form, Set<String> conditions, boolean forceConditions, Set<String> categories) {
		Set<Substance> result = new HashSet<>();
		boolean resultNotInit = true;
		for (Substance s : possibles) {
			if (type == NodeRelationType.TOWARD && form == NodeRelationForm.TO) {
				if (resultNotInit) { 
					result = getSuitableSubstances(s.backwardTo, conditions, forceConditions, categories);
					resultNotInit = false;
				}
				else result.addAll(getSuitableSubstances(s.backwardTo, conditions, forceConditions, categories));
			} if (type == NodeRelationType.BACKWARD && form == NodeRelationForm.TO) {
				if (resultNotInit) {
					result = getSuitableSubstances(s.towardTo, conditions, forceConditions, categories);
					resultNotInit = false;
				}
				else result.addAll(getSuitableSubstances(s.towardTo, conditions, forceConditions, categories));
			} if (type == NodeRelationType.TOWARD && form == NodeRelationForm.WITH) {
				if (resultNotInit) {
					result = getSuitableSubstances(s.towardWith, conditions, forceConditions, categories);
					resultNotInit = false;
				}
				else result.addAll(getSuitableSubstances(s.towardWith, conditions, forceConditions, categories));
			} if (type == NodeRelationType.BACKWARD && form == NodeRelationForm.WITH) {
				if (resultNotInit) {
					result = getSuitableSubstances(s.backwardWith, conditions, forceConditions, categories);
					resultNotInit = false;
				}
				else result.addAll(getSuitableSubstances(s.backwardWith, conditions, forceConditions, categories));
			}
		}
		return result;
	}
	
	public static Set<Substance> getSuitableSubstances(Set<Reaction> reactions, Set<String> conditions, 
			boolean forceConditions, Set<String> categories) {
		Set<Substance> result = new HashSet<>();
		for (Reaction r : reactions) {
			if (forceConditions && !conditions.equals(r.conditions)) continue;
			if (!forceConditions && !conditions.contains("(Any)")) {
				boolean toContinue = false;
				for (String c : conditions) {
					if (!r.conditions.contains(c)) toContinue = true;
				}
				if (toContinue) continue;
			}
			boolean toContinue = false;
			for (String c : categories) {
				if (!r.categories.contains(c)) toContinue = true;
			}
			if (toContinue) continue;
			result.add(r.substance);
		}
		return result;
	}
}
