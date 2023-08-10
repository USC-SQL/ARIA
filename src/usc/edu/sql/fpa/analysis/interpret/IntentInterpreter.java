package usc.edu.sql.fpa.analysis.interpret;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.PatternSyntaxException;

import soot.Unit;
import soot.Value;
import soot.tagkit.BytecodeOffsetTag;
import soot.tagkit.Tag;
import usc.edu.sql.fpa.analysis.IRAnalysis;
import usc.edu.sql.fpa.analysis.bundle.BundleAnalysis;
import usc.edu.sql.fpa.analysis.bundle.INode;
import usc.edu.sql.fpa.analysis.intra.AppInferrer;
import usc.edu.sql.fpa.analysis.intra.IntentReachability;
import usc.edu.sql.fpa.model.CodePoint;
import usc.edu.sql.fpa.model.IC3Intent;
import usc.edu.sql.fpa.model.IntentIR;
import usc.edu.sql.fpa.utils.AnalysisUtils;
import usc.edu.sql.fpa.utils.Constants;
import usc.edu.sql.fpa.utils.Hierarchy;
import usc.edu.sql.fpa.utils.IntentUtils;
import usc.edu.sql.fpa.utils.Constants.ATTRIBUTE;
import usc.sql.ir.*;

public class IntentInterpreter {
	private int maxLoop = Constants.MAX_LOOPS;
	private Set<Variable> target;
	Map<String, Set<String>> fieldMap;
	private Map<Variable, List<Set<String>>> tList = new HashMap<>();
	private INode pre = null;
	private IC3Intent currentIncomingIntent = null;

	public IntentInterpreter() {

	}

	public Set<IC3Intent> interpret(INode from, IntentIR intentIR, int itr) {
		System.out.println("interpreting " + intentIR + " from " + from);
		Set<IC3Intent> intents = new HashSet<IC3Intent>();
		Set<ATTRIBUTE> attrSet = intentIR.dependsOnIncomingIntent();
//		if (!attrSet.isEmpty()) {
//			return intents;
//		}
		INode resNode = null;
		if (!attrSet.isEmpty()) {

			if (from.getUnitStr().contains("setResult(")) {
				for (INode pre : from.getPreds()) {
					if (pre.getUnitStr().contains("setResult(")) {
						resNode = pre;
					}
				}
			}
			for (INode pre : from.getPreds()) {
				if (pre.getUnitStr().contains("setResult(") && from.getPreds().size() > 1) {
					continue;
				}
				this.pre = pre;
				HashSet<IC3Intent> set = new HashSet<>(pre.getInterpretedIntents());
				if (set.isEmpty())
					set.add(new IC3Intent());
				for (IC3Intent inIntent : set) {
					this.currentIncomingIntent = inIntent;
					Set<IC3Intent> currIntents = new HashSet<IC3Intent>();
					currIntents.add(new IC3Intent());
					intenrpretIntents(from, intentIR, itr, currIntents);
					if (resNode != null && from.getPreds().size() > 1) {
						boolean isFound = false;
						for (INode resPre : resNode.getPreds()) {
							for (IC3Intent resPreIntent : resPre.getInterpretedIntents()) {
								if (resPreIntent.equals(this.currentIncomingIntent)) {
									isFound = true;
									for (IC3Intent tn : currIntents) {
										IC3Intent tempIn = tn.clone();
										if (resPre.getComponent() != null)
											tempIn.setRES(resPre.getComponent().getName());
										else
											tempIn.setRES(".*");
										intents.add(tempIn);
									}

								}
								if (isFound)
									break;
							}
							if (isFound) {
								break;
							}
						}

						if (!isFound)
							intents.addAll(currIntents);
					} else if (resNode != null) {
						boolean isFound = false;
						for (INode resPre : resNode.getPreds()) {
							for (IC3Intent tn : currIntents) {
								IC3Intent tempIn = tn.clone();
								isFound = true;
								if (resPre.getComponent() != null)
									tempIn.setRES(resPre.getComponent().getName());
								else
									tempIn.setRES(".*");
								intents.add(tempIn);
							}
						}
						if (!isFound) {
							intents.addAll(currIntents);
						}

					} else {
						intents.addAll(currIntents);
					}
				}
			}

		} else {
			this.pre = null;
			intents.add(new IC3Intent());
			intenrpretIntents(from, intentIR, itr, intents);
		}
		return intents;

	}

