package usc.edu.sql.fpa.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;
import usc.edu.sql.fpa.analysis.IRAnalysis;
import usc.edu.sql.fpa.analysis.intra.AppInferrer;
import usc.edu.sql.fpa.utils.Constants.ATTRIBUTE;
import usc.sql.ir.ConstantString;
import usc.sql.ir.Expression;
import usc.sql.ir.InternalVar;
import usc.sql.ir.Operation;
import usc.sql.ir.Variable;
import usc.sql.string.utils.Util;

public class URIUtils {
	public static Value getURIDefinition(Unit unit) {
		if (unit instanceof DefinitionStmt) {
			if (isURI(((DefinitionStmt) unit).getLeftOp())) {
				return ((DefinitionStmt) unit).getLeftOp();
			}
		}
		return null;
	}

	public static boolean isURI(Value v) {
		if (v.getType().toString().equals("android.net.Uri"))
			return true;
		return false;
	}

	public static Set<Variable> getIRForURIDef(Unit uriDef, SootMethod sm) {
		IRAnalysis iranalysis = IRAnalysis.getInstance(Constants.APK_PATH);
		DefinitionStmt stmt = (DefinitionStmt) uriDef;
		if (stmt.containsInvokeExpr()) {
			InvokeExpr invokeExpr = stmt.getInvokeExpr();
			if (invokeExpr.getMethod().getSignature()
					.contains("<android.net.Uri: android.net.Uri parse(java.lang.String)>")) {
				return iranalysis.getIRsForInvocation(sm, stmt, "0", ATTRIBUTE.CATEGOR);
			}
			if (invokeExpr.getMethod().getSignature()
					.contains("<android.net.Uri: android.net.Uri fromFile(java.io.File)>")) {

				return generateFileUri(stmt, sm, invokeExpr.getArg(0), iranalysis);
			}
			if (invokeExpr.getMethod().getSignature().contains("<android.content.Intent: android.net.Uri getData()>")) {
				return generateGetDataExpression(stmt, sm);
			}
		}

		return null;
	}

	private static Set<Variable> generateFileUri(DefinitionStmt stmt, SootMethod sm, Value arg, IRAnalysis iranalysis) {
		Set<Variable> set = new HashSet<Variable>();
		if (arg instanceof Local) {
			Local file = (Local) arg;
			SimpleLocalDefs defs = AppInferrer.getInstance(Constants.APK_PATH).getDefs(sm);
			SimpleLocalUses uses = AppInferrer.getInstance(Constants.APK_PATH).getUses(sm);

			for (Unit def : defs.getDefsOfAt(file, stmt)) {
				for (UnitValueBoxPair use : uses.getUsesOf(def)) {
					if (((Stmt) use.getUnit()).containsInvokeExpr()) {
						InvokeExpr expr = ((Stmt) use.getUnit()).getInvokeExpr();
						if (expr.getMethod().getSignature().contains("<java.io.File: void <init>(java.lang.String)>")) {
							set.addAll(iranalysis.getIRsForInvocation(sm, stmt, "0", ATTRIBUTE.DATA));
						} else if (expr.getMethod().getSignature()
								.contains("<java.io.File: void <init>(java.lang.String,java.lang.String)>")) {
							set.addAll(iranalysis.getIRsForInvocation(sm, stmt, "0", ATTRIBUTE.DATA));
						}
					}
				}
			}
		}
		if (set.isEmpty())
			set.add(new ConstantString("file://.*"));
		return set;
	}

	private static Set<Variable> generateGetDataExpression(DefinitionStmt stmt, SootMethod sm) {
		List<Variable> operands = new ArrayList<>();
		Operation op = null;
		InstanceInvokeExpr expr = (InstanceInvokeExpr) stmt.getInvokeExpr();
		InternalVar intentVar = new InternalVar(expr.getBase().toString());
		operands.add(intentVar);

		if (expr.getMethod().getName().contains("getData")) {
			op = new Operation("getData");
		}
		List<List<Variable>> operandList = new ArrayList<>();
		for (Variable opd : operands) {
			List<Variable> temp = new ArrayList<>();
			temp.add(opd);
			operandList.add(temp);
		}
		Set<Variable> set = new HashSet<Variable>();
		Expression exp = new Expression(operandList, op, Util.getBytecodeOffset(stmt), sm.getSignature());
		set.add(exp);
		return set;
	}
}
