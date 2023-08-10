package usc.edu.sql.fpa.utils;

import java.io.File;
import java.util.*;

import com.google.common.collect.Lists;

import CallGraph.StringCallGraph;
import edu.usc.sql.graphs.cfg.CFGInterface;
import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Timers;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.CastExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;
import soot.tagkit.BytecodeOffsetTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.Pair;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.UnitValueBoxPair;
import soot.util.Chain;
import usc.edu.sql.fpa.analysis.IRAnalysis;
import usc.edu.sql.fpa.analysis.intra.AppInferrer;
import usc.edu.sql.fpa.analysis.intra.CallGraphAnalysis;
import usc.edu.sql.fpa.analysis.intra.IntentReachability;
import usc.edu.sql.fpa.model.*;
import usc.edu.sql.fpa.utils.Constants.COMPONENT;
import usc.edu.sql.fpa.utils.Constants.INVOCATION_TYPE;
import usc.sql.string.IntentAnalysis;

public class AnalysisUtils {

//	public static List<SootMethod> computeEntryPoints() {
//		List<SootMethod> entryPoints = new ArrayList<SootMethod>();
//		InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
//		config.getAnalysisFileConfig().setTargetAPKFile(Constants.APK_PATH);
//		config.getAnalysisFileConfig().setAndroidPlatformDir(Constants.ANDROID_JAR);
//		config.getIccConfig().setIccModel(null);
//		SetupApplication sa = new SetupApplication(config);
//		sa.constructCallgraph();
//		SootMethod dummyMain = sa.getDummyMainMethod();
//		entryPoints.add(dummyMain);
////		Hierarchy h = Scene.v().getActiveHierarchy();
////
////		List<SootMethod> nonLifeCycleEntryPoints = getExtraEntryPoints(Scene.v().getApplicationClasses(), h);
////		List<SootMethod> dynamicEntryPoints = getDynamicRegRecieversEntryPoints(Scene.v().getApplicationClasses(),
////				dynRegReceivers, h);
//		return entryPoints;
//
//	}

	public static List<SootMethod> getMethodsInReverseTopologicalOrder(Map<String, SootMethod> map) {

		List<SootMethod> reverseTopologicalOrderMethods = new LinkedList<SootMethod>();

		MyStringCallGraph scg = new MyStringCallGraph(Scene.v().getCallGraph(), new HashSet<>(map.values()));
		for (CFGInterface n : scg.getRTOInterface()) {
			reverseTopologicalOrderMethods.add(map.get(n.getSignature()));

		}

//		Stack<SootMethod> methodsToAnalyze = new Stack<SootMethod>();
//		for (SootMethod entryPoint : entryPoints) {
//			methodsToAnalyze.push(entryPoint);
//			while (!methodsToAnalyze.isEmpty()) {
//				SootMethod method = methodsToAnalyze.pop();
//				if (!topologicalOrderMethods.contains(method)) {
//					if (method.hasActiveBody() || method.isConcrete()) {
//						topologicalOrderMethods.add(method);
//						for (Edge edge : getOutgoingEdges(method, cg)) {
//							methodsToAnalyze.push(edge.tgt());
//						}
//					}
//				} else {
//					topologicalOrderMethods.remove(method);
//					topologicalOrderMethods.add(method);
//				}
//			}
//
//		}
////		List<SootMethod> topologicalOrderMethodsOriginal = new LinkedList<SootMethod>();
////		for (SootMethod sm : topologicalOrderMethods) {
////
////			topologicalOrderMethodsOriginal.add(map.get(sm.getSignature()));
////		}
//		List<SootMethod> rtoMethods = Lists.reverse(topologicalOrderMethods);
		return reverseTopologicalOrderMethods;

	}

	public static boolean isApplicationMethod(SootMethod method) {
		Chain<SootClass> applicationClasses = Scene.v().getApplicationClasses();
		for (SootClass appClass : applicationClasses) {
			if (appClass.getMethods().contains(method)) {
				return true;
			}
		}
		return false;
	}

	public static SootClass getLibraryClass(String className) {
		Chain<SootClass> libraryClasses = Scene.v().getLibraryClasses();
		for (SootClass libClass : libraryClasses) {
			if (libClass.getName().equals(className)) {
				return libClass;
			}
		}
		return null;
	}