	private void intenrpretIntents(INode from, IntentIR intentIR, int itr, Set<IC3Intent> intents) {
		for (ATTRIBUTE attr : intentIR.iattrsMap.keySet()) {
			if (attr == ATTRIBUTE.CATEGOR)
				handleCategory(intentIR, intents, itr, from);
			else {
				Set<Variable> val = intentIR.iattrsMap.get(attr);
				for (Variable v : val) {
					Set<String> set = interpret(v, itr, from);

					if (set.isEmpty())
						continue;
					for (IC3Intent i3i : new ArrayList<>(intents)) {
						intents.remove(i3i);
						for (String str : set) {
							if (str == null || str.equals(Constants.NULL))
								str = null;
							if (attr.equals(ATTRIBUTE.ACTION)) {
								IC3Intent newi3i = i3i.clone();
								newi3i.setAction(str);
								intents.add(newi3i);
							}
							if (attr.equals(ATTRIBUTE.TYPE)) {
								IC3Intent newi3i = i3i.clone();
								newi3i.setType(str);
								intents.add(newi3i);
							}
							if (attr.equals(ATTRIBUTE.DATA)) {
								IC3Intent newi3i = i3i.clone();
								newi3i.setData(str);
								intents.add(newi3i);
							}
							if (attr.equals(ATTRIBUTE.COMPONENT)) {
								IC3Intent newi3i = i3i.clone();
								newi3i.setComponent(str);
								intents.add(newi3i);
							}
							if (attr.equals(ATTRIBUTE.PACKAGE)) {
								IC3Intent newi3i = i3i.clone();
								newi3i.setComponentPackage(str);
								intents.add(newi3i);
							}
							if (attr.equals(ATTRIBUTE.RES)) {
								IC3Intent newi3i = i3i.clone();
								newi3i.setRES(str);
								intents.add(newi3i);
							}
						}
					}
				}
			}

		}

		for (IC3Intent i3i : new ArrayList<>(intents)) {
			List<IC3Intent> worklist = new ArrayList<IC3Intent>();
			worklist.add(i3i);
			intents.remove(i3i);
			for (Variable vKey : intentIR.extraMap.keySet()) {
				Set<IC3Intent> temp = new HashSet<IC3Intent>();
				while (!worklist.isEmpty()) {
					IC3Intent w = worklist.remove(0);
					Set<String> sKey = interpret(vKey, itr, from);
					Set<String> sVal = new HashSet<String>();
					if (comesFromIncomingIntent(sKey, intentIR.extraMap.get(vKey))) {
						Expression expr = (Expression) intentIR.extraMap.get(vKey).iterator().next();
						Set<Map<String, String>> extras = getPossibleExtrasFromIntents(ATTRIBUTE.EXTRA,
								expr.getMethod(), expr.getUnitOffset(), from);
						for (Map<String, String> map : extras) {
							IC3Intent newi3i = w.clone();
							for (Entry<String, String> en : map.entrySet()) {
								newi3i.addExtras(en.getKey(), en.getValue());
							}
							temp.add(newi3i);
						}

					} else {
						for (Variable vVar : intentIR.extraMap.get(vKey)) {
							sVal.addAll(interpret(vVar, itr, from));
						}
						for (String sk : sKey) {
							for (String sv : sVal) {
								IC3Intent newi3i = w.clone();
								newi3i.addExtras(sk, sv);
								temp.add(newi3i);
							}
						}
					}
				}

				worklist.addAll(temp);
			}
			intents.addAll(worklist);
		}

	}

	public static boolean comesFromIncomingIntent(Set<String> sKey, Set<Variable> set) {
		if (sKey.size() == 1 && sKey.contains(Constants.ALL_EXTRA_KEY)) {
			if (set.size() == 1)
				if (IRAnalysis.isIntentRelatedOp(set.iterator().next()) == ATTRIBUTE.EXTRA) {
					return true;
				}
		}
		return false;
	}

	private void handleCategory(IntentIR intentIR, Set<IC3Intent> intents, int itr, INode from) {
		for (IC3Intent i3i : new ArrayList<>(intents)) {
			List<IC3Intent> worklist = new ArrayList<IC3Intent>();
			worklist.add(i3i);
			intents.remove(i3i);
			Set<Variable> val = intentIR.iattrsMap.get(ATTRIBUTE.CATEGOR);
			if (val != null) {
				for (Variable v : val) {
					Set<IC3Intent> temp = new HashSet<IC3Intent>();
					while (!worklist.isEmpty()) {
						IC3Intent w = worklist.remove(0);
						Set<String> set = interpret(v, itr, from);
						if (hasNoCategoryIncomingIntent(set, v)) {
							IC3Intent newi3i = w.clone();
							temp.add(newi3i);
							continue;
						}
						for (String s : set) {
							if (s.equals(Constants.NULL))
								s = "";
							IC3Intent newi3i = w.clone();
							for (String ss : s.split(Constants.RANDOM_SPLITTER)) {
								newi3i.addCategory(ss);
							}
							temp.add(newi3i);
						}
					}
					worklist.addAll(temp);
				}
			}
			intents.addAll(worklist);
		}

	}

	private boolean hasNoCategoryIncomingIntent(Set<String> set, Variable v) {
		if (set.size() == 1 && set.contains(Constants.NULL)) {
			if (IRAnalysis.isIntentRelatedOp(v) == ATTRIBUTE.CATEGOR) {
				return true;
			}
		}
		if (set.size() == 0) {
			if (IRAnalysis.isIntentRelatedOp(v) == ATTRIBUTE.CATEGOR) {
				return true;
			}
		}
		return false;
	}

