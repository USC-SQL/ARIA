package usc.edu.sql.fpa.analysis.intra;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import CallGraph.StringCallGraph;
import soot.Body;
import soot.CompilationDeathException;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Timers;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.Pair;
import soot.util.Chain;
import usc.edu.sql.fpa.analysis.IRAnalysis;
import usc.edu.sql.fpa.model.CodePoint;
import usc.edu.sql.fpa.model.Component;
import usc.edu.sql.fpa.model.ICCResponsibleComponent;
import usc.edu.sql.fpa.model.Intent;
import usc.edu.sql.fpa.model.IntentFilter;
import usc.edu.sql.fpa.model.IntentIR;
import usc.edu.sql.fpa.output.GraphReporter;
import usc.edu.sql.fpa.output.Reporter;
import usc.edu.sql.fpa.utils.APIUtils;
import usc.edu.sql.fpa.utils.AnalysisUtils;
import usc.edu.sql.fpa.utils.Constants;
import usc.edu.sql.fpa.utils.Hierarchy;
import usc.edu.sql.fpa.utils.IntentUtils;
import usc.edu.sql.fpa.utils.Parser;
import usc.edu.sql.fpa.utils.Constants.API_TYPE;
import usc.edu.sql.fpa.utils.Constants.ATTRIBUTE;
import usc.edu.sql.fpa.utils.Constants.COMPONENT;
import usc.edu.sql.fpa.utils.Constants.INVOCATION_TYPE;

public class IntraIntentAnalysis {
	protected Logger logger = LoggerFactory.getLogger(IntraIntentAnalysis.class);
	protected Parser parser = Parser.getInstance(Constants.APK_PATH);
	Set<CodePoint> senders = new HashSet<>();
	Set<CodePoint> receivers = new HashSet<>();
	Map<CodePoint, Set<IntentIR>> intentsAtSpoints = new HashMap<>();
	Map<CodePoint, Set<IntentIR>> intentsAtOtherPoints = new HashMap<>();
	Map<CodePoint, Set<IntentIR>> intentsAtRpoints = new HashMap<>();

	protected Map<CodePoint, Set<IType>> itypesAtOtherPoints = new HashMap<>();
	protected Map<CodePoint, Set<IType>> itypesAtSpoints = new HashMap<>();
	protected Map<CodePoint, Set<IType>> itypesAtRPoints = new HashMap<>();
	protected Map<SootMethod, Set<CodePoint>> itypePoints = new HashMap<>();

	Map<IType, Set<IntentIR>> itypeToIntentIRs = new HashMap<>();
	Map<SootMethod, Map<Unit, Set<IType>>> summary = new HashMap<>();
	Map<SootMethod, Set<CodePoint>> tracker = new HashMap<>();

	public void run() {
		long start = System.currentTimeMillis();
		preLoadARSC();
		long xml = System.currentTimeMillis();
		findITypes();
		long its = System.currentTimeMillis();
		generateIntents();
		long irintent = System.currentTimeMillis();
		Constants.APP_EXTRA_INFO.put("Parse_TIME", (xml - start) + "");
		Constants.APP_EXTRA_INFO.put("ITYPE_TIME", (its - xml) + "");
		Constants.APP_EXTRA_INFO.put("INTENTIR_TIME", (irintent - its) + "");

	}

	protected void preLoadARSC() {
		ZipFile archive = null;
		try {
			archive = new ZipFile(Constants.APK_PATH);
			for (Enumeration<? extends ZipEntry> entries = archive.entries(); entries.hasMoreElements();) {
				ZipEntry entry = entries.nextElement();

				String entryName = entry.getName();
				if (entryName.equals("resources.arsc")) {
					parser.parseARSC(archive.getInputStream(entry));
				} else if (entryName.equals("AndroidManifest.xml")) {
					parser.parseManifestNoads(archive.getInputStream(entry));
				} else if (entryName.startsWith("res/layout/") && entryName.endsWith(".xml")) {
					parser.parseLayout(entryName, archive.getInputStream(entry));
				}
			}

		} catch (IOException e) {
			System.out.println(e.getMessage());
			throw new CompilationDeathException("Error reasing archive '" + Constants.APK_PATH + "'", e);
		} finally {
			try {
				if (archive != null)
					archive.close();
			} catch (Throwable t) {
			}
		}

	}

	private void generateIntents() {
		intentsAtSpoints = IntentGenerator.generateIRIntent(itypesAtSpoints, receivers);
		intentsAtOtherPoints = IntentGenerator.generateIRIntent(itypesAtOtherPoints, receivers);
		intentsAtRpoints = IntentGenerator.generateIRIntent(itypesAtRPoints, receivers);
	}

