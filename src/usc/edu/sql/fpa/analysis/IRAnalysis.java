package usc.edu.sql.fpa.analysis;

import java.util.*;

import CallGraph.StringCallGraph;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ClassConstant;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.util.Chain;
import usc.edu.sql.fpa.analysis.interpret.IntentInterpreter;
import usc.edu.sql.fpa.model.CodePoint;
import usc.edu.sql.fpa.model.IntentIR;
import usc.edu.sql.fpa.utils.AnalysisUtils;
import usc.edu.sql.fpa.utils.Constants;
import usc.edu.sql.fpa.utils.IntentUtils;
import usc.edu.sql.fpa.utils.Constants.ATTRIBUTE;
import usc.sql.ir.ConstantString;
import usc.sql.ir.Expression;
import usc.sql.ir.InternalVar;
import usc.sql.ir.NullVariable;
import usc.sql.ir.Operation;
import usc.sql.ir.Variable;
import usc.sql.string.JavaAndroid;
import usc.sql.string.utils.Util;

public class IRAnalysis {
	private static IRAnalysis irAnalysis;
	private static StringCallGraph callgraph;
	private static HashMap<String, List<Integer>> TARGET = new HashMap<>();
	private static Map<String, Set<Variable>> irs = new HashMap<String, Set<Variable>>();
	private static Map<String, Set<String>> vals = new HashMap<String, Set<String>>();
	private static Map<String, Map<String, Integer>> irOps = new HashMap<String, Map<String, Integer>>();

