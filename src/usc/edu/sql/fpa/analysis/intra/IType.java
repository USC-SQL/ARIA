package usc.edu.sql.fpa.analysis.intra;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.Stmt;
import usc.edu.sql.fpa.model.CodePoint;
import usc.edu.sql.fpa.utils.Constants.ATTRIBUTE;

public class IType {
	public Value reference;
	public Map<ATTRIBUTE, Set<CodePoint>> iattrsMap = new HashMap<>();
	public Map<String, Set<CodePoint>> extras = new HashMap<>();
	CodePoint definiStmt;

	public IType(CodePoint def) {
		definiStmt = def;
	}

//	public IType updateCopy(IType genIType) {
//		IType iType = new IType(reference);
//		iType.iattrsMap.putAll(iattrsMap);
//		for (ATTRIBUTE attr : genIType.iattrsMap.keySet()) {
//
//			iType.iattrsMap.put(attr, new HashSet<CodePoint>());
//			Set<CodePoint> updatedAttrInfo = genIType.iattrsMap.get(attr);
//			iType.iattrsMap.get(attr).addAll(updatedAttrInfo);
//		}
//		return iType;
//	}

	public boolean isExplicit() {
		if (iattrsMap.containsKey(ATTRIBUTE.COMPONENT))
			return true;
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;

		if (!(o instanceof IType))
			return false;
		IType ii = (IType) o;
//		return ii.iattrsMap.equals(this.iattrsMap);
		for (ATTRIBUTE attr : this.iattrsMap.keySet()) {
			if (!ii.iattrsMap.containsKey(attr))
				return false;

			for (CodePoint cp : this.iattrsMap.get(attr)) {
				if (!ii.iattrsMap.get(attr).contains(cp))
					return false;
			}

			for (CodePoint cp : ii.iattrsMap.get(attr)) {
				if (!this.iattrsMap.get(attr).contains(cp))
					return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public String toString() {
		String s = "";
		for (ATTRIBUTE attr : iattrsMap.keySet()) {
			s += attr + ": " + iattrsMap.get(attr) + "\n";
		}
		return s;
	}

	public CodePoint getDefinitionPoint() {
		return definiStmt;
	}

	@Override
	protected IType clone() throws CloneNotSupportedException {
		IType i = new IType(this.definiStmt);
		i.iattrsMap = new HashMap<ATTRIBUTE, Set<CodePoint>>();
		for (ATTRIBUTE attr : this.iattrsMap.keySet()) {
			i.iattrsMap.put(attr, new HashSet<CodePoint>(this.iattrsMap.get(attr)));
		}
		i.extras = new HashMap<String, Set<CodePoint>>();
		for (String key : this.extras.keySet()) {
			i.extras.put(key, new HashSet<CodePoint>(this.extras.get(key)));
		}
		return i;
	}

	public boolean hasExtraKey(String key) {
		if (extras.containsKey(key))
			return true;
		return false;
	}

	public Set<CodePoint> getExtraValueForKey(String key) {
		if (hasExtraKey(key)) {
			return extras.get(key);
		}
		return null;
	}
}
