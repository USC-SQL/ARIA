package usc.edu.sql.fpa.analysis.intra;

import soot.Local;
import soot.PrimType;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.pdg.PDGNode.Attribute;
import soot.toolkits.scalar.Pair;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.UnitValueBoxPair;
import usc.edu.sql.fpa.analysis.IRAnalysis;
import usc.edu.sql.fpa.model.CodePoint;
import usc.edu.sql.fpa.model.Intent;
import usc.edu.sql.fpa.model.IntentIR;
import usc.edu.sql.fpa.utils.AnalysisUtils;
import usc.edu.sql.fpa.utils.Constants;
import usc.edu.sql.fpa.utils.Hierarchy;
import usc.edu.sql.fpa.utils.IntentUtils;
import usc.edu.sql.fpa.utils.Parser;
import usc.edu.sql.fpa.utils.URIUtils;
import usc.edu.sql.fpa.utils.Constants.ATTRIBUTE;
import usc.sql.ir.ConstantString;
import usc.sql.ir.Expression;
import usc.sql.ir.ExternalPara;
import usc.sql.ir.InternalVar;
import usc.sql.ir.Operation;
import usc.sql.ir.Variable;
import usc.sql.string.utils.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IntentGenerator {

	public static Map<CodePoint, Set<IntentIR>> generateIRIntent(Map<? extends CodePoint, Set<IType>> itypesAtSpoints,
			Set<CodePoint> receivers) {
		Map<CodePoint, Set<IntentIR>> result = new HashMap<>();
		for (CodePoint cp : itypesAtSpoints.keySet()) {
			Set<IntentIR> intents = new HashSet<>();
			for (IType it : itypesAtSpoints.get(cp)) {
				Set<IntentIR> set = generateIntentIRPerIType(it, cp, receivers);
				intents.addAll(set);
			}
			result.put(cp, intents);
		}
		return result;
	}


	private static Set<IntentIR> generateIntentIRPerIType(IType it, CodePoint ipoint, Set<CodePoint> receivers) {
		IRAnalysis irAnalysis = IRAnalysis.getInstance(Constants.APK_PATH);
		Set<IntentIR> workingIntents = new HashSet<>();
//		for (CodePoint cp : it.iattrsMap.get(ATTRIBUTE.DEF)) {
//			IntentIR in = new IntentIR(Util.getBytecodeOffset(cp.getUnit()), cp.getMethod().getSignature());
//			workingIntents.add(in);
//		}
		if (ipoint.getUnit().toString().contains("setResult")) {
			it.iattrsMap.put(ATTRIBUTE.RES, Set.of(ipoint));
		}
		IntentIR in = new IntentIR(Util.getBytecodeOffset(it.getDefinitionPoint().getUnit()),
				it.getDefinitionPoint().getMethod().getSignature());

		workingIntents.add(in);
		List<IntentIR> worklist = handleExtraAttr(workingIntents, it, irAnalysis, receivers);
		worklist = handleCategoryAttr(worklist, it, irAnalysis, receivers);

		for (ATTRIBUTE attr : it.iattrsMap.keySet()) {
			if (attr == ATTRIBUTE.DEF || attr == ATTRIBUTE.EXTRA || attr == ATTRIBUTE.CATEGOR || attr == ATTRIBUTE.FLAG)
				continue;

			Set<IntentIR> updatedIntents = new HashSet<IntentIR>();
			while (!worklist.isEmpty()) {
				IntentIR w = worklist.remove(0);
				for (CodePoint currPoint : it.iattrsMap.get(attr)) {

					Stmt stmt = (Stmt) (currPoint.getUnit());

					Set<Variable> ir = irAnalysis.getIRsForInvocation(currPoint.getMethod(), currPoint.getUnit(),
							currPoint.getArgIndex() + "", attr);
					if (isComingFromIntent(currPoint, it, receivers)) {
						ir = irAnalysis.generateIRforReceivedInformation(currPoint, attr);
					}
					if (ir.isEmpty() && attr.equals(ATTRIBUTE.DATA)) {
						ir = handleDataAttr(it, irAnalysis);
					}
					if (ir.isEmpty() && attr.equals(ATTRIBUTE.COMPONENT)) {

						if (stmt.containsInvokeExpr() && currPoint.getArgIndex() != -1) {

							if (isComponentNameSetting(stmt.getInvokeExpr(), currPoint.getArgIndex())) {
								ir = handleComponentName(it, irAnalysis, receivers, ATTRIBUTE.COMPONENT, "1");
							}
							if (isClassSetting(stmt.getInvokeExpr(), currPoint.getArgIndex()))
								ir = irAnalysis.getClassConstantVariable(stmt.getInvokeExpr(), currPoint.getArgIndex());
						}
					}
					if (ir.isEmpty() && attr.equals(ATTRIBUTE.PACKAGE)) {
						if (stmt.containsInvokeExpr() && currPoint.getArgIndex() != -1) {
							Type v = stmt.getInvokeExpr().getArg(currPoint.getArgIndex()).getType();
							if (Hierarchy.getInstance(Constants.APK_PATH)
									.isContextClass(AnalysisUtils.getClassFromString(v.toString()))) {
								Set<String> pkg = IntentUtils
										.isClassAppComponent(currPoint.getMethod().getDeclaringClass());

								if (pkg.isEmpty()) {
									String[] tt = v.toString().split("\\.");
									if (tt.length > 2) {
										pkg.add(tt[0] + "." + tt[1] + ".*");

									}
								}
								for (String s : pkg) {
									ir.add(new ConstantString(s));
								}

							}
						}

						if (stmt.containsInvokeExpr() && currPoint.getArgIndex() != -1) {

							if (isComponentNameSetting(stmt.getInvokeExpr(), currPoint.getArgIndex())) {
								ir = handleComponentName(it, irAnalysis, receivers, ATTRIBUTE.PACKAGE, "0");
							}
						}
						if (ir.isEmpty()) {
							ir.addAll(IntentUtils.getAppPackageIR());

						}
					}

					updatedIntents.addAll(updateIntentBasedOnAttr(w, attr, currPoint, ir));

				}

			}
			worklist.addAll(updatedIntents);
		}
		return new HashSet<IntentIR>(worklist);
	}

	private static boolean isClassSetting(InvokeExpr expr, int ind) {
		if (expr == null)
			return false;
		Value arg = expr.getArg(ind);
		if (arg.getType().toString().contains("java.lang.Class"))
			return true;
		return false;
	}

	private static boolean isComponentNameSetting(InvokeExpr expr, int ind) {
		if (expr == null)
			return false;
		Value arg = expr.getArg(ind);
		System.out.println(expr);
		if (arg.getType().toString().contains("ComponentName"))
			return true;
		return false;
	}

	private static List<IntentIR> handleCategoryAttr(List<IntentIR> workingIntents, IType it, IRAnalysis irAnalysis,
			Set<CodePoint> receivers) {
		List<IntentIR> worklist = new ArrayList<IntentIR>(workingIntents);
		if (!it.iattrsMap.containsKey(ATTRIBUTE.CATEGOR)) {
			return worklist;
		}
		for (CodePoint currPoint : it.iattrsMap.get(ATTRIBUTE.CATEGOR)) {

			Set<IntentIR> updatedIntents = new HashSet<IntentIR>();
			if (isComingFromIntent(currPoint, it, receivers)) {
				Set<Variable> ir = irAnalysis.generateIRforReceivedInformation(currPoint, ATTRIBUTE.CATEGOR);
				while (!worklist.isEmpty()) {
					IntentIR w = worklist.remove(0);
					updatedIntents.addAll(updateIntentBasedOnAttr(w, ATTRIBUTE.CATEGOR, currPoint, ir));
				}
			} else {
				while (!worklist.isEmpty()) {
					IntentIR w = worklist.remove(0);
					Set<Variable> keyIR = irAnalysis.getIRsForInvocation(currPoint.getMethod(), currPoint.getUnit(),
							currPoint.getArgIndex() + "", ATTRIBUTE.CATEGOR);
					updatedIntents.addAll(updateIntentBasedOnAttr(w, ATTRIBUTE.CATEGOR, currPoint, keyIR));
				}
			}
			worklist.addAll(updatedIntents);
		}

		return new ArrayList<IntentIR>(worklist);
	}

	private static List<IntentIR> handleExtraAttr(Set<IntentIR> workingIntents, IType it, IRAnalysis irAnalysis,
			Set<CodePoint> receivers) {
		List<IntentIR> worklist = new ArrayList<IntentIR>(workingIntents);
		if (!it.iattrsMap.containsKey(ATTRIBUTE.EXTRA)) {
			return worklist;
		}

		for (CodePoint currPoint : it.iattrsMap.get(ATTRIBUTE.EXTRA)) {

			Set<IntentIR> updatedIntents = new HashSet<IntentIR>();
			if (isComingFromIntent(currPoint, it, receivers)) {
				Set<Variable> ir = irAnalysis.generateIRforReceivedInformation(currPoint, ATTRIBUTE.EXTRA);
				while (!worklist.isEmpty()) {
					IntentIR w = worklist.remove(0);
					updatedIntents.addAll(updateIntentBasedOnAttr(w, ATTRIBUTE.EXTRA, currPoint, ir));
				}
			} else {
				while (!worklist.isEmpty()) {
					IntentIR w = worklist.remove(0);
					Set<Variable> keyIR = irAnalysis.getIRsForInvocation(currPoint.getMethod(), currPoint.getUnit(),
							currPoint.getArgIndex() + "", ATTRIBUTE.EXTRA);
					Set<Variable> valIR = irAnalysis.getIRsForInvocation(currPoint.getMethod(), currPoint.getUnit(),
							"1", ATTRIBUTE.EXTRA);
					System.out.println(currPoint);
					if (currPoint.getUnit() instanceof IdentityStmt) {

					} else if (AnalysisUtils.getInvoke(currPoint.getUnit()).getArgCount() == 1) { // putExtras(Bundle)
						keyIR = Set.of(new ConstantString(Constants.EXTRA_BUNDLE_KEY));
						valIR = Set.of(new ConstantString(Constants.EXTRA_BUNDLE_KEY));

					} else {
						System.out.println(currPoint);
						if (AnalysisUtils.getExtraValue(currPoint) == null)
							System.out.println("hi");
						if (valIR.isEmpty() && AnalysisUtils.getExtraValue(currPoint) != null
								&& AnalysisUtils.getExtraValue(currPoint).getType() instanceof PrimType) {
							if (AnalysisUtils.getExtraValue(currPoint) instanceof Constant) {
								valIR = Set.of(new ConstantString(AnalysisUtils.getExtraValue(currPoint).toString()));
							}
						}
//						if (valIR.isEmpty()) {
//							valIR = Set.of(
//									new ConstantString(AnalysisUtils.getExtraValue(currPoint).getType().toString()));
//						}
					}

					updatedIntents.addAll(updateIntentBasedOnAttr(w, ATTRIBUTE.EXTRA, currPoint, keyIR, valIR));
				}
			}
			worklist.addAll(updatedIntents);
		}

		return new ArrayList<IntentIR>(worklist);

	}

	private static Set<Variable> handleDataAttr(IType it, IRAnalysis irAnalysis) {
		if (!it.iattrsMap.containsKey(ATTRIBUTE.DATA)) {
			return null;
		}
		Set<Variable> result = new HashSet<>();
		for (CodePoint currPoint : it.iattrsMap.get(ATTRIBUTE.DATA)) {
			Set<CodePoint> reached = AppInferrer.getInstance(Constants.APK_PATH).getURIReachability(
					new BriefUnitGraph(currPoint.getMethod().retrieveActiveBody()), currPoint.getUnit());
			for (CodePoint cp : reached) {
				Set<Variable> ir = URIUtils.getIRForURIDef(cp.getUnit(), cp.getMethod());
				if (ir != null)
					result.addAll(ir);
			}
		}
		return result;
	}

	private static Set<Variable> handleComponentName(IType it, IRAnalysis irAnalysis, Set<CodePoint> receivers,
			ATTRIBUTE attr, String i) {
		if (!it.iattrsMap.containsKey(attr)) {
			return null;
		}
		Set<Variable> result = new HashSet<>();
		for (CodePoint currPoint : it.iattrsMap.get(attr)) {
			Local l = (Local) (AnalysisUtils.getInvoke(currPoint.getUnit()).getArg(currPoint.getArgIndex()));
			List<Unit> reached = AppInferrer.getInstance(Constants.APK_PATH).getDefs(currPoint.getMethod())
					.getDefsOfAt(l, currPoint.getUnit());
			for (Unit unit : reached) {
				if (AnalysisUtils.getInvoke(unit) != null && AnalysisUtils.getInvoke(unit).getMethod().getSignature()
						.contains("<android.content.Intent: android.content.ComponentName getComponentName()>")) {
					return generateGetComponentExpression(unit, currPoint.getMethod());
				}
				for (UnitValueBoxPair use : AppInferrer.getInstance(Constants.APK_PATH).getUses(currPoint.getMethod())
						.getUsesOf(unit)) {
					if (AnalysisUtils.getInvokedMethod(use.getUnit()) != null) {
						if (AnalysisUtils.getInvokedMethod(use.getUnit()).getName().contains("<init>")) {
							Set<Variable> ir = irAnalysis.getIRsForInvocation(currPoint.getMethod(), use.unit, i, attr);
							result.addAll(ir);
							Type v = ((Stmt) use.getUnit()).getInvokeExpr().getArg(Integer.parseInt(i)).getType();
							if (result.isEmpty() && Hierarchy.getInstance(Constants.APK_PATH)
									.isContextClass(AnalysisUtils.getClassFromString(v.toString()))) {

								String[] tt = v.toString().split("\\.");
								if (tt.length > 2) {
									String pkg = tt[0] + "." + tt[1] + ".*";
									ir.add(new ConstantString(pkg));
									result.addAll(ir);
								}

							}

						}
					}

				}
			}
		}
		return result;
	}

	private static Set<Variable> generateGetComponentExpression(Unit u, SootMethod sm) {
		List<Variable> operands = new ArrayList<>();
		Operation op = null;
		InstanceInvokeExpr expr = (InstanceInvokeExpr) AnalysisUtils.getInvoke(u);
		InternalVar intentVar = new InternalVar(expr.getBase().toString());
		operands.add(intentVar);

		if (expr.getMethod().getName().contains("getComponent")) {
			op = new Operation("getCompoent");
		}
		List<List<Variable>> operandList = new ArrayList<>();
		for (Variable opd : operands) {
			List<Variable> temp = new ArrayList<>();
			temp.add(opd);
			operandList.add(temp);
		}
		Set<Variable> set = new HashSet<Variable>();
		Expression exp = new Expression(operandList, op, Util.getBytecodeOffset(u), sm.getSignature());
		set.add(exp);
		return set;
	}

	private static boolean isComingFromIntent(CodePoint cp, IType it, Set<CodePoint> receivers) {

		for (CodePoint rp : receivers) {
			if (rp.getUnit().equals(cp.getUnit()))
				if (rp.getMethod().equals(cp.getMethod()))
					return true;
		}
		return false;
	}

	private static Set<IntentIR> updateIntentBasedOnAttr(IntentIR w, ATTRIBUTE attr, CodePoint currPoint,
			Set<Variable> keyIR, Set<Variable> valIR) {
		Set<IntentIR> res = new HashSet<>();
		if (keyIR == null)
			return res;
		if (keyIR.isEmpty()) {
			res.add(w);
			return res;
		}
		for (Variable v : keyIR) {
			if (IRAnalysis.isAllExtraVariable(v)) {
				IntentIR cw = new IntentIR(w);
				Set<Variable> set = new HashSet<Variable>();
				set.add(v);
				cw.extraMap.put(v, set);
				res.add(cw);
			} else {
				IntentIR cw = new IntentIR(w);
				cw.extraMap.put(v, valIR);
				res.add(cw);

			}
		}
		return res;
	}

	private static Set<IntentIR> updateIntentBasedOnAttr(IntentIR w, ATTRIBUTE attr, CodePoint currPoint,
			Set<Variable> ir) {
		Set<IntentIR> res = new HashSet<>();
		if (ir == null) {
			return res;
		}
		if (ir.isEmpty()) {
			res.add(w);
			return res;
		}
		for (Variable v : ir) {
			IntentIR cw = new IntentIR(w);
			if (attr.equals(ATTRIBUTE.RES)) { // setRes
				cw.addAttr(ATTRIBUTE.RES, v);
			}
			if (attr.equals(ATTRIBUTE.ACTION) || attr.equals(ATTRIBUTE.INIT1_ACTION)) { // setAction
				cw.addAttr(ATTRIBUTE.ACTION, v);
			}
			if (attr.equals(ATTRIBUTE.CATEGOR)) { // addCategory
				cw.addAttr(ATTRIBUTE.CATEGOR, v);
			}
			if (attr.equals(ATTRIBUTE.DATA)) { // setData
				cw.addAttr(ATTRIBUTE.DATA, v);
			}
			if (attr.equals(ATTRIBUTE.COMPONENT) || attr.equals(ATTRIBUTE.INIT2_EXPLICIT)) { // setComponent
				cw.addAttr(ATTRIBUTE.COMPONENT, v);
			}
			if (attr.equals(ATTRIBUTE.TYPE)) { // setType
				cw.addAttr(ATTRIBUTE.TYPE, v);
			}
			if (attr.equals(ATTRIBUTE.PACKAGE)) { // setPackage

				cw.addAttr(ATTRIBUTE.PACKAGE, v);

			}
			if (attr.equals(ATTRIBUTE.EXTRA)) {
				cw.addExtra(new ConstantString(Constants.ALL_EXTRA_KEY), v);
			}
			res.add(cw);
		}
		return res;
	}

//	public Set<Intent> generateNewIntent(Map<ATTRIBUTE, Set<CodePoint>> iattrmp, IType itype, SootMethod method) {
//
//		Set<Intent> intents = new HashSet<Intent>();
//		Set<Intent> workingIntents = new HashSet<Intent>();
//		for(CodePoint cp: iattrmp.get(ATTRIBUTE.DEF)) {
//			Intent in = new Intent();
//			in.definitionPoint = cp;
//			workingIntents.add(in);
//		}
//
//		for (Intent workingIntent : workingIntents) {
//			Intent in = new Intent(workingIntent);
//			for (ATTRIBUTE attr : iattrmp.keySet()) {
//				for (CodePoint currPoint : iattrmp.get(attr)) {
//					Stmt stmt = (Stmt) (currPoint.getUnit());
//					if (!stmt.containsInvokeExpr())
//						continue;
//					InvokeExpr ie = stmt.getInvokeExpr();
//					if (attr.equals(ATTRIBUTE.ACTION) || attr.equals(ATTRIBUTE.INIT1_ACTION)) { // setAction
//						intent.action = ie.getArg(0);
//					}
//					if (attr.equals(ATTRIBUTE.CATEGOR)) { // addCategory
//						intent.categories.add(p.getO2());
//					}
//					if (attr.equals(ATTRIBUTE.DATA)) { // setData
//						intent.uri = p.getO2();
//					}
//					if (attr.equals(ATTRIBUTE.COMPONENT) || attr.equals(ATTRIBUTE.CLASS_CONSTANT)
//							|| attr.equals(ATTRIBUTE.CLASSNAME) || attr.equals(ATTRIBUTE.INIT2_EXPLICIT)) { // setComponent
//						intent.targetComponent = p.getO2();
//					}
//					if (attr.equals(ATTRIBUTE.TYPE)) { // setType
//						intent.dataType = p.getO2();
//					}
//
//				}
//
//			}
//			intents.add(intent);
//		}
//		return intents;
//	}

//	private Pair<Stmt, String> findValue(IType iTypeAttrValue, ATTRIBUTE attr, CodePoint currPoint) {
//		Stmt stmt = (Stmt) (currPoint.getUnit());
//		InvokeExpr iexpr = stmt.getInvokeExpr();
//		updatePossibleValsOfType(iTypeAttrValue, attr, currPoint, stmt, iexpr.getArg(currPoint.getArgIndex()),
//				currPoint.getMethod());
//		return findValue(iTypeAttrValue, attr, stmt, iexpr.getArg(currPoint.getArgIndex()), currPoint.getMethod());
//	}

//	private void updatePossibleValsOfType(IType iTypeAttrValue, ATTRIBUTE attr, CodePoint cp, Stmt stmt, Value arg,
//			SootMethod method) {
//		if (attr.equals(ATTRIBUTE.DEF))
//			return;
//		DataFlowAnalysis reachAnalysis = DataFlowAnalysis.getInstance();
//		Set<CodePoint> possibleVals = findPossibleNumberOfValsForArg(reachAnalysis, cp, stmt, arg, method);
//		iTypeAttrValue.possibleVal.put(attr, possibleVals);
//
//	}

//	private Set<CodePoint> findPossibleNumberOfValsForArg(DataFlowAnalysis reachAnalysis, CodePoint currPoint,
//			Stmt stmt, Value arg, SootMethod method) {
//		Set<CodePoint> res = new HashSet<CodePoint>();
//		if (arg instanceof Local) {
//
//			Set<CodePoint> reacheds = reachAnalysis.computeReverseReachability(method, stmt);
//			for (CodePoint cp : reacheds) {
//				if (AnalysisUtils.isdefinition(cp.getUnit(), arg)) {
//					res.addAll(findPossibleNumberOfValsForArg(reachAnalysis, cp, (Stmt) cp.getUnit(),
//							((DefinitionStmt) cp.getUnit()).getRightOp(), method));
//				}
//			}
//
//		} else {
//			res.add(currPoint);
//		}
//
//		return res;
//	}

//	public Pair<Stmt, String> findValue(IType iTypeAttrValue, ATTRIBUTE attr, Stmt stmt, Value arg, SootMethod method) {
//		if (arg.getType().toString().equals("android.net.Uri")) {
//			return IntentUtils.findValueByData(stmt, arg, method);
//		}
//		if (arg.getType().toString().equals("android.content.ComponentName")) {
//			return IntentUtils.findValueByComponent(stmt, arg, method);
//		} else {
//			return IntentUtils.findValueByString(stmt, arg, method);
//		}
//
//	}

}
