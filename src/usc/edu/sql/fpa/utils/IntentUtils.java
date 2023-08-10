package usc.edu.sql.fpa.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import polyglot.ast.Assign;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.ClassConstant;
import soot.jimple.DefinitionStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.Pair;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.UnitValueBoxPair;
import usc.edu.sql.fpa.analysis.bundle.INode;
import usc.edu.sql.fpa.analysis.intra.IType;
import usc.edu.sql.fpa.model.CodePoint;
import usc.edu.sql.fpa.model.IC3Intent;
import usc.edu.sql.fpa.model.Intent;
import usc.edu.sql.fpa.utils.Constants.API_TYPE;
import usc.edu.sql.fpa.utils.Constants.ATTRIBUTE;
import usc.edu.sql.fpa.utils.Constants.INVOCATION_TYPE;
import usc.sql.ir.ConstantString;

public class IntentUtils {

//	public static Map<CodePoint, Set<Intent>> getRelevantIntents(IType itype,
//			Map<CodePoint, Set<Intent>> intentSummaries) {
//		Map<CodePoint, Set<Intent>> relevantIntents = new HashMap<CodePoint, Set<Intent>>();
//		for (CodePoint reachable : itype.iattrsMap.get(ATTRIBUTE.DEF)) {
//			if (((Stmt) reachable.getUnit()).containsInvokeExpr()) {
//				SootMethod invokeMethod = ((Stmt) reachable.getUnit()).getInvokeExpr().getMethod();
//				for (CodePoint um : intentSummaries.keySet()) {
//					if (um.getUnit() instanceof ReturnStmt && um.getMethod().equals(invokeMethod)) {
//						if (!relevantIntents.containsKey(reachable))
//							relevantIntents.put(reachable, new HashSet<Intent>());
//						relevantIntents.get(reachable).addAll(intentSummaries.get(um));
//					}
//				}
//			}
//		}
//		return relevantIntents;
//
//	}

//	public static void updateParamBasedSummaries(Unit unit, SootMethod method,
//			Map<CodePoint, Set<Intent>> intentSummaries) {
//		InvokeExpr invokeExpr = AnalysisUtils.getInvoke(unit);
//		if (invokeExpr == null)
//			return;
//		Map<Intent, Map<Intent, Set<Set<CodePoint>>>> result = new HashMap<>();
//
//		for (Set<Intent> intents : intentSummaries.values()) {
//			for (Intent intent : intents) {
//				boolean isUpdated = false;
//				for (ATTRIBUTE attr : intent.itype.iattrsMap.keySet()) {
//					Map<ATTRIBUTE, Set<CodePoint>> map = new HashMap<>(intent.itype.iattrsMap);
//					for (CodePoint um : map.get(attr)) {
//						if (invokeExpr.getMethod().equals(um.getMethod())
//								&& AnalysisUtils.isDeveloperMethod(um.getMethod())) {
//							int paramInd = AnalysisUtils.getParameterIndex(um.getUnit());
//							if (paramInd != -1) {
//								intent.itype.iattrsMap.get(attr).remove(um);
//								CodePoint point = new CodePoint(unit, method);
//								point.setArgIndex(paramInd);
//								intent.itype.iattrsMap.get(attr).add(point);
//								isUpdated = true;
//							}
//						}
//					}
//				}
//				if (isUpdated)
//					result.put(intent, new IntentGenerator().generate(intent.itype, method, unit,
//							intent.originComponent, getRelevantIntents(intent.itype, intentSummaries)));
//			}
//		}
//		updateIntentSummaries(intentSummaries, result);
//
//	}
//
//	private static void updateIntentSummaries(Map<CodePoint, Set<Intent>> intentSummaries,
//			Map<Intent, Map<Intent, Set<Set<CodePoint>>>> result) {
//		for (CodePoint cp : intentSummaries.keySet()) {
//			Set<Intent> old = new HashSet<Intent>(intentSummaries.get(cp));
//			for (Intent oldIntent : old) {
//				if (result.containsKey(oldIntent)) {
//					Set<Intent> news = new HashSet<Intent>(result.get(oldIntent).keySet());
//					news.addAll(intentSummaries.get(cp));
//					news.remove(oldIntent);
//					intentSummaries.put(cp, news);
//
//				}
//			}
//
//		}
//
//	}

