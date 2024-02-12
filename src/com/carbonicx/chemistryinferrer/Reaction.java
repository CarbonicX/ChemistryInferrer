package com.carbonicx.chemistryinferrer;

import java.util.Set;

public class Reaction {
	public Substance substance;
	public Set<String> conditions;
	public Set<String> categories;
	
	public Reaction(Substance substance, Set<String> conditions, Set<String> categories) {
		this.substance = substance;
		this.conditions = conditions;
		this.categories = categories;	
	}
	
	public Reaction(String ionA, String ionB, Set<String> conditions, Set<String> categories) throws Exception {
		this.substance = Program.substances.get(Loader.getSubstanceNameByIons(ionA, ionB));
		this.conditions = conditions;
		this.categories = categories;
	}
}