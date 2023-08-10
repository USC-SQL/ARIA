package usc.edu.sql.fpa.utils;

import afu.org.checkerframework.checker.units.qual.C;
import soot.SootClass;
import soot.SootMethod;
import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import usc.edu.sql.fpa.analysis.bundle.ICCFinder;
import usc.edu.sql.fpa.model.CodePoint;
import usc.edu.sql.fpa.utils.Constants.COMPONENT;

public class APIUtils {
	public static COMPONENT checkInvoke(Stmt stmt) {
		SootMethod invokeMethod = stmt.getInvokeExpr().getMethod();
		Hierarchy hier = Hierarchy.getInstance(Constants.APK_PATH);
		if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
			Value base = ((InstanceInvokeExpr) stmt.getInvokeExpr()).getBase();
			SootClass c = hier.scene.getSootClass(base.getType().toString());
			if (c == null)
				return null;
			COMPONENT type = getAPIType(invokeMethod);
			if (hier.isServiceClass(c) || hier.isActivityClass(c) || hier.isContextClass(c)
					|| hier.isBroadcastReceiverClass(c)) {
				if (!type.equals(COMPONENT.OTHER)) {

					return type;

				}
			}
			if(type.equals(COMPONENT.INTENT))
				return type;

		}
		return null;

	}

	public static COMPONENT getAPIType(SootMethod invokeMethod) {

		if (MethodConstants.Activity_Senders.contains(invokeMethod.getSubSignature()))
			return COMPONENT.ACTIVITY;
		if (MethodConstants.Activity_Recievers_APIs.contains(invokeMethod.getSubSignature()))
			return COMPONENT.ACTIVITY_RECEIVE_API;
		if (MethodConstants.Activity_Recievers_Callbacks.contains(invokeMethod.getSubSignature()))
			return COMPONENT.ACTIVITY_RECEIVE_CALLBACK;
		if (MethodConstants.Service_Senders.contains(invokeMethod.getSubSignature()))
			return COMPONENT.SERVICE;
		if (MethodConstants.Service_Recievers.contains(invokeMethod.getSubSignature()))
			return COMPONENT.SERVICE_RECEIVE_CALLBACK;
		if (MethodConstants.BroadcastReceiver_Senders.contains(invokeMethod.getSubSignature()))
			return COMPONENT.BROADCAST_RECEIVER;
		if (MethodConstants.BroadCastReceiver_Recievers.contains(invokeMethod.getSubSignature()))
			return COMPONENT.BROADCAST_RECEIVER_CALLBACK;
		if (MethodConstants.intentAPIs.contains(invokeMethod.getSubSignature()) && invokeMethod.getName().startsWith("get"))
			return COMPONENT.INTENT;
		if (MethodConstants.intentAPIs.contains(invokeMethod.getSubSignature()) && invokeMethod.getSignature().contains("Parcelable"))
			return COMPONENT.PARCELABLE;
		return COMPONENT.OTHER;
	}
	
	public static COMPONENT getAPIType(String invokeMethod) {

		if (MethodConstants.Activity_Senders.contains(invokeMethod))
			return COMPONENT.ACTIVITY;
		if (MethodConstants.Activity_Recievers_APIs.contains(invokeMethod))
			return COMPONENT.ACTIVITY_RECEIVE_API;
		if (MethodConstants.Activity_Recievers_Callbacks.contains(invokeMethod))
			return COMPONENT.ACTIVITY_RECEIVE_CALLBACK;
		if (MethodConstants.Service_Senders.contains(invokeMethod))
			return COMPONENT.SERVICE;
		if (MethodConstants.Service_Recievers.contains(invokeMethod))
			return COMPONENT.SERVICE_RECEIVE_CALLBACK;
		if (MethodConstants.BroadcastReceiver_Senders.contains(invokeMethod))
			return COMPONENT.BROADCAST_RECEIVER;
		if (MethodConstants.BroadCastReceiver_Recievers.contains(invokeMethod))
			return COMPONENT.BROADCAST_RECEIVER_CALLBACK;
		if (MethodConstants.intentAPIs.contains(invokeMethod) && invokeMethod.startsWith("get"))
			return COMPONENT.INTENT;
		return COMPONENT.OTHER;
	}

}