	private Set<String> interpret(Variable ir, int itr, INode from) {
//		if (ir instanceof NullVariable) {
//			Set<String> s = new LinkedHashSet<>();
//			s.add("!!Null!!");
//			return s;
//		}
		if (!Constants.IS_BUNDLE_ANALYSIS && IRAnalysis.dependsOnIntentOp(ir)) {
			Set<String> s = new HashSet<>();
			s.add("(.*)");
			return s;
		}

		if (ir instanceof ConstantInt) {

			Set<String> s = new LinkedHashSet<>();
			// s.add(ir.getValue());

			try {
				String temp = "" + Integer.parseInt(ir.getValue());
				if (temp.length() > 0)
					s.add(temp);
			} catch (NumberFormatException e) {
				// e.printStackTrace();
				s.add(ir.getValue());
			}
			return s;
		}
		if (ir instanceof ConstantString) {
			Set<String> s = new LinkedHashSet<>();
			s.add(ir.getValue());
			return s;
		}

		else if (ir instanceof ExternalPara) {
			Set<String> s = new LinkedHashSet<>();
			ExternalPara ep = ((ExternalPara) ir);
			String externalName = ep.getName();

			int sourceLineNum = ep.getSourceLineNum();
			int bytecodeOffset = ep.getBytecodeOffSet();

			// field
			// FORMAT:Unknown@FIELD@<field_signature>!!!
			if (externalName.matches("(.*)")) {
				s.addAll(interpretField(externalName));
			}
			// para
			// FORMAT:Unknown@PARA@<method_signature>@parameter_index!!!
			else if (externalName.contains("@parameter")) {
				s.add("Unknown@PARA" + externalName + "!!!");
				// s.add("(.*)");
			}
			// method
			// FORMAT:Unknown@METHOD@<invoking_method_signature>@<containing_method_signature>@source_line_number@bytecode_offset!!!
			else if (externalName.contains("<")) {
//				s.add("Unknown@METHOD@" + externalName.substring(externalName.indexOf("<")) + "@"
//						+ ep.getContainingMethod() + "@" + sourceLineNum + "@" + bytecodeOffset + "!!!");
				s.add("(.*)");
			}
			// FORMAT:Unknown@DYNAMIC_VAR@variable_name@<containing_method_signature@source_line_number@bytecode_offset!!!
			else {
				String var_name;
				if (ep.getName().contains("@")) {
					String[] names = ep.getName().split("@");
					if (names.length >= 3)
						var_name = ep.getName().split("@")[2];
					else
						var_name = names[1];
				} else
					var_name = ep.getName();
//				s.add("Unknown@DYNAMIC_VAR@" + var_name + "@" + ep.getContainingMethod() + "@" + sourceLineNum + "@"
//						+ bytecodeOffset + "!!!");
				s.add("(.*)");
			}
			return s;
		}

		else if (ir instanceof Init) {
			Set<String> s = new LinkedHashSet<>();
			if (itr != 0)
				return s;
			else {
				for (Variable v : ((Init) ir).getInitVar())
					s.addAll(interpret(v, itr, from));
				// ((Init) ir).setInitVar(null);
				return s;
			}

		} else if (ir instanceof Expression) {
			// intent expression
			ATTRIBUTE attr = IRAnalysis.isIntentRelatedOp(ir);
			if (attr != null) {
				if (Constants.IS_BUNDLE_ANALYSIS)
					return intentAttributeInterpreter(attr, (Expression) ir, itr, from);
				Set<String> s = new HashSet<>();
				s.add("(.*)");
				return s;
			}
			// string expressions
			if (Constants.IS_COMPLEX_STR) {
				switch (((Expression) ir).getOperation().getName()) {
				case "append":
					return plusInterpreter((Expression) ir, itr, from);
				case "replaceAll":
					return replaceAllInterpreter((Expression) ir, itr, from);
				case "replaceFirst":
					return replaceFirstInterpreter((Expression) ir, itr, from);
				case "replace(java.lang.CharSequence,java.lang.CharSequence)":
					return replaceCharSequenceInterpreter((Expression) ir, itr, from);
				case "replace(char,char)":
					return replaceCharInterpreter((Expression) ir, itr, from);
				case "toUpperCase":
					return toUpperInterpreter((Expression) ir, itr, from);
				case "toLowerCase":
					return toLowerInterpreter((Expression) ir, itr, from);
				case "trim":
					return trimInterpreter((Expression) ir, itr, from);
				case "CastChar":
					return castInterpreter((Expression) ir, itr, from);
				case "contains":
					return containsInterpreter((Expression) ir, itr, from);
				case "substring(int)":
					return substringInterpreter((Expression) ir, itr, from);
				case "substring(int,int)":
					return substringInterpreter((Expression) ir, itr, from);
				case "charAt":
					return charAtInterpreter((Expression) ir, itr, from);
				case "toCharArray":
					return toCharArrayInterpreter((Expression) ir, itr, from);
				case "encode":
					return encodeInterpreter((Expression) ir, itr, from);
				case "split(java.lang.String)":
					return splitInterpreter((Expression) ir, itr, from);
				case "put":
					return putInterpreter((Expression) ir, itr, from);
				default:
					return new LinkedHashSet<String>();
				}
			} else {
				Set<String> res = new LinkedHashSet<String>();
				res.add(".*");
				switch (((Expression) ir).getOperation().getName()) {
				case "append":
					return plusInterpreter((Expression) ir, itr, from);
				case "replaceAll":
					return res;
				case "replaceFirst":
					return res;
				case "replace(java.lang.CharSequence,java.lang.CharSequence)":
					return res;
				case "replace(char,char)":
					return res;
				case "toUpperCase":
					return res;
				case "toLowerCase":
					return res;
				case "trim":
					return res;
				case "CastChar":
					return res;
				case "contains":
					return res;
				case "substring(int)":
					return res;
				case "substring(int,int)":
					return res;
				case "charAt":
					return res;
				case "toCharArray":
					return res;
				case "encode":
					return res;
				case "split(java.lang.String)":
					return res;
				case "put":
					return res;
				default:
					return new LinkedHashSet<String>();
				}
			}

		}

		else if (ir instanceof T) {

			if (!((T) ir).isFi()) {
				// find immediate T or Fi parent

				for (int i = 0; i < maxLoop; i++)
					parseTT((T) ir, i, from);

				Set<String> s = new LinkedHashSet<>();
				for (Set<String> tmp : tList.get(ir)) {

					s.addAll(tmp);
				}
				return s;

			} else {
				// find immediate T or Fi parent
				int temp = -1;
				Variable parent = ir.getParent();
				while (parent != null) {
					if (parent instanceof T) {
						temp = itr - ((T) parent).getK() + ((T) ir).getK();
						break;
					} else
						parent = parent.getParent();
				}
				// System.out.println(temp);
				// int temp = itr+((T)ir).getK();//System.out.println("itr"+ itr);
				if (temp < 0) {
					// System.out.println("oh no:"+ir);
					return new LinkedHashSet<>();
					/*
					 * Set<String> value = new LinkedHashSet<>(); Variable e = ir.getParent(); if(e
					 * instanceof Expression) { List<Variable> target = new ArrayList<>();
					 * for(List<Variable> opList: ((Expression) e).getOperands()) {
					 * if(opList.contains(ir)) {
					 * 
					 * target.addAll(opList); target.remove(ir);
					 * 
					 * for(Variable init: target) { // !!!might be a trouble here, make extra
					 * interpret if(init instanceof Init) value.addAll(interpret(init,0)); } break;
					 * } }
					 * 
					 * } return value;
					 */
				} else {

					parseTT((T) ir, temp, from);
					// System.out.println(temp+" "+tList.get(ir).get(temp));
					// System.out.println(temp+" "+tList.get(ir).get(temp-1));
					return tList.get(ir).get(temp);
				}

			}
		} else if (ir instanceof InternalVar) {
			return ((InternalVar) ir).getInitValue();
		} else
			return null;
	}