	public static List<Edge> getOutgoingEdges(SootMethod method, CallGraph cg) {
		Iterator<Edge> edgeIterator = cg.edgesOutOf(method);
		List<Edge> outgoingEdges = Lists.newArrayList(edgeIterator);
		return outgoingEdges;
	}

	public static boolean shouldAnalyzeMethod(SootMethod method) {
		if (method == null)
			return false;
		if (!method.isConcrete())
			return false;
		return shouldAnalyzeMethod(method.getDeclaringClass());
	}

	public static boolean shouldAnalyzeMethod(SootClass sc) {
		if (sc == null)
			return false;
		String className = sc.getName();
		if (className.startsWith("androidx.") || className.startsWith("android.") || className.startsWith("java.")
				|| className.startsWith("javax.") || className.startsWith("sun.") || className.startsWith("org.omg.")
				|| className.startsWith("org.w3c.dom.") || className.startsWith("com.android."))
			return false;
		return true;
	}

	public static INVOCATION_TYPE unitNeedsAnalysis(Unit unit) {

		if (unit instanceof Stmt) {
			Stmt stmt = (Stmt) unit;
			if (stmt.containsInvokeExpr()) {
			
				COMPONENT type = APIUtils.checkInvoke(stmt);
				if (IntentUtils.getIntentBase(unit) != null && COMPONENT.PARCELABLE.equals(type) && unit.toString().contains("putExtra")) {
					return INVOCATION_TYPE.PUT_PARCLEABLE;
					
				}
				if (IntentUtils.getIntentBase(unit) != null && COMPONENT.PARCELABLE.equals(type) && unit.toString().contains("getParcelableExtra")) {
					return INVOCATION_TYPE.GET_PARCLEABLE;
					
				}
				if (COMPONENT.INTENT.equals(type)) {
					return INVOCATION_TYPE.RETREVING;
				}

				if (COMPONENT.ACTIVITY.equals(type) || COMPONENT.BROADCAST_RECEIVER.equals(type)
						|| COMPONENT.SERVICE.equals(type)) {

					return INVOCATION_TYPE.INITIATING_INTENT;
				}
				if (COMPONENT.ACTIVITY_RECEIVE_API.equals(type)) {

					return INVOCATION_TYPE.RECEIVING;
				}
				if (COMPONENT.CONTENT_PROVIDER.equals(type)) {
					return INVOCATION_TYPE.INITIATION_URI;
				}
				if (IntentUtils.getIntentArg(stmt) != -1) {
					return INVOCATION_TYPE.INTENT_ARGUMENT;
				}
				if (stmt instanceof DefinitionStmt) {
					return INVOCATION_TYPE.INTENT_INVOKE_DEFINE;
				}

			} else if (stmt instanceof ReturnStmt) {
				Value returnType = ((ReturnStmt) stmt).getOp();
				if (IntentUtils.isIntent(returnType)) {
					return INVOCATION_TYPE.RETURNING;
				}
			} else if (stmt instanceof DefinitionStmt) {
				Value defType = ((DefinitionStmt) stmt).getLeftOp();
				if (IntentUtils.isIntent(defType)) {
					return INVOCATION_TYPE.DEFINING;
				}
			}

		}
		return null;
	}

	public static boolean isSameReference(CodePoint codePoint, Value useVal, Unit use, SootMethod sm) {
		if (useVal instanceof Local) {
			SimpleLocalDefs defs = AppInferrer.getInstance(Constants.APK_PATH).getDefs(sm);
			List<Unit> valDefs = defs.getDefsOfAt((Local) useVal, use);
			if (valDefs.contains(codePoint.getUnit()))
				return true;
			if (codePoint.getUnit() instanceof DefinitionStmt) {
				DefinitionStmt defstmt = (DefinitionStmt) codePoint.getUnit();
				if (defstmt.getRightOp() instanceof Local) {

					List<Unit> codePointDefs = defs.getDefsOfAt((Local) defstmt.getRightOp(), defstmt);
					for (Unit u : codePointDefs)
						if (isSameReference(new CodePoint(u, sm, codePoint.getApp()), useVal, use, sm))
							return true;
				}
			}

		}

		return false;
	}