	public static IC3Intent generateLauncherNode(INode rNode) {
		IC3Intent in = new IC3Intent();
		in.setAction(Constants.MAIN_ACTION);
		in.addCategory(Constants.MAIN_CATEGORY);
		in.setComponent(rNode.getComponent().getName());
		// in.iattrsMap.put(ATTRIBUTE.COMPONENT, Set.of(new
		// ConstantString(rNode.getComponent().getName())));

		return in;
	}

	public static void addAttr2Map(ATTRIBUTE attr, Set<Map<ATTRIBUTE, Set<CodePoint>>> iattrsMaps, CodePoint rUnit) {
		Set<Map<ATTRIBUTE, Set<CodePoint>>> clonedMapNeeded = new HashSet<>();
		for (Map<ATTRIBUTE, Set<CodePoint>> map : iattrsMaps) {
			if (map.containsKey(attr) && !attr.equals(ATTRIBUTE.CATEGOR)) {
				clonedMapNeeded.add(new HashMap<>(map));
			}
			if (!attr.equals(ATTRIBUTE.CATEGOR) || !map.containsKey(attr)) {
				map.put(attr, new HashSet<CodePoint>());
				map.get(attr).add(rUnit);
			}
			if (attr.equals(ATTRIBUTE.CATEGOR)) {
				map.get(attr).add(rUnit);
			}

		}
		iattrsMaps.addAll(clonedMapNeeded);

	}

	public static boolean isReceivedPoint(Unit unit) {
		if (!(unit instanceof DefinitionStmt))
			return false;

		DefinitionStmt defStmt = (DefinitionStmt) unit;
		if (!(defStmt.getRightOp() instanceof InstanceInvokeExpr)) {
			return false;
		}
		InstanceInvokeExpr expr = ((InstanceInvokeExpr) defStmt.getRightOp());

		if (INVOCATION_TYPE.RECEIVING.equals(AnalysisUtils.unitNeedsAnalysis(unit)))
			return true;

		return false;
	}

	public static ATTRIBUTE analyzeUnit(Unit unit) {
		if (isIntentCast(unit)) {
			return ATTRIBUTE.CAST;
		}
		if (!isIntentObjectInvoked(unit)) {
			return null;
		}
		if (getAPIType((Stmt) unit).equals(API_TYPE.ADD) || getAPIType((Stmt) unit).equals(API_TYPE.SET)
				|| getAPIType((Stmt) unit).equals(API_TYPE.INIT)) {
			return getAttr((Stmt) unit);
		}
		return null;

	}

	public static API_TYPE getAPIType(Stmt stmt) {
		if (!stmt.containsInvokeExpr())
			return null;
		String method = stmt.getInvokeExpr().getMethod().getName();

		if (isAPIType(method, Constants.GET_KEYWORDS, API_TYPE.GET))
			return API_TYPE.GET;
		if (isAPIType(method, Constants.SET_KEYWORDS, API_TYPE.SET))
			return API_TYPE.SET;
		if (isAPIType(method, Constants.ADD_KEYWORDS, API_TYPE.ADD))
			return API_TYPE.ADD;
		if (isAPIType(method, Constants.HAS_KEYWORDS, API_TYPE.HAS))
			return API_TYPE.HAS;
		if (method.equals("<init>"))
			return API_TYPE.INIT;
		return Constants.API_TYPE.OTHER;
	}

	public static boolean isAPIType(String method, List<String> KEYWORDS, API_TYPE type) {
		for (String key : KEYWORDS) {
			if (method.startsWith(key)) {
				return true;
			}
		}
		return false;
	}