	public Set<String> intentAttributeInterpreter(ATTRIBUTE attr, Expression expr, int itr, INode from) {
		String exprSm = expr.getMethod();
		int exprOffset = expr.getUnitOffset();

		Set<String> res = new HashSet<>();
		if (attr.equals(ATTRIBUTE.EXTRA)) {
			List<Variable> keyvar = expr.getOperands().get(1);
			for (Variable kv : keyvar) {
				Set<String> kss = interpret(kv, itr, from);
				for (String ks : kss) {
					res.addAll(getPossibleVariableFromIntents(attr, exprSm, exprOffset, ks, from));
				}
			}
		} else {
			Set<String> vars = getPossibleVariableFromIntents(attr, exprSm, exprOffset, null, from);
			res.addAll(vars);
		}
		return res;
	}

	public Set<String> getPossibleVariableFromIntents(ATTRIBUTE attr, String nodeMethodSig, int nodeUnitOffset,
			String keyvar, INode from) {
		Set<String> res = new HashSet<>();
		res.add(getRevelantAttrValFromIC3Intents(attr, keyvar));

		return res;
	}

	private String getRevelantAttrValFromIC3Intents(ATTRIBUTE attr, String key) {

		if (attr.equals(ATTRIBUTE.EXTRA)) {

			if (currentIncomingIntent.getExtras().containsKey(key))
				return currentIncomingIntent.getExtras().get(key);

		} else {
			if (ATTRIBUTE.ACTION == attr)
				if (currentIncomingIntent.getAction() != null)
					return currentIncomingIntent.getAction();
				else
					return Constants.NULL;
			if (ATTRIBUTE.COMPONENT == attr)
				if (currentIncomingIntent.getComponent() != null)
					return currentIncomingIntent.getComponent();
				else
					return Constants.NULL;
			if (ATTRIBUTE.TYPE == attr)
				if (currentIncomingIntent.getType() != null)
					return currentIncomingIntent.getType();
				else
					return Constants.NULL;
			if (ATTRIBUTE.DATA == attr)
				if (currentIncomingIntent.getData() != null)
					return currentIncomingIntent.getData();
				else
					return Constants.NULL;
			if (ATTRIBUTE.PACKAGE == attr)
				if (currentIncomingIntent.getComponentPackage() != null)
					return currentIncomingIntent.getComponentPackage();
				else
					return Constants.NULL;
			if (ATTRIBUTE.CATEGOR == attr)
				if (currentIncomingIntent.getCategories() != null) {
					String res = "";
					for (String s : currentIncomingIntent.getCategories()) {
						res += s + Constants.RANDOM_SPLITTER;
					}
					return res;
				} else
					return Constants.NULL;
//			if (ATTRIBUTE.RES == attr) {
//				if (pre.getUnitStr().contains("setResult(")) {
//					if (currentIncomingIntent.getRes() != null) {
//						return currentIncomingIntent.getRes();
//					}
//				}
//			}

		}

		return null;
	}