	protected void findITypes() {
		Hierarchy h = Hierarchy.getInstance(Constants.APK_PATH);

//		Timers.v().totalTimer.start();
		setupComponents();
		List<SootMethod> rtoMethods = AnalysisUtils.getMethodsInReverseTopologicalOrder(h.getAppMethodMap());

		CallGraphAnalysis cga = new CallGraphAnalysis();

		for (SootMethod method : rtoMethods) {
			if (method == null)
				continue;
			if (AnalysisUtils.shouldAnalyzeMethod(method)) {
				Set<ICCResponsibleComponent> comps = cga.backTrackCallerSequence(method, parser.components);
				if (method.retrieveActiveBody() != null) {
					Body b = method.getActiveBody();
					final BriefUnitGraph ug = new BriefUnitGraph(b);
					doReachabilityAnalysisOnMethod(method, comps, ug);
					if (COMPONENT.ACTIVITY_RECEIVE_CALLBACK.equals(APIUtils.getAPIType(method))
							|| COMPONENT.SERVICE_RECEIVE_CALLBACK.equals(APIUtils.getAPIType(method))
							|| COMPONENT.BROADCAST_RECEIVER_CALLBACK.equals(APIUtils.getAPIType(method))) {
						logger.info("Found receinving method " + " in " + method);
						for (ICCResponsibleComponent icccomp : comps) {
							Unit u = IntentUtils.getIntentParameterUnit(method);
							CodePoint cp = new CodePoint(u, method, Constants.APK_PATH, icccomp);
							Set<IType> revResult = genItypeForReceivingPoint(cp);
							itypesAtRPoints.put(cp, revResult);
							receivers.add(cp);
						}
					}
				}
			}
		}
		
		for(CodePoint cp: new HashSet<>(itypesAtSpoints.keySet())) {
			Value intentVal = IntentUtils.getSentIntentRef(cp.getUnit());
			Set<IType> result = AppInferrer.getInstance(Constants.APK_PATH).sigToSummary.get(cp.getMethod().getSignature()).get(cp.getUnit());
			Set<IType> revResult = getRelevantITypes(cp.getMethod(), intentVal, cp.getUnit(), result);
			itypesAtSpoints.put(cp, revResult);
		}
		
		for(CodePoint cp: new HashSet<>(itypesAtOtherPoints.keySet())) {
			Value intentVal = IntentUtils.getIntentBase(cp.getUnit());
			Set<IType> result = AppInferrer.getInstance(Constants.APK_PATH).sigToSummary.get(cp.getMethod().getSignature()).get(cp.getUnit());
			Set<IType> revResult = getRelevantITypes(cp.getMethod(), intentVal, cp.getUnit(), result);
			itypesAtOtherPoints.put(cp, revResult);
		}

	}

	private void setupComponents() {

		Set<Pair<SootClass, List<IntentFilter>>> dynRegReceivers = new HashSet<>();
		List<Pair<SootClass, SootMethod>> registerationPoints = AnalysisUtils
				.getDynamicRegRecieversEntryPointsByMethods(dynRegReceivers,
						new ArrayList<>(Hierarchy.getInstance(Constants.APK_PATH).appMethods),
						Hierarchy.getInstance(Constants.APK_PATH));

		Boolean isChanged = true;
		List<SootMethod> initial = new ArrayList<>(AnalysisUtils.findEntryPoints(parser.components.keySet(),
				parser.getOnClicks(), Hierarchy.getInstance(Constants.APK_PATH).appMethods, new HashSet<SootMethod>()));
		while (isChanged) {
			List<SootMethod> entryPoints = new ArrayList<>(
					AnalysisUtils.findEntryPoints(parser.components.keySet(), parser.getOnClicks(),
							Hierarchy.getInstance(Constants.APK_PATH).appMethods, new HashSet<SootMethod>(initial)));
			Scene.v().setEntryPoints(entryPoints);
			CallGraphAnalysis cga = new CallGraphAnalysis();
			isChanged = false;
			List<SootClass> newReceivers = new ArrayList<>();
			for (Pair<SootClass, SootMethod> p : registerationPoints) {
				Set<ICCResponsibleComponent> set = cga.backTrackCallerSequence(p.getO2(), parser.components);
				if (!set.isEmpty() && !parser.components.containsKey(p.getO1().getName())) {
					isChanged = true;
					newReceivers.add(p.getO1());
				}
			}
			addToComponents(newReceivers, dynRegReceivers);
		}

	}

	private void addToComponents(List<SootClass> newReceivers,
			Set<Pair<SootClass, List<IntentFilter>>> dynRegReceivers) {
		for (Pair<SootClass, List<IntentFilter>> p : dynRegReceivers) {
			if (newReceivers.contains(p.getO1())) {
				addToComponents(p);
			}
		}

	}