	private static String appPath;
	static {
		TARGET.put("<java.io.File: void <init>(java.lang.String)>", Arrays.asList(0));
		TARGET.put("<java.io.File: void <init>(java.lang.String,java.lang.String)>", Arrays.asList(0, 1));
		TARGET.put("<android.content.IntentFilter: void <init>(java.lang.String)>", Arrays.asList(0));
		TARGET.put("<android.content.IntentFilter: void <init>(java.lang.String,java.lang.String)>",
				Arrays.asList(0, 1));
		TARGET.put(
				"<android.content.IntentFilter: android.content.IntentFilter create(java.lang.String,java.lang.String)>",
				Arrays.asList(0, 1));
		TARGET.put("<android.content.IntentFilter: void addAction(java.lang.String)>", Arrays.asList(0));
		TARGET.put("<android.content.IntentFilter: void addDataType(java.lang.String)>", Arrays.asList(0));
		TARGET.put("<android.content.IntentFilter: void addDataScheme(java.lang.String)>", Arrays.asList(0));
		TARGET.put("<android.content.IntentFilter: void addDataAuthority(java.lang.String,java.lang.String)>",
				Arrays.asList(0, 1));
		TARGET.put("<android.content.IntentFilter: void addDataPath(java.lang.String,int)>", Arrays.asList(0));
		TARGET.put("<android.content.IntentFilter: void addCategory(java.lang.String)>", Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,java.lang.String)>",
				Arrays.asList(0, 1));
		TARGET.put("<android.content.Intent: android.content.Intent setPackage(java.lang.String)>", Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,java.lang.String)>",
				Arrays.asList(0, 1));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,boolean)>",
				Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,byte)>",
				Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,char)>",
				Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,short)>",
				Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,int)>", Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,long)>",
				Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,float)>",
				Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,double)>",
				Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,java.lang.CharSequence)>",
				Arrays.asList(0, 1));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,android.os.Parcelable)>",
				Arrays.asList(0));
		TARGET.put(
				"<android.content.Intent: android.content.Intent putExtra(java.lang.String,android.os.Parcelable[])>",
				Arrays.asList(0));
		TARGET.put(
				"<android.content.Intent: android.content.Intent putParcelableArrayListExtra(java.lang.String,java.util.ArrayList)>",
				Arrays.asList(0));
		TARGET.put(
				"<android.content.Intent: android.content.Intent putIntegerArrayListExtra(java.lang.String,java.util.ArrayList)>",
				Arrays.asList(0));
		TARGET.put(
				"<android.content.Intent: android.content.Intent putStringArrayListExtra(java.lang.String,java.util.ArrayList)>",
				Arrays.asList(0));
		TARGET.put(
				"<android.content.Intent: android.content.Intent putCharSequenceArrayListExtra(java.lang.String,java.util.ArrayList)>",
				Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,java.io.Serializable)>",
				Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,boolean[])>",
				Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,byte[])>",
				Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,short[])>",
				Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,char[])>",
				Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,int[])>",
				Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,long[])>",
				Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,float[])>",
				Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,double[])>",
				Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,java.lang.String[])>",
				Arrays.asList(0, 1));
		TARGET.put(
				"<android.content.Intent: android.content.Intent putExtra(java.lang.String,java.lang.CharSequence[])>",
				Arrays.asList(0, 1));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,android.os.Bundle)>",
				Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,int)>", Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent putExtra(java.lang.String,android.os.Parcelable)>",
				Arrays.asList(0));
		TARGET.put("<android.content.Intent: void <init>(java.lang.String,android.net.Uri)>", Arrays.asList(0));
		TARGET.put("<android.content.Intent: void <init>(java.lang.String)>", Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent setAction(java.lang.String)>", Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent setType(java.lang.String)>", Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent setDataAndType(android.net.Uri,java.lang.String)>",
				Arrays.asList(1));
		TARGET.put(
				"<android.content.Intent: android.content.Intent setDataAndTypeAndNormalize(android.net.Uri,java.lang.String)>",
				Arrays.asList(1));
		TARGET.put("<android.content.Intent: android.content.Intent setTypeAndNormalize(java.lang.String)>",
				Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent addCategory(java.lang.String)>", Arrays.asList(0));
		TARGET.put("<android.content.Intent: android.content.Intent setClassName(java.lang.String,java.lang.String)>",
				Arrays.asList(0, 1));
		TARGET.put(
				"<android.content.Intent: android.content.Intent setClassName(android.content.Context,java.lang.String)>",
				Arrays.asList(1));
		TARGET.put("<android.content.Intent: java.lang.String getStringExtra(java.lang.String)>", Arrays.asList(0));
		TARGET.put("<android.content.Intent: int getIntExtra(java.lang.String,int)>", Arrays.asList(0));
		TARGET.put("<android.net.Uri: android.net.Uri parse(java.lang.String)>", Arrays.asList(0));
		TARGET.put("<android.net.Uri: android.net.Uri fromParts(java.lang.String,java.lang.String,java.lang.String)>",
				Arrays.asList(0));
		TARGET.put("<android.content.ComponentName: void <init>(android.content.Context,java.lang.String)>",
				Arrays.asList(1));

		TARGET.put("<android.content.ComponentName: void <init>(java.lang.String,java.lang.String)>",
				Arrays.asList(0, 1));
	}

	public IRAnalysis(String path) {
		IRAnalysis.appPath = path;
		setup();
		analyze();
	}

	public void setup() {
		long t1 = System.currentTimeMillis();
		Chain<SootClass> classes = Scene.v().getApplicationClasses();
		List<SootMethod> entryPoints = new ArrayList<>();
		Set<SootMethod> allMethods = new HashSet<>();
		for (Iterator<SootClass> iter = classes.iterator(); iter.hasNext();) {
			SootClass sc = iter.next();
			if (!sc.getName().startsWith("android") && !sc.getName().startsWith("java")) {
				sc.setApplicationClass();
				allMethods.addAll(sc.getMethods());
				for (SootMethod sm : sc.getMethods()) {
					if (sm.isConcrete()) {
						entryPoints.add(sm);
					}

				}
			}
		}
		Scene.v().loadNecessaryClasses();
		Scene.v().setEntryPoints(entryPoints);
		CHATransformer.v().transform();
		CallGraph cg = Scene.v().getCallGraph();
		long t2 = System.currentTimeMillis();
		System.out.println("Running soot takes " + (t2 - t1) + "ms");
		callgraph = new StringCallGraph(cg, allMethods);

	}

	public void analyze() {
		JavaAndroid ja = new JavaAndroid(callgraph, TARGET, Constants.MAX_LOOPS, Constants.output);
		irs = ja.getIRs();
		irOps = ja.getIROpStatistics();
	}

	public Set<Variable> getIRsForInvocation(SootMethod sm, Unit u, String i, ATTRIBUTE attr) {
		Set<Variable> set = new HashSet<Variable>();
		if (ATTRIBUTE.ACTION.equals(attr) && u.toString().contains("createChooser")) {
			set.add(new ConstantString("android.intent.action.CHOOSER"));
			return set;
		}
		if (ATTRIBUTE.CATEGOR.equals(attr) && u.toString().contains("getLaunchIntentForPackage")) {
			set.add(new ConstantString("android.intent.category.INFO"));
			set.add(new ConstantString("android.intent.category.LAUNCHER"));
			return set;
		}

		for (String key : irs.keySet()) {
			String[] chainInfo = key.split("->\n");
			String[] invokeInfo = chainInfo[chainInfo.length - 1].split("@");
			String declaringMethodSig = invokeInfo[0];
			String unitHash = invokeInfo[2];
			String argInd = invokeInfo[4];
			if (declaringMethodSig.equals(sm.getSignature())) {
				if (unitHash.equals(Util.getBytecodeOffset(u) + "")) {
					if (i.equals(argInd.strip())) {
						for (Variable v : irs.get(key)) {
							if (v.toString().contains("android.content.Context: java.lang.String getPackageName()")) {
								set.add(new ConstantString(Constants.APP_PACKAGE));
							} else {
								set.add(v);
							}
						}
						return set;

					}
				}
			}
		}

		InvokeExpr expr = AnalysisUtils.getInvoke(u);
		System.out.println(expr);
		if (expr != null && expr.getArgCount() > Integer.parseInt(i) && Integer.parseInt(i) >= 0) {
			if (expr.getArg(Integer.parseInt(i)) instanceof StringConstant) {
				String str = ((StringConstant) expr.getArg(Integer.parseInt(i))).value;
				set.add(new ConstantString(str));
			}
			if (expr.getArg(Integer.parseInt(i)) instanceof NullConstant) {
				set.add(new ConstantString(Constants.NULL));
			}
			if (expr.getArg(Integer.parseInt(i)) instanceof ClassConstant) {
				set.addAll(getClassConstantVariable(expr, Integer.parseInt(i)));
			}
			if (expr.getArg(Integer.parseInt(i)).getType().toString().contains("boolean")) {
				set.add(new ConstantString(Constants.BOOL));
			}
		}
		return set;
	}

	public static IRAnalysis getInstance(String path) {
		if (irAnalysis == null || !path.equals(IRAnalysis.appPath)) {
			irAnalysis = new IRAnalysis(path);
		}
		return irAnalysis;

	}

	public static ATTRIBUTE isIntentRelatedOp(Variable v) {
		if (!(v instanceof Expression))
			return null;

		Operation op = ((Expression) v).getOperation();
		System.out.println("interpreting variable v " + v + " with op " + op);
		if (op.getName().startsWith("get")) {
			if (op.getName().contains("Action")) {
				return ATTRIBUTE.ACTION;
			}
			if (op.getName().contains("Res")) {
				return ATTRIBUTE.RES;
			}
			if (op.getName().contains("Data")) {
				return ATTRIBUTE.DATA;
			}
			if (op.getName().contains("Type")) {
				return ATTRIBUTE.TYPE;
			}
			if (op.getName().contains("Extra")) {
				return ATTRIBUTE.EXTRA;
			}
			if (op.getName().contains("Component")) {
				return ATTRIBUTE.COMPONENT;
			}
			if (op.getName().contains("Categor")) {
				return ATTRIBUTE.CATEGOR;
			}
			if (op.getName().contains("Package")) {
				return ATTRIBUTE.PACKAGE;
			}
		}

		return null;
	}

	public static Boolean dependsOnIntentOp(Variable v) {
		if (isIntentRelatedOp(v) != null) {
			return true;
		}
		if (v instanceof Expression) {
			for (List<Variable> operand : ((Expression) v).getOperands()) {
				for (Variable var : operand) {
					if (isIntentRelatedOp(var) != null)
						return true;
				}
			}

		}
		return false;
	}