	public Set<Map<String, String>> getPossibleExtrasFromIntents(ATTRIBUTE attr, String nodeMethodSig,
			int nodeUnitOffset, INode from) {
		Set<Map<String, String>> res = new HashSet<>();

		res.add(this.currentIncomingIntent.getExtras());

		return res;
	}

	private Set<String> interpretField(String fieldName) {
		Set<String> s = new LinkedHashSet<>();
//		s.add("Unknown@FIELD@" + fieldName + "!!!");
		s.add("(.*)");
		/*
		 * if(fieldMap==null) s.add("Unknown@FIELD@"+fieldName + "!!!"); else {
		 * if(fieldMap.get(fieldName)!=null) { for(String temp:fieldMap.get(fieldName))
		 * { if(temp.matches("(.*)")) s.add("Unknown@FIELD@"+fieldName + "!!!"); else
		 * s.add(temp); } //s.addAll(fieldMap.get(((ExternalPara) ir).getName())); }
		 * else s.add("Unknown@FIELD@"+fieldName + "!!!"); //s.add(fieldName); }
		 */
		return s;
	}

	private void parseTT(T t, int itr, INode from) {

		if (!tList.containsKey(t)) {
			tList.put(t, new ArrayList<Set<String>>());
		} else {
			// avoid making extra parseTT due to the initial variable problem
			if (tList.get(t).size() >= itr + 1) {
				if (t.isFi())
					return;
				else
					tList.get(t).clear();
			}
			// return;
		}

		Variable ir = t.getVariable();
		String name = t.getTVarName();
		int regionNum = t.getRegionNumber();
		String line = t.getLine();
		if (itr == 0) {

			tList.get(t).add(interpret(ir, itr, from));

		} else {

			setInitOfInternal(name, regionNum, line, ir, itr, t, from);
			// System.out.println(tList.get(t).get(itr-dif));

			tList.get(t).add(interpret(ir, itr, from));
			// if(t.getTVarName().equals("r2"))
			// System.out.println("ar1:"+t.getLine()+" "+tList.get(t));
		}
	}

	private Set<String> splitInterpreter(Expression ir, int itr, INode from) {
		// TODO operation for index should be added

		Set<String> s = new LinkedHashSet<>();
		List<Variable> op1 = ir.getOperands().get(0);
		List<Variable> op2 = ir.getOperands().get(1);
		List<Variable> ind = new ArrayList<>();
		if (ir.getOperands().size() > 2)
			ind = ir.getOperands().get(2);
		List<Integer> is = new ArrayList<Integer>();
		for (Variable indVar : ind) {
			if (indVar instanceof ConstantString) {
				try {
					int tmp = Integer.parseInt(((ConstantString) indVar).getValue());
					is.add(tmp);
				} catch (NumberFormatException e) {
					is.add(-1);
				}
			}
		}
		for (Variable v1 : op1) {
			Set<String> s1 = interpret(v1, itr, from);
			if (s1.isEmpty())
				continue;
			for (Variable v2 : op2) {
				Set<String> s2 = interpret(v2, itr, from);
				for (String ss1 : s1) {
					if (ss1 == null)
						break;
					for (String ss2 : s2) {
						if (ss2 == null)
							break;
						String[] temp = ss1.split(ss2);
						for (int i : is) {
							if (i < temp.length && i >= 0)
								s.add(temp[i]);
							else
								s.add(temp[0]);
						}

					}
				}

			}
		}
		return s;
	}

	// We use ""

	private Set<String> toCharArrayInterpreter(Expression ir, int itr, INode from) {
		Set<String> s = new LinkedHashSet<>();
		List<Variable> op1 = ir.getOperands().get(0);
		for (Variable v1 : op1) {
			Set<String> s1 = interpret(v1, itr, from);
			if (s1.isEmpty())
				continue;
			for (String value1 : s1) {
				for (int i = 0; i < value1.length(); i++)
					s.add(value1.charAt(i) + "");
			}
		}
		return s;
	}

	private Set<String> charAtInterpreter(Expression ir, int itr, INode from) {
		Set<String> s = new LinkedHashSet<>();

		List<Variable> op1 = ir.getOperands().get(0);
		List<Variable> op2 = ir.getOperands().get(1);
		for (Variable v1 : op1) {
			Set<String> s1 = interpret(v1, itr, from);
			if (s1.isEmpty())
				continue;
			for (Variable v2 : op2) {
				Set<String> s2 = interpret(v2, itr, from);
				if (s2.isEmpty())
					continue;
				for (String value1 : s1)
					for (String value2 : s2) {

						if (value1.equals("null"))
							continue;
						value1 = "" + (int) value1.charAt(0);

						// Can only work in JSA benchmark

						int temp = value2.charAt(0);
						// System.out.println(temp);
						// System.out.println(value1+" charatatat "+temp);
						try {
							s.add("" + value1.charAt(temp));
						} catch (IndexOutOfBoundsException e) {

						}
					}

			}

		}

		return s;
	}

