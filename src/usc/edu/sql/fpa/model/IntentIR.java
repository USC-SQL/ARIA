package usc.edu.sql.fpa.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import usc.edu.sql.fpa.analysis.IRAnalysis;
import usc.edu.sql.fpa.utils.Constants.ATTRIBUTE;
import usc.sql.ir.Variable;

public class IntentIR implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// private CodePoint definitionPoint;
	private int bytecodeOffset;
	private String methodSig;
	public Map<ATTRIBUTE, Set<Variable>> iattrsMap = new HashMap<>();
	public Map<Variable, Set<Variable>> extraMap = new HashMap<>();
	public String appName;

	public IntentIR(int offset, String method) {
		this.bytecodeOffset = offset;
		this.methodSig = method;
	}

	public IntentIR(IntentIR i) {
		// this.definitionPoint = i.definitionPoint;
		this.iattrsMap = new HashMap<>();

		for (ATTRIBUTE attr : i.iattrsMap.keySet()) {
			this.iattrsMap.put(attr, new HashSet<>(i.iattrsMap.get(attr)));
		}
		this.extraMap = new HashMap<>(i.extraMap);
	}

	@Override
	public String toString() {
		return iattrsMap.toString() + " " + extraMap.toString();
	}

	public void addAttr(ATTRIBUTE attr, Variable v) {
		if (attr.equals(ATTRIBUTE.CATEGOR)) {
			if (this.iattrsMap.get(attr) == null)
				this.iattrsMap.put(attr, new HashSet<>());
			this.iattrsMap.get(attr).add(v);

		} 
		else {
			Set<Variable> set = new HashSet<Variable>();
			set.add(v);
			this.iattrsMap.put(attr, set);
		}
	}

	public void addExtra(Variable key, Variable value) {
		if (!extraMap.containsKey(key))
			extraMap.put(key, new HashSet<Variable>());
		extraMap.get(key).add(value);
	}

	public Set<ATTRIBUTE> dependsOnIncomingIntent() {
		Set<ATTRIBUTE> res = new HashSet<>();
		for (ATTRIBUTE attr : iattrsMap.keySet()) {
			for (Variable v : iattrsMap.get(attr)) {
				if (IRAnalysis.dependsOnIntentOp(v))
					res.add(attr);
			}
		}
		for (Variable key : extraMap.keySet()) {
			if (IRAnalysis.dependsOnIntentOp(key))
				res.add(ATTRIBUTE.EXTRA);
			for (Variable v : extraMap.get(key)) {
				if (IRAnalysis.dependsOnIntentOp(v))
					res.add(ATTRIBUTE.EXTRA);
			}

		}
		return res;
	}

}
