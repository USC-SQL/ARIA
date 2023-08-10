package usc.edu.sql.fpa.analysis.bundle;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import usc.edu.sql.fpa.model.CodePoint;
import usc.edu.sql.fpa.model.Component;
import usc.edu.sql.fpa.model.IC3Intent;
import usc.edu.sql.fpa.model.IntentIR;
import usc.edu.sql.fpa.utils.Constants.COMPONENT;
import usc.sql.string.utils.Util;

public class INode implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private COMPONENT componentType;
	private String methodSig;
	private String unitStr;
	private int bytecodeOffset;
	private int unitIndex;
	private Component component;
	private String app;
	private boolean isSendingPoint = false;
	private boolean isreceivingPoint = false;
	private Set<IntentIR> irIntents;
	private Set<IC3Intent> interpretedIntents;
	private Map<IntentIR, Set<IC3Intent>> irToConcerete = new HashMap<>();
	private Set<INode> preds = new HashSet<INode>();
	private Set<INode> succ = new HashSet<INode>();
	private String unitInvokeExprSubSig;
	private String pkg;

	public INode(boolean isReceiving) {
		this.isreceivingPoint = isReceiving;
	}

	public INode(Set<IntentIR> intents, boolean isReceiving) {
		this.isreceivingPoint = isReceiving;
		if (intents != null)
			irIntents = new HashSet<IntentIR>(intents);
		else
			irIntents = new HashSet<IntentIR>();
		interpretedIntents = new HashSet<IC3Intent>();
	}

	public void clearInterpretedIntents() {
		interpretedIntents.clear();
	}

	public void addInterpretedIntent(IC3Intent i) {
		interpretedIntents.add(i);
	}

	public void removeInterpretedIntent(IC3Intent i) {
		interpretedIntents.remove(i);

	}

	public void addAllInterpreted(Set<IC3Intent> interepted) {
		interpretedIntents.addAll(interepted);
	}

	public void addAllMappings(Set<IC3Intent> interepted, IntentIR ir) {
		irToConcerete.put(ir, interepted);
	}
	
	public Map<IntentIR, Set<IC3Intent>> getAllMappings() {
		return irToConcerete;
	}

	public void removeSucc(INode n) {
		succ.remove(n);

	}

	public void removePred(INode n) {
		preds.remove(n);

	}

	public void addSucc(INode n) {
		succ.add(n);
	}

	public void addPred(INode n) {
		preds.add(n);
	}

	public Set<IntentIR> getIrIntents() {
		return irIntents;
	}

	public Set<IC3Intent> getInterpretedIntents() {
		return interpretedIntents;
	}

	public Set<INode> getPreds() {
		return preds;
	}

	public Set<INode> getSuccs() {
		return succ;
	}

	public String getUnitStr() {
		return unitStr;
	}

	public void setUnitStr(String unitStr) {
		this.unitStr = unitStr;
	}

	public boolean isSendingPoint() {
		return isSendingPoint;
	}

	public Component getComponent() {
		return component;
	}

	public COMPONENT getComponentType() {
		return componentType;
	}

	public String getInvokedExpr() {
		return unitInvokeExprSubSig;
	}

	public String getMethod() {
		return methodSig;
	}

	public String getApp() {
		return app;
	}

	public void setApp(String app) {
		this.app = app;

	}

	public void setComponentType(COMPONENT type) {
		this.componentType = type;

	}

	public void setComponent(Component component2) {
		this.component = component2;

	}

	public void setInvokeExpr(String subSignature) {
		this.unitInvokeExprSubSig = subSignature;

	}

	public void setOffset(int offset) {
		this.bytecodeOffset = offset;

	}

	public int getOffset() {
		return this.bytecodeOffset;

	}

	public String getPackage() {
		return pkg;
	}

	public void setPackage(String pkg) {
		this.pkg = pkg;

	}

	public void setMethod(String signature) {
		this.methodSig = signature;

	}

	public void setUnitIndex(int id) {
		unitIndex = id;

	}

	public int getUnitIndex() {
		return unitIndex;

	}

	public boolean isReceivingPoint() {
		return isreceivingPoint;
	}

	public boolean containsCodePoint(CodePoint definitionPoint) {
		if (definitionPoint.getMethod().getSignature().equals(this.getMethod()))
			if (Util.getBytecodeOffset(definitionPoint.getUnit()) == bytecodeOffset)
				return true;
		return false;
	}

	@Override
	public String toString() {
		String s = app + " " + methodSig + " " + bytecodeOffset + "  " + unitIndex + " " + unitStr + ": \n ";
		if (unitInvokeExprSubSig != null)
			s += unitInvokeExprSubSig;
		return s;
	}

}