	private void addToComponents(Pair<SootClass, List<IntentFilter>> dynRegReceivers) {
		Component comp = new Component(dynRegReceivers.getO1().getName(), Constants.APK_PATH,
				COMPONENT.BROADCAST_RECEIVER);
		for (IntentFilter inf : dynRegReceivers.getO2()) {
			comp.addIntentFilter(inf);
		}
		parser.components.put(comp.getName(), comp);

	}

	public void doReachabilityAnalysisOnMethod(SootMethod method, Set<ICCResponsibleComponent> comps,
			BriefUnitGraph ug) {

		Options.v().set_time(false);

		for (Unit u : method.retrieveActiveBody().getUnits()) {
			doReachabilityAnalysisOnUnit(u, method, ug, comps);

		}
	}

	public void doReachabilityAnalysisOnUnit(Unit u, SootMethod method, BriefUnitGraph ug,
			Set<ICCResponsibleComponent> comps) {
		if (isInvocationOf(INVOCATION_TYPE.DEFINING, u)) {
			logger.info("DefPOINT compute iTypes at " + u + " in " + method);
		}

		if (isInvocationOf(INVOCATION_TYPE.INITIATING_INTENT, u)) {
			logger.info("SPOINT compute iTypes at " + u + " in " + method);
			for (ICCResponsibleComponent icccomp : comps) {
				CodePoint sp = new CodePoint(u, method, Constants.APK_PATH, icccomp);
				AppInferrer.getInstance(Constants.APK_PATH).getITypeReachableSetAtUnit(method, u);
				itypesAtSpoints.put(sp, new HashSet<>());
				senders.add(sp);
			}
		}
		if (isInvocationOf(INVOCATION_TYPE.RETREVING, u)) {
			logger.info("OPOINT compute iTypes at " + u + " in " + method);
			for (ICCResponsibleComponent icccomp : comps) {
				CodePoint cp = new CodePoint(u, method, Constants.APK_PATH, icccomp);
				AppInferrer.getInstance(Constants.APK_PATH).getITypeReachableSetAtUnit(method, u);
				itypesAtOtherPoints.put(cp, new HashSet<>());

			}
		}
		if (isInvocationOf(INVOCATION_TYPE.RECEIVING, u)) {
			logger.info("Found receinving invocation " + u + " in " + method);
			if (!(u instanceof DefinitionStmt)) {
				if (!u.toString().contains("setResult"))
					return;
			}

			for (ICCResponsibleComponent icccomp : comps) {
				CodePoint cp = new CodePoint(u, method, Constants.APK_PATH, icccomp);
				Set<IType> revResult = genItypeForReceivingPoint(cp);
				receivers.add(cp);
				itypesAtRPoints.put(cp, revResult);

			}
		}

	}

//	private void updateWithNewVals(Map<CodePoint, Set<IType>> map, CodePoint cp, Value intentVal) {
//		if (!map.containsKey(cp))
//			return;
//		BriefUnitGraph ug = new BriefUnitGraph(cp.getMethod().retrieveActiveBody());
//		Set<IType> result = AppInferrer.getInstance(Constants.APK_PATH).getITypeReachableSetAtUnit(ug, cp.getUnit());
//		Set<IType> revResult = getRelevantITypes(cp.getMethod(), intentVal, cp.getUnit(), result);
//		map.put(cp, revResult);
//	}

	protected boolean isInvocationOf(INVOCATION_TYPE inkOfInterest, Unit u) {
		INVOCATION_TYPE ink = AnalysisUtils.unitNeedsAnalysis(u);
		if (ink == null)
			return false;
		if (u.toString().contains("void setResult(int") && inkOfInterest.equals(INVOCATION_TYPE.INITIATING_INTENT))
			return true;
		if (u.toString().contains("void setResult(int") && inkOfInterest.equals(INVOCATION_TYPE.RECEIVING))
			return true;
		if (inkOfInterest.equals(ink))
			return true;
		return false;
	}

