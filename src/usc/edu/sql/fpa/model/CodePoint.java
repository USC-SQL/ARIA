package usc.edu.sql.fpa.model;

import soot.SootMethod;
import soot.Unit;
import soot.Value;

import java.io.Serializable;

public class CodePoint{

	private Unit unit;
	private SootMethod method;
	private int argumentIndex = -1;
	private Value attrReference;
	private Value intentReference;
	private ICCResponsibleComponent component;
	private String app;

	public CodePoint(Unit unit, SootMethod method, String app) {
		this.unit = unit;
		this.method = method;
		this.app = app;
	}

	public CodePoint(Unit unit, SootMethod method, String app, ICCResponsibleComponent component) {
		this.unit = unit;
		this.method = method;
		this.app = app;
		this.setComponent(component);
	}

	public Value getIntentReference() {
		return intentReference;
	}

	public void setIntentReference(Value reference) {
		this.intentReference = reference;
	}

	public Value getAttrReference() {
		return attrReference;
	}

	public void setAttrReference(Value reference) {
		this.attrReference = reference;
	}

	public Unit getUnit() {
		return unit;
	}

	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public SootMethod getMethod() {
		return method;
	}

	public void setMethod(SootMethod method) {
		this.method = method;
	}

	public int getArgIndex() {
		return argumentIndex;
	}

	public void setArgIndex(int argumentIndex) {
		this.argumentIndex = argumentIndex;
	}

	public ICCResponsibleComponent getComponent() {
		return component;
	}

	public void setComponent(ICCResponsibleComponent component2) {
		this.component = component2;
	}

	public String getApp() {
		return this.app;
	}

	@Override
	public String toString() {

		return method.toString() + "\n" + unit.toString() + "\n";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result + ((unit == null) ? 0 : unit.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof CodePoint))
			return false;
		CodePoint other = (CodePoint) obj;
		if (method == null) {
			if (other.method != null)
				return false;
		} else if (!method.equals(other.method))
			return false;
		if (unit == null) {
			return other.unit == null;
		} else
			return unit.equals(other.unit);
	}

}