	public static boolean isApplicationMethodInvoked(Unit unit) {
		if (unit instanceof Stmt) {
			if (((Stmt) unit).containsInvokeExpr()) {
				return isApplicationMethod(((Stmt) unit).getInvokeExpr().getMethod());
			}
		}
		return false;
	}
	public static boolean isCast(Unit unit) {
		if (unit instanceof DefinitionStmt) {
			DefinitionStmt def = (DefinitionStmt) unit;
			if (def.getRightOp() instanceof CastExpr) {
				return true;
			}
		}
		return false;
	}
	
	public static Value getCastValue(Unit unit) {
		if (unit instanceof DefinitionStmt) {
			DefinitionStmt def = (DefinitionStmt) unit;
			if (def.getRightOp() instanceof CastExpr) {
				return ((CastExpr) def.getRightOp()).getOp();
			}
		}
		return null;
	}
	
	public static SootMethod getInvokedMethod(Unit u) {
		if (u == null)
			return null;
		Stmt stmt = (Stmt) u;
		if (stmt.containsInvokeExpr()) {
			return stmt.getInvokeExpr().getMethod();
		}
		return null;
	}

	public static InvokeExpr getInvoke(Unit u) {
		if (u == null)
			return null;
		Stmt stmt = (Stmt) u;
		if (stmt.containsInvokeExpr()) {
			return stmt.getInvokeExpr();
		}
		return null;
	}

	public static List<Pair<SootClass, SootMethod>> getDynamicRegRecieversEntryPoints(Chain<SootClass> classes,
			Set<Pair<SootClass, List<IntentFilter>>> dynRegReceivers, Hierarchy h) {

		List<Pair<SootClass, SootMethod>> entrypoints = new ArrayList<>();
		for (SootClass c : classes) {
			entrypoints.addAll(getDynamicRegRecieversEntryPointsByMethods(dynRegReceivers, c.getMethods(), h));
		}
		return entrypoints;
	}

	public static List<Pair<SootClass, SootMethod>> getDynamicRegRecieversEntryPointsByMethods(
			Set<Pair<SootClass, List<IntentFilter>>> dynRegReceivers, List<SootMethod> methods, Hierarchy h) {
		List<Pair<SootClass, SootMethod>> dynRegReceiverRegPoints = new ArrayList<>();
		for (SootMethod m : new ArrayList<SootMethod>(methods)) {
			if (m.isConcrete()) {
				Body b = m.retrieveActiveBody();
				for (Unit u : b.getUnits()) {
					Stmt s = (Stmt) u;
					if (s.containsInvokeExpr()) {
						InvokeExpr ie = s.getInvokeExpr();
						if (ie.getMethod().getDeclaringClass().isInterface()) {
							continue;
						}

						if (h.isContextClass(ie.getMethod().getDeclaringClass())) {
							if (ie.getMethod().getName().equals("registerReceiver")) {
								String registeredType = ie.getArg(0).getType().toString();
								SootClass receiverClass = Scene.v().getSootClass(registeredType);
								List<IntentFilter> registerFilter = computeIntentFilter(m, s, ie.getArg(1));
								Pair<SootClass, List<IntentFilter>> componentInfo = new Pair<>(receiverClass,
										registerFilter);
								dynRegReceivers.add(componentInfo);
								dynRegReceiverRegPoints.add(new Pair<SootClass, SootMethod>(receiverClass, m));
//								for (SootMethod regTypeMethod : receiverClass.getMethods()) {
//									if (regTypeMethod.getName().startsWith("on") && regTypeMethod.isConcrete()) {
//										
//									}
//								}
							}
						}
					}

				}
			}
		}
		return dynRegReceiverRegPoints;
	}

