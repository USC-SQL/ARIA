package usc.edu.sql.fpa.analysis.intra;

import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import usc.edu.sql.fpa.model.CodePoint;
import usc.edu.sql.fpa.utils.AnalysisUtils;
import usc.edu.sql.fpa.utils.Constants;
import usc.edu.sql.fpa.utils.IntentUtils;
import usc.edu.sql.fpa.utils.Constants.ATTRIBUTE;
import usc.edu.sql.fpa.utils.Constants.INVOCATION_TYPE;

import java.util.*;
import java.util.Map.Entry;

import org.hamcrest.core.IsSame;

public class IntentReachability {

	private SootMethod currentMethod;
	// private IType argType;
	private final List<RNode> allRNode = new ArrayList<>();
	// a node mapping to the nodes it can reach
	private final Map<Unit, Set<IType>> rTable = new HashMap<>();
	Map<String, Map<Unit, Set<IType>>> sigToSummary;

	public IntentReachability(UnitGraph cfg, Map<String, Map<Unit, Set<IType>>> sigToSummary) {
		currentMethod = cfg.getBody().getMethod();
		this.sigToSummary = sigToSummary;
		for (Unit n : cfg.getBody().getUnits()) {
			allRNode.add(new RNode(n));

			rTable.put(n, new HashSet<>());
		}
		try {
			computeReachability(cfg);
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}

	private void initialize(UnitGraph cfg) throws CloneNotSupportedException {
		for (RNode rn : allRNode) {
			if (IntentUtils.getIntentDefinition(rn.getNode()) != null) {
				if (AnalysisUtils.isCast(rn.getNode()))
					continue;
				CodePoint point = new CodePoint(rn.getNode(), currentMethod, Constants.APK_PATH);
				IType newIType = new IType(point);
				newIType.iattrsMap.put(ATTRIBUTE.DEF, new HashSet<>());
				newIType.iattrsMap.get(ATTRIBUTE.DEF).add(point);
				if (IntentUtils.isReceivedPoint(rn.getNode())) {
					IntentUtils.updateWithReceivingInfo(newIType, point);
					rn.getGenSet().add(newIType);
				} else if (IntentUtils.isParameterUnit(rn.getNode())) {
					IntentUtils.updateWithCallerInfo(newIType, point);
					rn.getGenSet().add(newIType);
				} else if (AnalysisUtils.getInvoke(rn.getNode()) != null) {

					rn.getGenSet().addAll(updateWithReturnInfo(newIType, rn.getNode()));
				} else {
					rn.getGenSet().add(newIType);
				}
//				}

			} else if (IntentUtils.getParcelableDefinition(rn.getNode()) != null) {
				CodePoint point = new CodePoint(rn.getNode(), currentMethod, Constants.APK_PATH);
				IType newIType = new IType(point);
				newIType.iattrsMap.put(ATTRIBUTE.DEF, new HashSet<>());
				newIType.iattrsMap.get(ATTRIBUTE.DEF).add(point);
				IntentUtils.updateWithReceivingInfo(newIType, point);
				rn.getGenSet().add(newIType);
			}

		}
	}

	private boolean compareTwoSet(Set<IType> oldset, Set<IType> newset) {
		return oldset.containsAll(newset) && newset.containsAll(oldset);
	}

	private void computeReachability(UnitGraph cfg) throws CloneNotSupportedException {
		initialize(cfg);
		Map<RNode, List<RNode>> preList = new HashMap<>();
		// Create predecessor list for each node
		for (RNode node : allRNode) {
			List<RNode> nodePre = new ArrayList<>();
			for (Unit pre : cfg.getPredsOf(node.getNode())) {
				for (RNode n : allRNode)
					if (n.getNode().equals(pre))
						nodePre.add(n);

			}
			preList.put(node, nodePre);

		}
		Map<RNode, Set<IType>> old = new HashMap<>();
		for (RNode rn : allRNode) {
			Set<IType> s = new HashSet<>();
			old.put(rn, s);
		}
		boolean needtoloop = true;

		while (needtoloop) {
			needtoloop = false;
			for (RNode rn : allRNode) {
				// Union the out set of all the nodes in the predecessor
				// list，add to the in set of node rn

				for (RNode node : preList.get(rn)) {
					for (IType outnode : node.getOutSet())
						rn.getInSet().add(outnode);
				}
				// Union the gen set and in set to the out set of node rn
				for (IType gennode : rn.getGenSet()) {
					rn.getOutSet().add(gennode);
				}

				for (IType inIType : rn.getInSet()) {
					Set<IType> newSet = new HashSet<IType>();
					boolean isKilled = false;
					ATTRIBUTE attr = IntentUtils.analyzeUnit(rn.getNode());
					Value intentRef = IntentUtils.getIntentBase(rn.getNode());
					if (attr != null) {
						IType newIType = inIType.clone();
						if (attr.equals(ATTRIBUTE.CAST)) {
							SimpleLocalDefs defs = AppInferrer.getInstance(Constants.APK_PATH).getDefs(currentMethod);
							List<Unit> valDefs = defs.getDefsOfAt((Local) AnalysisUtils.getCastValue(rn.getNode()),
									rn.getNode());
							if (valDefs.contains(inIType.getDefinitionPoint().getUnit())) {
								isKilled = true;
								newIType.definiStmt = new CodePoint(rn.getNode(), currentMethod, Constants.APK_PATH);
							}
						} else if (AnalysisUtils.isSameReference(inIType.getDefinitionPoint(), intentRef, rn.getNode(),
								currentMethod)) {
							isKilled = true;
							if (ATTRIBUTE.DATA_TYPE.equals(attr)) {
								IntentUtils.updateAttributes(newIType, ATTRIBUTE.DATA, rn.getNode(), currentMethod, 0);
								IntentUtils.updateAttributes(newIType, ATTRIBUTE.TYPE, rn.getNode(), currentMethod, 1);

							} else if (attr.equals(ATTRIBUTE.INIT2_EXPLICIT) || attr.equals(ATTRIBUTE.CLASS)) {
								IntentUtils.updateAttributes(newIType, ATTRIBUTE.COMPONENT, rn.getNode(), currentMethod,
										1);
								IntentUtils.updateAttributes(newIType, ATTRIBUTE.PACKAGE, rn.getNode(), currentMethod,
										0);

							} else if (attr.equals(ATTRIBUTE.INIT1_ACTION)) {
								IntentUtils.updateAttributes(newIType, ATTRIBUTE.ACTION, rn.getNode(), currentMethod,
										0);

							} else if (attr.equals(ATTRIBUTE.INIT2_IMPLICIT)) {
								IntentUtils.updateAttributes(newIType, ATTRIBUTE.ACTION, rn.getNode(), currentMethod,
										0);
								IntentUtils.updateAttributes(newIType, ATTRIBUTE.DATA, rn.getNode(), currentMethod, 1);

							} else if (attr.equals(ATTRIBUTE.INIT4)) {
								IntentUtils.updateAttributes(newIType, ATTRIBUTE.ACTION, rn.getNode(), currentMethod,
										0);
								IntentUtils.updateAttributes(newIType, ATTRIBUTE.DATA, rn.getNode(), currentMethod, 1);
								IntentUtils.updateAttributes(newIType, ATTRIBUTE.COMPONENT, rn.getNode(), currentMethod,
										3);
								IntentUtils.updateAttributes(newIType, ATTRIBUTE.PACKAGE, rn.getNode(), currentMethod,
										2);

							} else if (attr.equals(ATTRIBUTE.EXTRA)) {
								String key = IntentUtils.getPutExtraKey(rn.getNode());
								if (newIType.hasExtraKey(key)) {
									Set<CodePoint> cps = newIType.getExtraValueForKey(key);
									for (CodePoint cp : cps) {
										newIType.iattrsMap.get(ATTRIBUTE.EXTRA).remove(cp);
									}
								}
								if (key != null) {
									Set<CodePoint> temp = new HashSet<CodePoint>();
									temp.add(new CodePoint(rn.getNode(), currentMethod, Constants.APK_PATH));
									newIType.extras.put(key, temp);
								}
								IntentUtils.updateAttributes(newIType, attr, rn.getNode(), currentMethod, 0);
							} else {
								IntentUtils.updateAttributes(newIType, attr, rn.getNode(), currentMethod, 0);
								if (rn.getNode().toString().contains("setComponent")
										&& attr.equals(ATTRIBUTE.COMPONENT)) {
									IntentUtils.updateAttributes(newIType, ATTRIBUTE.PACKAGE, rn.getNode(),
											currentMethod, 0);
								}
							}
						}
						newSet.add(newIType);
						if (!rn.getGenSet().isEmpty()) {
							rn.getOutSet().removeAll(rn.getGenSet());
							rn.getGenSet().clear();
							IType gtype = newIType.clone();
							gtype.definiStmt = new CodePoint(rn.getNode(), currentMethod, Constants.APK_PATH);
							rn.getGenSet().add(gtype);
							rn.getOutSet().addAll(rn.getGenSet());
						}
					}
					INVOCATION_TYPE ink = AnalysisUtils.unitNeedsAnalysis(rn.getNode());

					if (INVOCATION_TYPE.INTENT_ARGUMENT.equals(ink)) {
						newSet.addAll(updatedInItypeForArgument(inIType, rn.getNode()));
						isKilled = true;
					}

					if (!isKilled) {
						rn.getOutSet().add(inIType);
					} else {
						rn.getOutSet().addAll(newSet);
					}

				}

				if (!compareTwoSet(old.get(rn), rn.getOutSet())) {
					needtoloop = true;
					Set<IType> s = new HashSet<>(rn.getOutSet());
					old.put(rn, s);

				}
			}
		}

		for (RNode rn : allRNode) {
			for (IType n : rn.getInSet()) {
				rTable.get(rn.getNode()).add(n);
			}
		}
	}

	private Set<IType> updateWithReturnInfo(IType newType, Unit u) throws CloneNotSupportedException {
		Set<IType> res = new HashSet<IType>();
		CodePoint cp = new CodePoint(u, currentMethod, Constants.APK_PATH);
		SootMethod sm = AnalysisUtils.getInvokedMethod(u);
		if (sm.toString().contains("createChooser")) {
			newType.iattrsMap.put(ATTRIBUTE.ACTION, new HashSet<CodePoint>());
			newType.iattrsMap.get(ATTRIBUTE.ACTION).add(cp);
			res.add(newType);
			return res;
		}
		if (sm.toString().contains(
				"android.content.pm.PackageManager: android.content.Intent getLaunchIntentForPackage(java.lang.String)")) {
			newType.iattrsMap.put(ATTRIBUTE.PACKAGE, new HashSet<CodePoint>());
			cp.setArgIndex(0);
			newType.iattrsMap.get(ATTRIBUTE.PACKAGE).add(cp);
			newType.iattrsMap.put(ATTRIBUTE.CATEGOR, new HashSet<CodePoint>());
			newType.iattrsMap.get(ATTRIBUTE.CATEGOR).add(cp);
			res.add(newType);
			return res;
		}
		if (!sm.isConcrete()) {
			res.add(newType);
			return res;
		}
		if (!sigToSummary.containsKey(sm.getSignature())) {
			AppInferrer.getInstance(Constants.APK_PATH).getITypeReachableSet(sm);
		}
		for (Entry<Unit, Set<IType>> en : sigToSummary.get(sm.getSignature()).entrySet()) {
			if (en.getKey() instanceof ReturnStmt) {
				Value val = ((ReturnStmt) en.getKey()).getOp();
				for (IType i : en.getValue()) {
					if (AnalysisUtils.isSameReference(i.getDefinitionPoint(), val, en.getKey(), sm)) {
						IType r = i.clone();
						r.definiStmt = cp;
						res.add(r);
					}
				}

			}
		}
		return res;
	}

	private Set<IType> updatedInItypeForArgument(IType inIType, Unit unit) {
		Set<IType> res = new HashSet<IType>();
		SootMethod sm = AnalysisUtils.getInvokedMethod(unit);
		int i = getIntentValueArgIndexSameInTypeReference(inIType, unit);
		if (i == -1 || !sm.isConcrete()) {
			res.add(inIType);
			return res;
		}
		AppInferrer.getInstance(Constants.APK_PATH).getITypeReachableSet(sm);
		for (Entry<Unit, Set<IType>> en : sigToSummary.get(sm.getSignature()).entrySet()) {

			for (IType it : en.getValue()) {
				if (!IntentUtils.isParamAndArgMatch(it, i)) {
					continue;
				}
				try {
					IType c = inIType.clone();
					for (ATTRIBUTE attr : it.iattrsMap.keySet()) {
						IntentUtils.updateAttributes(c, attr, it.iattrsMap.get(attr), i);
					}
					res.add(c);
				} catch (CloneNotSupportedException e) {
					res.add(inIType);
				}
			}

		}
		if (res.isEmpty())
			res.add(inIType);
		return res;
	}

	private int getIntentValueArgIndexSameInTypeReference(IType inIType, Unit unit) {
		InvokeExpr expr = ((Stmt) unit).getInvokeExpr();
		for (int i = 0; i < expr.getArgCount(); i++) {
			if (IntentUtils.isIntent(expr.getArg(i))) {
				if (AnalysisUtils.isSameReference(inIType.getDefinitionPoint(), expr.getArg(i), unit, currentMethod)) {
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * Return a map mapping a node to the nodes it can reach.
	 */
	public Map<CodePoint, Set<IType>> getReachableTable() {
		Map<CodePoint, Set<IType>> res = new HashMap<>();
		for (Unit uk : rTable.keySet()) {

			res.put(new CodePoint(uk, currentMethod, Constants.APK_PATH), rTable.get(uk));
		}
		return res;
	}

}

class RNode {
	private Unit node;
	private Set<IType> inSet = new HashSet<>();
	private Set<IType> outSet = new HashSet<>();
	private Set<IType> genSet = new HashSet<>();

	public RNode(Unit node) {
		this.node = node;
	}

	public Unit getNode() {
		return node;
	}

	public void setNode(Unit node) {
		this.node = node;
	}

	public Set<IType> getInSet() {
		return inSet;
	}

	public void setInSet(Set<IType> inSet) {
		this.inSet = inSet;
	}

	public Set<IType> getOutSet() {
		return outSet;
	}

	public void setOutSet(Set<IType> outSet) {
		this.outSet = outSet;
	}

	public Set<IType> getGenSet() {
		return genSet;
	}

	public void setGenSet(Set<IType> genSet) {
		this.genSet = genSet;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return node.toString();
	}

}