	public static ATTRIBUTE getAttr(Stmt sm) {
		if (!sm.containsInvokeExpr()) {
			return null;
		}
		InvokeExpr invoke = sm.getInvokeExpr();

//		if (invoke.getMethod().getName().equals("createChooser"))
//			return ATTRIBUTE.CHOOSER;
//		if (invoke.getMethod().getName().equals("getIntent"))
//			return ATTRIBUTE.GETINTENT;

		if (invoke.getMethod().getName().equals("<init>")) {
			if (invoke.getArgCount() > 2) {
				return ATTRIBUTE.INIT4;
			}
			if (invoke.getArgCount() == 2) {
				if (invoke.getArg(0).getType().toString().equals("java.lang.String"))
					return ATTRIBUTE.INIT2_IMPLICIT;
				return ATTRIBUTE.INIT2_EXPLICIT;
			}

			if (invoke.getArgCount() == 1) {
				if (invoke.getArg(0).getType().toString().equals("java.lang.String"))
					return ATTRIBUTE.INIT1_ACTION;
				return ATTRIBUTE.INIT1_CLONE;
			}

		}
		if (invoke.getMethod().getName().contains("setDataAndType"))
			return ATTRIBUTE.DATA_TYPE;

		if (invoke.getMethod().getName().contains("setClass"))
			return ATTRIBUTE.CLASS;

		if (invoke.getMethod().getName().contains("putExtra"))
			return ATTRIBUTE.EXTRA;

		if (invoke.getMethod().getName().contains("setRes"))
			return ATTRIBUTE.RES;
		for (ATTRIBUTE attr : ATTRIBUTE.values()) {
			if (invoke.getMethod().getName().toLowerCase().contains(attr.toString().toLowerCase())) {
				return attr;
			}
		}

		return null;
	}

	public static Set<usc.sql.ir.Variable> getAppPackageIR() {
		Set<usc.sql.ir.Variable> ir = new HashSet<>();
		for (String str : Parser.getInstance(Constants.APK_PATH).getPkg()) {
			ir.add(new ConstantString(str));
		}
		return ir;

	}

	public static boolean isIntentObjectInvoked(Unit unit) {
		if (((Stmt) unit).containsInvokeExpr()) {
			InvokeExpr invkExpr = ((Stmt) unit).getInvokeExpr();

			if (invkExpr instanceof InstanceInvokeExpr) {
				InstanceInvokeExpr ie = (InstanceInvokeExpr) ((Stmt) unit).getInvokeExpr();
				if (isIntent(ie.getBase())) {
					return true;
				}
			}
		}
		return false;

	}

	public static int getIntentAttributeTypeInvoked(Unit unit) {
		if (((Stmt) unit).containsInvokeExpr()) {
			InvokeExpr invkExpr = ((Stmt) unit).getInvokeExpr();

			if (invkExpr instanceof InstanceInvokeExpr) {
				InstanceInvokeExpr ie = (InstanceInvokeExpr) ((Stmt) unit).getInvokeExpr();
				if (isIntent(ie.getBase())) {
					if (invkExpr.getMethod().getName().startsWith("set")
							|| invkExpr.getMethod().getName().startsWith("add")) {
						return 1;
					}
					if (invkExpr.getMethod().getName().startsWith("get")
							|| invkExpr.getMethod().getName().startsWith("resolve")) {
						return -1;
					}
					return -5;
				}

			}
		}
		return 0;
	}

	public static boolean isIntent(Value v) {
		if (v.getType().toString().equals("android.content.Intent"))
			return true;
		if (Hierarchy.getInstance(Constants.APK_PATH).isIntentClass(Scene.v().getSootClass(v.getType().toString()))) {
			return true;
		}
		return false;
	}