	private Set<String> substringInterpreter(Expression ir, int itr, INode from) {
		Set<String> s = new LinkedHashSet<>();

		List<Variable> op1 = ir.getOperands().get(0);
		List<Variable> op2 = ir.getOperands().get(1);
		List<Variable> op3 = null;
		if (ir.getOperands().size() > 2) {
			op3 = ir.getOperands().get(2);
		}
		for (Variable v1 : op1) {
			Set<String> s1 = interpret(v1, itr, from);
			if (s1.isEmpty())
				continue;
			for (Variable v2 : op2) {
				Set<String> s2 = interpret(v2, itr, from);
				if (s2.isEmpty())
					continue;
				for (String value1 : s1)
					for (String value2 : s2) {
						if (value1.equals("null"))
							continue;
						if (value1.equals("(.*)")) {
							s.add("(.*)");
							continue;
						}
						try {
							int tmp = Integer.parseInt(value2);
							if (op3 == null) {
								s.add(value1.substring(tmp));
							} else {
								for (Variable v3 : op3) {
									Set<String> s3 = interpret(v3, itr, from);
									if (s3.isEmpty())
										s.add(value1.substring(tmp));
									else {
										for (String value3 : s3) {
											int tmp2 = Integer.parseInt(value3);
											s.add(value1.substring(tmp, tmp2));
										}
									}

								}
							}
						} catch (NumberFormatException e) {
							s.add(value1);
						}

					}

			}

		}

		return s;
	}

	private Set<String> plusInterpreter(Expression ir, int itr, INode from) {
		Set<String> s = new LinkedHashSet<>();

		List<Variable> op1 = ir.getOperands().get(0);
		List<Variable> op2 = ir.getOperands().get(1);
		Set<String> s1 = new LinkedHashSet<>();
		Set<String> s2 = new LinkedHashSet<>();
		for (Variable v1 : op1)
			s1.addAll(interpret(v1, itr, from));
		for (Variable v2 : op2)
			s2.addAll(interpret(v2, itr, from));
		if (s1.isEmpty() || s2.isEmpty())
			return s;
		else {
			for (String value1 : s1) {
				for (String value2 : s2) {
					s.add(value1 + value2);
				}
			}
			return s;

		}
		/*
		 * for(Variable v1: op1) { Set<String> s1 = interpret(v1,itr); if(s1.isEmpty())
		 * continue; for(Variable v2: op2) { Set<String> s2 = interpret(v2,itr);
		 * if(s2.isEmpty()) continue; for(String value1:s1) for(String value2: s2) {
		 * //System.out.println("PPP:"+value1+value2);
		 * 
		 * //String temp1 = value1; //String temp2 = value2; try { // temp1 =
		 * ""+(char)Integer.parseInt(temp1);
		 * 
		 * 
		 * } catch(NumberFormatException e) { // e.printStackTrace(); } try {
		 * 
		 * // temp2 = ""+(char)Integer.parseInt(temp2);
		 * 
		 * } catch(NumberFormatException e) { // e.printStackTrace(); }
		 * //s.add(temp1+temp2);
		 * 
		 * 
		 * 
		 * s.add(value1+value2); //value1 = null; //value2 = null; }
		 * 
		 * }
		 * 
		 * 
		 * }
		 * 
		 * return s;
		 */
	}

	private Set<String> putInterpreter(Expression ir, int itr, INode from) {
		Set<String> s = new LinkedHashSet<>();

		List<Variable> op1 = ir.getOperands().get(0);
		List<Variable> op2 = ir.getOperands().get(1);
		Set<String> s1 = new LinkedHashSet<>();
		Set<String> s2 = new LinkedHashSet<>();
		for (Variable v1 : op1)
			s1.addAll(interpret(v1, itr, from));
		for (Variable v2 : op2)
			s2.addAll(interpret(v2, itr, from));
		if (s1.isEmpty() || s2.isEmpty())
			return s;
		else {
			for (String value1 : s1) {
				for (String value2 : s2) {
					s.add(value1 + InterRe.CONTENT_VALUE_OPERATOR + value2);
				}
			}
			return s;

		}
		/*
		 * for(Variable v1: op1) { Set<String> s1 = interpret(v1,itr); if(s1.isEmpty())
		 * continue; for(Variable v2: op2) { Set<String> s2 = interpret(v2,itr);
		 * if(s2.isEmpty()) continue; for(String value1:s1) for(String value2: s2) {
		 * //System.out.println("PPP:"+value1+value2);
		 * 
		 * //String temp1 = value1; //String temp2 = value2; try { // temp1 =
		 * ""+(char)Integer.parseInt(temp1);
		 * 
		 * 
		 * } catch(NumberFormatException e) { // e.printStackTrace(); } try {
		 * 
		 * // temp2 = ""+(char)Integer.parseInt(temp2);
		 * 
		 * } catch(NumberFormatException e) { // e.printStackTrace(); }
		 * //s.add(temp1+temp2);
		 * 
		 * 
		 * 
		 * s.add(value1+value2); //value1 = null; //value2 = null; }
		 * 
		 * }
		 * 
		 * 
		 * }
		 * 
		 * return s;
		 */
	}

