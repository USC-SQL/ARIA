package usc.edu.sql.fpa.analysis.intra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Local;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import usc.edu.sql.fpa.model.CodePoint;
import usc.edu.sql.fpa.model.Intent;
import usc.edu.sql.fpa.utils.AnalysisUtils;
import usc.edu.sql.fpa.utils.Constants;
import usc.edu.sql.fpa.utils.Parser;

import java.util.*;

public class AppInferrer {
	private static AppInferrer inferrer;
	private static String app;
	public Map<String, Map<Unit, Set<IType>>> sigToSummary = new HashMap<>(); // sig to summary which is a set of
	private Map<String, Set<String>> paramMethodToMethod = new HashMap<>();																			// itypes that can

	// reach the exit
	private Map<String, IntentReachability> cachedIntentReachability = new HashMap<>();
	private Map<String, URIReachability> cachedURIReachability = new HashMap<>();
	private Map<String, SimpleLocalDefs> cachedDefs = new HashMap<>();
	private Map<String, SimpleLocalUses> cachedUses = new HashMap<>();

	public AppInferrer(String app) {
		AppInferrer.app = app;
	}

	public void getITypeReachableSetAtUnit(SootMethod sm, Unit unit) {
		getITypeReachableSetAtUnit(new BriefUnitGraph(sm.retrieveActiveBody()), unit);

	}

	public void getITypeReachableSet(SootMethod sm) {
		if (!cachedIntentReachability.containsKey(sm.getSignature())) {
			IntentReachability ir = new IntentReachability(new BriefUnitGraph(sm.retrieveActiveBody()), sigToSummary);
			cachedIntentReachability.put(sm.getSignature(), ir);
			updateSummary(ir, sm);
		}
	}

	public void getITypeReachableSetAtUnit(BriefUnitGraph ug, Unit unit) {
		String sig = ug.getBody().getMethod().getSignature();
		if (cachedIntentReachability.get(sig) == null) {
			IntentReachability ir = new IntentReachability(ug, sigToSummary);
			cachedIntentReachability.put(sig, ir);
			updateSummary(ir, ug.getBody().getMethod());
		}
		CodePoint key = new CodePoint(unit, ug.getBody().getMethod(), Constants.APK_PATH);
		Set<IType> set = cachedIntentReachability.get(sig).getReachableTable().get(key);
		if (!sigToSummary.get(sig).containsKey(unit))
			sigToSummary.get(sig).put(unit, new HashSet<>());
		sigToSummary.get(sig).get(unit).addAll(set);
	}

	public Set<CodePoint> getURIReachability(BriefUnitGraph ug, Unit u) {
		String sig = ug.getBody().getMethod().getSignature();
		if (!cachedURIReachability.containsKey(sig)) {
			URIReachability ir = new URIReachability(ug);
			cachedURIReachability.put(sig, ir);
		}
		CodePoint key = new CodePoint(u, ug.getBody().getMethod(), Constants.APK_PATH);
		Set<CodePoint> res = cachedURIReachability.get(sig).getReachableTable().get(key);
		return res;
	}

	public SimpleLocalDefs getDefs(SootMethod sm) {
		if (!cachedDefs.containsKey(sm.getSignature())) {
			cachedDefs.put(sm.getSignature(), new SimpleLocalDefs(new BriefUnitGraph(sm.retrieveActiveBody())));
		}
		return cachedDefs.get(sm.getSignature());
	}

	public SimpleLocalUses getUses(SootMethod sm) {
		if (!cachedUses.containsKey(sm.getSignature())) {
			cachedUses.put(sm.getSignature(), new SimpleLocalUses(new BriefUnitGraph(sm.retrieveActiveBody()),
					cachedDefs.get(sm.getSignature())));

		}
		return cachedUses.get(sm.getSignature());
	}

	public static AppInferrer getInstance(String app) {
		if (inferrer == null || !AppInferrer.app.equals(app)) {
			inferrer = new AppInferrer(app);
		}
		return inferrer;
	}

//	public boolean isSameReference(CodePoint definitionPoint, Value intentValue, Unit unit, String method) {
//		for (String sm : cachedDefs.keySet()) {
//			if (sm.equals(method)) {
//
//				return AnalysisUtils.isSameReference(definitionPoint, intentValue, unit, sm);
//			}
//		}
//		return false;
//	}
	private void updateSummary(IntentReachability ir, SootMethod sm) {
		if (!sigToSummary.containsKey(sm.getSignature())) {
			sigToSummary.put(sm.getSignature(), new HashMap<>());
		}
		for (Unit u : new BriefUnitGraph(sm.retrieveActiveBody()).getTails()) {
			CodePoint key = new CodePoint(u, sm, Constants.APK_PATH);
			sigToSummary.get(sm.getSignature()).put(u, ir.getReachableTable().get(key));
		}

	}
}