	public static Value getIntentBase(Unit unit) {
		if (((Stmt) unit).containsInvokeExpr()) {
			InvokeExpr invkExpr = ((Stmt) unit).getInvokeExpr();

			if (invkExpr instanceof InstanceInvokeExpr) {
				InstanceInvokeExpr ie = (InstanceInvokeExpr) ((Stmt) unit).getInvokeExpr();
				if (isIntent(ie.getBase())) {
					return ie.getBase();
				}
			}
		}
		return null;
	}

	public static Value getIntentDefinition(Unit unit) {
		if (unit instanceof DefinitionStmt) {
			if (isIntent(((DefinitionStmt) unit).getLeftOp())) {
				return ((DefinitionStmt) unit).getLeftOp();
			}

		}
		return null;
	}

	public static void updateAttributes(IType newIType, ATTRIBUTE attr, Set<CodePoint> set, int paramInd) {
		for (CodePoint cp : set) {
			if (IntentUtils.isParameterUnit(cp.getUnit())) {
				if (cp.getUnit().toString().contains("@parameter" + paramInd)) {
					continue;
				}

			}
			if (attr.equals(ATTRIBUTE.EXTRA) || attr.equals(ATTRIBUTE.CATEGOR)) {
				if (!newIType.iattrsMap.containsKey(attr))
					newIType.iattrsMap.put(attr, new HashSet<>());
			} else {
				newIType.iattrsMap.put(attr, new HashSet<>());
			}
			newIType.iattrsMap.get(attr).add(cp);

		}

	}

	public static void updateAttributes(IType newIType, ATTRIBUTE attr, Unit unit, SootMethod method, int i) {
		if (attr.equals(ATTRIBUTE.EXTRA) || attr.equals(ATTRIBUTE.CATEGOR)) {
			if (!newIType.iattrsMap.containsKey(attr))
				newIType.iattrsMap.put(attr, new HashSet<>());
		} else {
			newIType.iattrsMap.put(attr, new HashSet<>());
		}
		CodePoint point = new CodePoint(unit, method, Constants.APK_PATH);
		point.setArgIndex(i);
		newIType.iattrsMap.get(attr).add(point);
	}

	public static void setIntentRefInPoint(CodePoint point) {
		if (point.getUnit() instanceof AssignStmt) {
			for (ValueBox vb : point.getUnit().getDefBoxes()) {
				if (isIntent(vb.getValue())) {
					point.setIntentReference(vb.getValue());
				}
			}
		} else {
			for (ValueBox vb : point.getUnit().getUseBoxes()) {
				if (isIntent(vb.getValue())) {
					point.setIntentReference(vb.getValue());
				}
			}

		}

	}

	public static Value getSentIntentRef(Unit u) {
		Stmt stmt = (Stmt) u;
		for (Value arg : stmt.getInvokeExpr().getArgs()) {
			if (isIntent(arg))
				return arg;
		}
		return null;
	}

	public static boolean isITypeUsed(IType itype, Unit unit) {
		for (ValueBox vb : unit.getUseBoxes()) {
			if (isIntent(vb.getValue())) {
				if (vb.getValue().equals(itype.reference))
					return true;
			}
		}
		return false;
	}

	public static void updateWithReceivingInfo(IType i, CodePoint point) {
		if(point.getUnit().toString().contains("setResult(")) {
			i.iattrsMap.put(ATTRIBUTE.RES, getReceivingSymbol(point));
			return;
		}
		for (ATTRIBUTE attr : ATTRIBUTE.values()) {
			if (!attr.toString().startsWith("INIT")) {
				i.iattrsMap.put(attr, getReceivingSymbol(point));
			}
		}

	}

	public static void updateWithCallerInfo(IType i, CodePoint point, IType argType) {
		for (ATTRIBUTE attr : ATTRIBUTE.values()) {
			if (!attr.toString().startsWith("INIT")) {
				if (argType == null)
					i.iattrsMap.put(attr, getCallerSymbol(point));
				else {
					if (argType.iattrsMap.containsKey(attr))
						i.iattrsMap.put(attr, argType.iattrsMap.get(attr));
				}
			}
		}

	}