	private Set<String> containsInterpreter(Expression ir, int itr, INode from) {
		Set<String> s = new LinkedHashSet<>();

		List<Variable> op1 = ir.getOperands().get(0);
		List<Variable> op2 = ir.getOperands().get(1);
		for (Variable v1 : op1) {
			Set<String> s1 = interpret(v1, itr, from);
			if (s1.isEmpty())
				continue;
			for (Variable v2 : op2) {
				Set<String> s2 = interpret(v2, itr, from);
				if (s2.isEmpty())
					continue;
				for (String value1 : s1)
					for (String value2 : s2) {
						boolean temp = value1.contains(value2);
						if (temp)
							s.add("true");
						else
							s.add("false");
						// s.add(value1+value2);
					}

			}

		}

		return s;
	}

	private Set<String> trimInterpreter(Expression ir, int itr, INode from) {
		Set<String> s = new LinkedHashSet<>();
		List<Variable> op1 = ir.getOperands().get(0);
		for (Variable v1 : op1) {
			Set<String> s1 = interpret(v1, itr, from);
			if (s1.isEmpty())
				continue;
			for (String value1 : s1)
				s.add(value1.trim());
		}
		return s;
	}

	private Set<String> encodeInterpreter(Expression ir, int itr, INode from) {
		Set<String> s = new LinkedHashSet<>();
		List<Variable> op1 = ir.getOperands().get(0);
		for (Variable v1 : op1) {
			Set<String> s1 = interpret(v1, itr, from);
			if (s1.isEmpty())
				continue;
			for (String value1 : s1)
				s.add("###@@@<java.net.URLEncoder: java.lang.String encode(java.lang.String,java.lang.String)>" + value1
						+ "@@@###");
		}
		return s;
	}

	private Set<String> castInterpreter(Expression ir, int itr, INode from) {
		Set<String> s = new LinkedHashSet<>();
		List<Variable> op1 = ir.getOperands().get(0);
		for (Variable v1 : op1) {
			Set<String> s1 = interpret(v1, itr, from);
			if (s1.isEmpty())
				continue;
			for (String value1 : s1) {
				try {
					int tmp = Integer.parseInt(value1);
					s.add("" + (char) tmp);
				} catch (NumberFormatException e) {
					// e.printStackTrace();
					s.add(value1);
				}

			}
		}
		return s;
	}

	private Set<String> toLowerInterpreter(Expression ir, int itr, INode from) {
		Set<String> s = new LinkedHashSet<>();
		List<Variable> op1 = ir.getOperands().get(0);
		for (Variable v1 : op1) {
			Set<String> s1 = interpret(v1, itr, from);
			if (s1.isEmpty())
				continue;
			for (String value1 : s1)
				s.add(value1.toLowerCase());
		}
		return s;
	}

	private Set<String> toUpperInterpreter(Expression ir, int itr, INode from) {
		Set<String> s = new LinkedHashSet<>();
		List<Variable> op1 = ir.getOperands().get(0);
		for (Variable v1 : op1) {
			Set<String> s1 = interpret(v1, itr, from);
			if (s1.isEmpty())
				continue;
			for (String value1 : s1)
				s.add(value1.toUpperCase());
		}
		return s;
	}

	private Set<String> replaceCharInterpreter(Expression ir, int itr, INode from) {
		Set<String> s = new LinkedHashSet<>();

		List<Variable> op1 = ir.getOperands().get(0);
		List<Variable> op2 = ir.getOperands().get(1);
		List<Variable> op3 = ir.getOperands().get(2);
		for (Variable v1 : op1) {
			Set<String> s1 = interpret(v1, itr, from);
			if (s1.isEmpty())
				continue;
			for (Variable v2 : op2) {
				Set<String> s2 = interpret(v2, itr, from);
				if (s2.isEmpty())
					continue;
				for (Variable v3 : op3) {
					Set<String> s3 = interpret(v3, itr, from);
					if (s3.isEmpty())
						continue;

					for (String value1 : s1)
						for (String value2 : s2)
							for (String value3 : s3) {
								// System.out.println("PPP:"+value1+value2);

								// Cast char to String
								/*
								 * String temp2 = null; String temp3 = null; try { temp2 =
								 * ""+(char)Integer.parseInt(value2); temp3 = ""+(char)Integer.parseInt(value3);
								 * } catch(NumberFormatException e) { e.printStackTrace(); }
								 * if(temp2!=null&&temp3!=null) s.add(value1.replace(temp2,temp3));
								 */
								if ((".*").equals(value1))
									s.add(value1);
								s.add(value1.replace(value2, value3));
							}
				}

			}

		}

		return s;
	}

	private Set<String> replaceCharSequenceInterpreter(Expression ir, int itr, INode from) {
		Set<String> s = new LinkedHashSet<>();

		List<Variable> op1 = ir.getOperands().get(0);
		List<Variable> op2 = ir.getOperands().get(1);
		List<Variable> op3 = ir.getOperands().get(2);
		for (Variable v1 : op1) {
			Set<String> s1 = interpret(v1, itr, from);
			if (s1.isEmpty())
				continue;
			for (Variable v2 : op2) {
				Set<String> s2 = interpret(v2, itr, from);
				if (s2.isEmpty())
					continue;
				for (Variable v3 : op3) {
					Set<String> s3 = interpret(v3, itr, from);
					if (s3.isEmpty())
						continue;

					for (String value1 : s1)
						for (String value2 : s2)
							for (String value3 : s3) {
								// System.out.println("PPP:"+value1+value2);
								if ((".*").equals(value1))
									s.add(value1);
								s.add(value1.replace(value2, value3));
							}
				}

			}

		}

		return s;
	}

