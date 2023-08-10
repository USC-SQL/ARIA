package usc.edu.sql.fpa.analysis.intra;

import com.google.common.collect.Lists;

import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;
import usc.edu.sql.fpa.model.Component;
import usc.edu.sql.fpa.model.ICCResponsibleComponent;
import usc.edu.sql.fpa.utils.APINameUtil;
import usc.edu.sql.fpa.utils.Constants.COMPONENT;

import java.util.*;
import java.util.stream.Collectors;

public class CallGraphAnalysis {
	private final CallGraph cg;
	private final Map<SootMethod, Set<ICCResponsibleComponent>> callerTraces = new HashMap<>();

	public CallGraphAnalysis() {
		// AndroidProcessor androidProcessor = new AndroidProcessor();
		Options.v().set_no_writeout_body_releasing(true);
		Options.v().set_time(false);
		CHATransformer.v().transform();
		cg = Scene.v().getCallGraph();
	}

	public Set<ICCResponsibleComponent> backTrackCallerSequence(SootMethod sm, Map<String, Component> components) {
		if (callerTraces.containsKey(sm)) {
			return callerTraces.get(sm);
		} else {
			Set<ICCResponsibleComponent> callerTraces = new HashSet<>();
			ArrayDeque<SootMethod> curTrace = new ArrayDeque<>();
			Set<SootMethod> visited = new HashSet<>();
			curTrace.add(sm);
			visited.add(sm);
			backwardDFS(curTrace, callerTraces, visited, components);
			this.callerTraces.put(sm, callerTraces);
			return callerTraces;
		}
	}

	private void backwardDFS(ArrayDeque<SootMethod> curTrace, Set<ICCResponsibleComponent> callerTraces2,
			Set<SootMethod> visited, Map<String, Component> components) {
		if (curTrace.size() > 500) {
			curTrace.pollLast();
			return;
		}
		SootMethod last = curTrace.getLast();
		if (Scene.v().getEntryPoints().contains(last) || cg.isEntryMethod(last)) {
			// stop dfs at program entries
			List<SootMethod> copyTrace = new ArrayList<>(curTrace.stream()
					.filter(it -> !it.getSignature().contains("dummyMain")).collect(Collectors.toList()));
			SootMethod responsibleCaller = copyTrace.get(copyTrace.size() - 1);
			if (components.containsKey(responsibleCaller.getDeclaringClass().getName()))
				callerTraces2.add(new ICCResponsibleComponent(responsibleCaller,
						ICCResponsibleComponent.getScenarioType(responsibleCaller),
						components.get(responsibleCaller.getDeclaringClass().getName())));
			else
				callerTraces2.add(new ICCResponsibleComponent(responsibleCaller,
						ICCResponsibleComponent.getScenarioType(responsibleCaller), null));

			curTrace.pollLast();
			return;
		}
		List<SootMethod> predecessors = getPredecessors(last);
		predecessors.removeIf(visited::contains);
		for (SootMethod prec : predecessors) {
			if (visited.contains(prec)) {
				continue;
			}
			curTrace.addLast(prec);
			visited.add(prec);
			backwardDFS(curTrace, callerTraces2, visited, components);
		}
		curTrace.pollLast();
	}

	private List<SootMethod> getPredecessors(SootMethod method) {
		Iterator<Edge> edgeIterator = cg.edgesInto(method);
		return Lists.newArrayList(edgeIterator).stream().map(it -> it.getSrc().method()).collect(Collectors.toList());
	}

	public boolean isReacheable(SootMethod method) {
		List<Edge> inEdge = Lists.newArrayList(cg.edgesInto(method));
		List<Edge> outEdge = Lists.newArrayList(cg.edgesOutOf(method));
		return !inEdge.isEmpty() || !outEdge.isEmpty();
	}

}