	public static void updateWithCallerInfo(IType i, CodePoint point) {
		for (ATTRIBUTE attr : ATTRIBUTE.values()) {
			if (!attr.toString().startsWith("INIT")) {

				i.iattrsMap.put(attr, getCallerSymbol(point));

			}
		}

	}

	private static Set<CodePoint> getCallerSymbol(CodePoint point) {
		Set<CodePoint> set = new HashSet<CodePoint>();
		set.add(point);
		return set;
	}

	private static Set<CodePoint> getReceivingSymbol(CodePoint point) {
		Set<CodePoint> set = new HashSet<CodePoint>();
		set.add(point);
		return set;
	}

	public static Unit getIntentParameterUnit(SootMethod method) {
		for (Unit u : method.retrieveActiveBody().getUnits()) {
			if (u instanceof IdentityStmt) {
				Value v = ((IdentityStmt) u).getLeftOp();
				if (isIntent(v))
					return u;
				if (method.getName().contains("onCreate")) {
					return u;
				}
			}
		}
		return null;
	}

	public static Value getIntentValue(Unit unit) {
		for (ValueBox vb : unit.getUseBoxes()) {
			if (isIntent(vb.getValue()))
				return vb.getValue();
		}
		for (ValueBox vb : unit.getDefBoxes()) {
			if (isIntent(vb.getValue()))
				return vb.getValue();
		}
		return null;
	}

	public static int getIntentArg(Stmt stmt) {
		for (int i = 0; i < stmt.getInvokeExpr().getArgCount(); i++) {
			Value arg = stmt.getInvokeExpr().getArg(i);
			if (isIntent(arg))
				return i;
		}
		return -1;
	}

	public static Unit getIntentParamUnit(SootMethod invokedMethod) {
		if (!invokedMethod.isConcrete())
			return null;
		for (Unit u : invokedMethod.retrieveActiveBody().getUnits()) {
			if (u instanceof IdentityStmt) {
				IdentityStmt istmt = (IdentityStmt) u;
				if (isIntent(istmt.getLeftOp()))
					return u;
			}
		}
		return null;

	}

	public static boolean isParameterUnit(Unit u) {
		if (u instanceof IdentityStmt) {
			IdentityStmt istmt = (IdentityStmt) u;
			if (isIntent(istmt.getLeftOp()))
				return true;
		}
		return false;
	}

	public static String getPutExtraKey(Unit u) {
		InvokeExpr expr = AnalysisUtils.getInvoke(u);
		if (expr.getArg(0) instanceof StringConstant) {
			return expr.getArg(0).toString();
		}
		return null;
	}

	public static boolean isParamAndArgMatch(IType it, int i) {
		if (isParameterUnit(it.getDefinitionPoint().getUnit())) {
			IdentityStmt istmt = (IdentityStmt) it.getDefinitionPoint().getUnit();
			return istmt.toString().contains("@parameter" + i);
		}
		return false;
	}

	private static boolean isIntentCast(Unit unit) {
		if (AnalysisUtils.isCast(unit)) {
			if (isIntent(((DefinitionStmt) unit).getLeftOp()))
				return isParcelable(AnalysisUtils.getCastValue(unit));
		}
		return false;
	}

	public static Value getParcelableDefinition(Unit unit) {
		if (unit instanceof DefinitionStmt) {
			if (isParcelable(((DefinitionStmt) unit).getLeftOp()) && unit.toString().contains("getParcelableExtra")) {
				return ((DefinitionStmt) unit).getLeftOp();
			}

		}
		return null;
	}

	public static boolean isParcelable(Value val) {
		if (val.getType().toString().contains("Parcelable"))
			return true;
		return false;
	}

	public static Set<String> isClassAppComponent(SootClass declaringClass) {
		if(Parser.getInstance(Constants.APK_PATH).components.containsKey(declaringClass.getName())) {
			return Parser.getInstance(Constants.APK_PATH).getPkg();
		}
		return new HashSet<>();
	}

}