	private Set<String> replaceFirstInterpreter(Expression ir, int itr, INode from) {
		Set<String> s = new LinkedHashSet<>();

		List<Variable> op1 = ir.getOperands().get(0);
		List<Variable> op2 = ir.getOperands().get(1);
		List<Variable> op3 = ir.getOperands().get(2);
		for (Variable v1 : op1) {
			Set<String> s1 = interpret(v1, itr, from);
			if (s1.isEmpty())
				continue;
			for (Variable v2 : op2) {
				Set<String> s2 = interpret(v2, itr, from);
				if (s2.isEmpty())
					continue;
				for (Variable v3 : op3) {
					Set<String> s3 = interpret(v3, itr, from);
					if (s3.isEmpty())
						continue;

					for (String value1 : s1)
						for (String value2 : s2)
							for (String value3 : s3) {
								// System.out.println("PPP:"+value1+value2);
								if ((".*").equals(value1))
									s.add(value1);
								s.add(value1.replaceFirst(value2, value3));
							}
				}

			}

		}

		return s;
	}

	private Set<String> replaceAllInterpreter(Expression ir, int itr, INode from) {
		Set<String> s = new LinkedHashSet<>();

		List<Variable> op1 = ir.getOperands().get(0);
		List<Variable> op2 = ir.getOperands().get(1);
		List<Variable> op3 = ir.getOperands().get(2);
		for (Variable v1 : op1) {
			Set<String> s1 = interpret(v1, itr, from);
			if (s1.isEmpty())
				continue;
			for (Variable v2 : op2) {
				Set<String> s2 = interpret(v2, itr, from);
				if (s2.isEmpty())
					continue;
				for (Variable v3 : op3) {
					Set<String> s3 = interpret(v3, itr, from);
					if (s3.isEmpty())
						continue;

					for (String value1 : s1)
						for (String value2 : s2)
							for (String value3 : s3) {
								// System.out.println("PPP:"+value1+" "+value2+" "+value3);
								try {
									if ((".*").equals(value1))
										s.add(value1);
									s.add(value1.replaceAll(value2, value3));
								} catch (PatternSyntaxException e) {

								}

							}
				}

			}

		}

		return s;
	}

	private void setInitOfInternal(String name, int regionNum, String line, Variable v, int itr, T t, INode from) {
		if (v instanceof T) {
			// if(((T) v).isFi())
			// {
			if (!(((T) v).getTVarName().equals(name) && ((T) v).getRegionNumber() == regionNum
					&& ((T) v).getLine().equals(line))) {
				if (itr - t.getK() + ((T) v).getK() > 0) {

					setInitOfInternal(name, regionNum, line, ((T) v).getVariable(), itr, t, from);
				}
			}
			// }

		} else if (v instanceof Expression) {
			for (List<Variable> operandList : ((Expression) v).getOperands()) {
				for (Variable operand : operandList) {
					setInitOfInternal(name, regionNum, line, operand, itr, t, from);
				}
			}
		} else if (v instanceof InternalVar) {
			if (((InternalVar) v).getName().equals(name) && ((InternalVar) v).getRegionNum() == regionNum
					&& ((InternalVar) v).getLine().equals(line)) {
				if ((itr - (((InternalVar) v).getK()) < 0)) {
					// System.out.println("itr - the k of internal < 0"+" "+v);
					Variable e = v.getParent();
					if (e instanceof Expression) {
						List<Variable> target = new ArrayList<>();
						for (List<Variable> opList : ((Expression) e).getOperands()) {
							if (opList.contains(v)) {

								target.addAll(opList);
								target.remove(v);// System.out.println("got you"+target);
								Set<String> value = new LinkedHashSet<>();
								for (Variable init : target) {
									// !!!might be a trouble here, make extra interpret
									if (init instanceof Init)
										value.addAll(interpret(init, 0, from));
								}
								((InternalVar) v).setInitValue(value);
							}
						}
					}
				} else {
					// if(((InternalVar) v).getName().equals("r2"))
					// System.out.println(v+" "+tList.get(t).get(itr-(((InternalVar) v).getK())));
					if (itr - (((InternalVar) v).getK()) < tList.get(t).size())
						((InternalVar) v).setInitValue(tList.get(t).get(itr - (((InternalVar) v).getK())));

				}
			}
		}
	}

	public Set<String> getValueForIR() {
		Set<String> s = new LinkedHashSet<>();
		for (Variable ir : target) {
			// System.out.println(ir);
			s.addAll(ir.getInterpretedValue());
		}
		return s;
	}

//	private void setValueToIR() {
//
//		for (Variable ir : target) {
//			Set<String> s = new LinkedHashSet<>();
//			s.addAll(interpret(ir, maxLoop, from));
//			if (s.isEmpty())
//				s.add("Unknown@INTERPRET@" + ir + "!!!");
//			s.remove(InterRe.ARRAY_MARKER);
//			s.remove(InterRe.CONTENT_VALUE_MARKER);
//			ir.setInterpretedValue(s);
//		}
//	}

}
