package com.carbonicx.chemistryinferrer;

import java.util.HashSet;
import java.util.Set;

// ����
public class Substance {
	public String name;
	
	public Set<Reaction> towardTo = new HashSet<>();
	public Set<Reaction> towardWith = new HashSet<>();
	public Set<Reaction> backwardTo = new HashSet<>();
	public Set<Reaction> backwardWith = new HashSet<>();
	
	public Substance(String name) {
		this.name = name;
	}

	// ͨ��ԭ�ڵ��ṩ�� NodeRelationType �� NodeRelationForm �ṩ���ܵ�����
	// ����ͨ��Ŀ��ڵ㷴��ԭ�ڵ�
	// ע�⣺possibles ��Ŀ��ڵ�ģ�������ԭ�ڵ�ģ�NodeRelationType �� NodeRelationForm ��ԭ�ڵ��
	// N ���� X (N TOWARD, to X) �� X �� N ���� (X BACKWARD, to N) ��Ҫ
	// N �� X ��Ӧ (N TOWARD, with X) �� X �� N ��Ӧ (X TOWARD, with N) ��Ҫ
	// N �� X ��ͬ������ (N BACKWARD, with X) �� X �� N ��ͬ������ (X BACKWARD, with N) ��Ҫ
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