	private static List<IntentFilter> computeIntentFilter(SootMethod m, Stmt stmt, Value v) {
		if (!(v instanceof Local))
			return new ArrayList<>();
		List<IntentFilter> filters = new ArrayList<IntentFilter>();
		List<Unit> defs = AppInferrer.getInstance(Constants.APK_PATH).getDefs(m).getDefsOf((Local) v);
		for (Unit def : defs) {
			List<UnitValueBoxPair> uvbps = AppInferrer.getInstance(Constants.APK_PATH).getUses(m).getUsesOf(def);
			for (UnitValueBoxPair uvbp : uvbps) {
				Stmt s = (Stmt) uvbp.getUnit();
				if (s.containsInvokeExpr()) {
					InvokeExpr ie = s.getInvokeExpr();
					if (ie.getMethod().getName().equals("<init>")) {
						if (ie.getArgCount() == 2) {
							IntentFilter filter = new IntentFilter();
							filter.addAction(getConstantStringValue(ie.getArg(0)));
							filter.addDatum("mimType", getConstantStringValue(ie.getArg(1)));
							filters.add(filter);
						}
						if (ie.getArgCount() == 1) {
							IntentFilter filter = new IntentFilter();
							filter.addAction(getConstantStringValue(ie.getArg(0)));
							filters.add(filter);
						}

						if (ie.getArgCount() == 0) {
							IntentFilter filter = new IntentFilter();
							filters.add(filter);
						}
					}
					if (ie.getMethod().getName().contains("addAction")) {
						for (IntentFilter filter : filters)
							filter.addAction(getConstantStringValue(ie.getArg(0)));
					}
					if (ie.getMethod().getName().contains("addCategory")) {
						for (IntentFilter filter : filters)
							filter.addCategory(getConstantStringValue(ie.getArg(0)));
					}
					if (ie.getMethod().getName().contains("addDataScheme")) {
						for (IntentFilter filter : filters)
							filter.addDatum("scheme", getConstantStringValue(ie.getArg(0)));
					}
					if (ie.getMethod().getName().contains("addDataPath")) {
						for (IntentFilter filter : filters)
							filter.addDatum("path", getConstantStringValue(ie.getArg(0)));
					}

				}

			}
		}
		return filters;
	}

	private static String getConstantStringValue(Value arg) {
		if (arg instanceof StringConstant) {
			return ((StringConstant) arg).value;
		}
		return null;
	}

	public static Set<SootMethod> findEntryPoints(Set<String> components, Set<String> layoutDefniedOnClicks,
			Set<SootMethod> appMethods, Set<SootMethod> initialEntryPoints) {
		Set<SootMethod> entryPoints = new HashSet<>();
		for (String component : components) {
			SootClass sc = Scene.v().getSootClassUnsafe(component);
			if (sc == null)
				continue;
			if (!shouldAnalyzeMethod(sc)) {
				continue;
			}
			for (SootMethod sm : sc.getMethods()) {
				if (isEntryPoint(sm)) {
					entryPoints.add(sm);
				}
			}
		}
		if (initialEntryPoints.isEmpty()) {
			for (SootMethod sm : appMethods) {
				if (entryPoints.contains(sm))
					continue;
				if (!shouldAnalyzeMethod(sm)) {
					continue;
				}
				if (sm.getName().equals("onClick"))
					entryPoints.add(sm);
				if (Hierarchy.getInstance(Constants.APK_PATH).isFragmentClass(sm.getDeclaringClass())) {
					if (isEntryPoint(sm))
						entryPoints.add(sm);
				}
				if (Hierarchy.getInstance(Constants.APK_PATH).isActivityClass(sm.getDeclaringClass())) {
					if (layoutDefniedOnClicks.contains(sm.getName())) {
						entryPoints.add(sm);
					}
				}
			}
		} else {
			entryPoints.addAll(initialEntryPoints);
		}
		return entryPoints;
	}

	private static boolean isEntryPoint(SootMethod sm) {
		if (sm.getName().length() > 2 && sm.getName().startsWith("on")
				&& Character.isUpperCase(sm.getName().charAt(2))) {
			return true;
		}
		return false;
	}

	public static Unit findUnitByOffSet(SootMethod sm, int bytecodeOffSet) {
		for (Unit u : sm.retrieveActiveBody().getUnits()) {
			if (getByteCodeOffsetTag(u) == bytecodeOffSet && bytecodeOffSet != -1) {
				return u;
			}
		}
		return null;
	}

	public static int getByteCodeOffsetTag(Unit u) {
		int bytecodeOffset = -1;
		for (Tag t : u.getTags()) {
			if (t instanceof BytecodeOffsetTag)
				bytecodeOffset = ((BytecodeOffsetTag) t).getBytecodeOffset();
		}
		return bytecodeOffset;
	}

	public static String getAppName() {
		String[] t = Constants.APK_PATH.split(File.separator);
		return t[t.length - 1];
	}

	public static Value getExtraValue(CodePoint point) {
		if (getInvoke(point.getUnit()).getArgCount() == 2) {
			return getInvoke(point.getUnit()).getArg(1);
		}
		return null;
	}

	public static SootClass getClassFromString(String string) {
		
		return Scene.v().getSootClassUnsafe(string);
	}

}