//	public static boolean isVariableFromReceivedIntent(Variable v, CodePoint defPoint) {
//		if (isIntentRelatedOp(v) != null) {
//			Expression expr = (Expression) v;
//			if (expr.getUnitOffset().equals(defPoint.getUnit())
//					&& defPoint.getMethod().getSignature().equals(expr.getMethod()))
//				return true;
//		}
//		return false;
//	}

	public Set<Variable> getClassConstantVariable(InvokeExpr invokeExpr, int argIndex) {
		Value val = invokeExpr.getArg(argIndex);
		if (val instanceof ClassConstant) {
			Set<Variable> set = new HashSet<Variable>();
			set.add(new ConstantString(((ClassConstant) val).value.substring(1).replace("/", ".")));
			return set;
		}
		return new HashSet<>();
	}

	public Set<Variable> generateIRforReceivedInformation(CodePoint currPoint, ATTRIBUTE attr) {
		if (ATTRIBUTE.RES.equals(attr)) {
			if (currPoint.getUnit().toString().contains("setResult"))
				return generateIRForSetRes(currPoint);
			return new HashSet<>();
		}

		List<Variable> operands = new ArrayList<>();
		Operation op = null;
		System.out.println(currPoint);
		String intentStr = "";
		if (currPoint.getUnit() instanceof DefinitionStmt) {
			intentStr = ((DefinitionStmt) currPoint.getUnit()).getLeftOp().toString();

		}
		InternalVar intentVar = new InternalVar(intentStr);
		operands.add(intentVar);

		if (ATTRIBUTE.TYPE.equals(attr)) {
			op = new Operation("getType");
		} else if (ATTRIBUTE.ACTION.equals(attr)) {
			op = new Operation("getAction");
		} else if (ATTRIBUTE.SCHEME.equals(attr)) {
			op = new Operation("getScheme");
		} else if (ATTRIBUTE.DATA.equals(attr)) {
			op = new Operation("getData");
		} else if (ATTRIBUTE.EXTRA.equals(attr)) {
			op = new Operation("getStringExtra");
			Variable keyVar = new ConstantString(Constants.ALL_EXTRA_KEY);
			// Variable keyVar = new
			// InternalVar(AnalysisUtils.getInvoke(currPoint.getUnit()).getArg(0).toString());
			operands.add(keyVar);
		} else if (attr.equals(ATTRIBUTE.COMPONENT)) {
			op = new Operation("getComponent");
		} else if (ATTRIBUTE.CATEGOR.equals(attr)) {
			op = new Operation("getCategor");
		} else if (ATTRIBUTE.PACKAGE.equals(attr)) {
			op = new Operation("getPackage");
		} else {
			op = new Operation("getUnknown");
			System.out.println("NOT HANDLED " + attr);
		}
		List<List<Variable>> operandList = new ArrayList<>();
		for (Variable opd : operands) {
			List<Variable> temp = new ArrayList<>();
			temp.add(opd);
			operandList.add(temp);
		}
		Expression exp = new Expression(operandList, op, Util.getBytecodeOffset(currPoint.getUnit()),
				currPoint.getMethod().getSignature());
		Set<Variable> set = new HashSet<>();
		set.add(exp);
		return set;
	}

	public static boolean isAllExtraVariable(Variable v) {
		if (v instanceof Expression) {
			Expression e = (Expression) v;
			if (e.getOperation().getName().equals("getStringExtra")) {
				for (List<Variable> opd : e.getOperands()) {
					if (opd.get(0) instanceof ConstantString) {
						return ((ConstantString) opd.get(0)).getValue().equals(Constants.ALL_EXTRA_KEY);
					}
				}
			}

		}
		return false;
	}

	public static Variable generateNullCosntant() {

		return new NullVariable();
	}

	public Map<String, Set<Variable>> getIrs() {
		return irs;
	}

	public static boolean hasVariable(Set<Variable> set) {
		for (Variable v : set) {
			if (v instanceof Expression) {
				if (!((Expression) v).getOperation().toString().contains("append"))
					return true;
			}
		}
		return false;
	}

	public static Variable getIntentRelatedExtra(Map<Variable, Set<Variable>> extraMap) {
		for (Variable vk : extraMap.keySet()) {
			if (vk instanceof ConstantString) {
				if (((ConstantString) vk).getValue().equals(Constants.ALL_EXTRA_KEY)) {
					if (extraMap.get(vk).size() == 1
							&& ATTRIBUTE.EXTRA.equals(isIntentRelatedOp(extraMap.get(vk).iterator().next()))) {
						return extraMap.get(vk).iterator().next();
					}
				}
			}
		}
		return null;
	}

	public StringCallGraph getSCG() {
		// TODO Auto-generated method stub
		return callgraph;
	}

	public Set<Variable> generateIRForSetRes(CodePoint currPoint) {
		List<Variable> operands = new ArrayList<>();
		Operation op = new Operation("getRes");
		String intentStr = "";
		if (currPoint.getUnit() instanceof Stmt) {
			InvokeExpr iexpr = ((Stmt) currPoint.getUnit()).getInvokeExpr();
			if (iexpr.getArgs().size() > 1) {
				intentStr = iexpr.getArg(1).toString();
			}

		}
		InternalVar intentVar = new InternalVar(intentStr);
		operands.add(intentVar);

		List<List<Variable>> operandList = new ArrayList<>();
		for (Variable opd : operands) {
			List<Variable> temp = new ArrayList<>();
			temp.add(opd);
			operandList.add(temp);
		}
		Expression exp = new Expression(operandList, op, Util.getBytecodeOffset(currPoint.getUnit()),
				currPoint.getMethod().getSignature());
		Set<Variable> set = new HashSet<>();
		set.add(exp);
		return set;
	}

}