	private Set<IType> genItypeForReceivingPoint(CodePoint codePoint) {
		IType it = new IType(codePoint);
		IntentUtils.updateWithReceivingInfo(it, codePoint);
		return Set.of(it);
	}

//	private void updateITypesInvoked(SootMethod invokedMethod, Unit u, SootMethod containingMethod) {
//		updateITypesInvoked(invokedMethod, itypesAtSpoints, u, containingMethod);
//		updateITypesInvoked(invokedMethod, itypesAtOtherPoints, u, containingMethod);
//	}

//	private void updateITypesInvoked(SootMethod invokedMethod, Map<? extends CodePoint, Set<IType>> map, Unit u,
//			SootMethod containingMethod) {
//		for (CodePoint cp : map.keySet()) {
//
//			Set<IType> set = new HashSet<IType>();
//			for (IType it : map.get(cp)) {
//				set.addAll(applySummaryOfCallerMethod(it, containingMethod, u, invokedMethod));
//			}
//			map.get(cp).clear();
//			map.get(cp).addAll(set);
//
//		}
//
//	}

//	private Set<IType> applySummaryOfCallerMethod(IType it, SootMethod containingMethod, Unit u,
//			SootMethod invokedMethod) {
//		Set<IType> result = new HashSet<IType>();
//		Set<IType> allReached = AppInferrer.getInstance(Constants.APK_PATH)
//				.getITypeReachableSetAtUnit(new BriefUnitGraph(containingMethod.retrieveActiveBody()), u);
//		for (IType reached : getRelevantITypes(containingMethod, IntentUtils.getIntentValue(u), u, allReached)) {
//			result.add(updateITypeWithItype(it, reached));
//		}
//		if (result.isEmpty())
//			result.add(it);
//		return result;
//	}

	protected Set<IType> getRelevantITypes(SootMethod sm, Value intentVal, Unit u, Set<IType> set) {
		Set<IType> res = new HashSet<IType>();
		for (IType it : set) {
			if (AnalysisUtils.isSameReference(it.getDefinitionPoint(), intentVal, u, sm)) {
				res.add(it);

			}
		}
		return res;
	}

//	private Set<IType> updateItFromSummaryOfInvoked(IType it, SootMethod pointDeclaringMethod, Unit point) {
//		Set<IType> res = new HashSet<IType>();
//		SootMethod sm = AnalysisUtils.getInvokedMethod(it.getDefinitionPoint().getUnit());
//		if (summary.get(sm) == null) {
//			addSummary(sm);
//		}
//		for (Unit u : summary.get(sm).keySet()) {
//
//			Value val = IntentUtils.getIntentValue(u);
//			if (val != null)
//				for (IType retIt : summary.get(sm).get(u)) {
//					if (AnalysisUtils.isSameReference(retIt.getDefinitionPoint(), val, u, sm))
//						res.add(updateITypeWithItype(it, retIt));
//				}
//
//		}
//		if (res.isEmpty())
//			res.add(it);
//		return res;
//	}

//	private void addSummary(SootMethod method) {
//		if (!summary.containsKey(method))
//			summary.put(method, new HashMap<Unit, Set<IType>>());
//		if (!method.isConcrete())
//			return;
//		if (!method.hasActiveBody())
//			return;
//		BriefUnitGraph ug = new BriefUnitGraph(method.retrieveActiveBody());
//		for (Unit u : ug.getTails()) {
//			if (!summary.get(method).containsKey(u))
//				summary.get(method).put(u, new HashSet<IType>());
//			summary.get(method).get(u)
//					.addAll(AppInferrer.getInstance(Constants.APK_PATH).getITypeReachableSetAtUnit(ug, u));
//		}
//
//	}

	private IType updateITypeWithItype(IType curr, IType retIt) {
		IType curr2 = null;
		try {
			curr2 = curr.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		for (ATTRIBUTE attr : curr.iattrsMap.keySet()) {
			for (CodePoint cp : curr.iattrsMap.get(attr)) {
				if (shouldbeSet(cp, curr.definiStmt)) {
					curr2.iattrsMap.get(attr).remove(cp);
					if (retIt.iattrsMap.containsKey(attr))
						curr2.iattrsMap.get(attr).addAll(retIt.iattrsMap.get(attr));
					else
						curr2.iattrsMap.remove(attr);
				}
			}

		}
		return curr2;
	}

	private boolean shouldbeSet(CodePoint cp, CodePoint s) {
		if (s.equals(cp))
			return true;
		return false;
	}

	private boolean isReturnedFromAnotherMethod(CodePoint cp) {

		if (AnalysisUtils.isApplicationMethodInvoked(cp.getUnit())) {
			return true;
		}

		return false;

	}

	private boolean isParameterOfMethod(CodePoint cp) {

		if (cp.getUnit() instanceof IdentityStmt) {
			IdentityStmt istmt = (IdentityStmt) cp.getUnit();
			if (IntentUtils.isIntent(istmt.getLeftOp()))
				return true;

		}
		return false;

	}

	public Set<CodePoint> getSenders() {
		return senders;
	}

	public Map<CodePoint, Set<IntentIR>> getIntetIRAtSpoints() {
		return intentsAtSpoints;
	}

	public Map<CodePoint, Set<IntentIR>> getIntetIRAtOtherpoints() {
		return intentsAtOtherPoints;
	}

	public Map<CodePoint, Set<IntentIR>> getIntetIRAtRpoints() {
		return intentsAtRpoints;
	}

}
