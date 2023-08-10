package usc.edu.sql.fpa.model;

import soot.SootMethod;
import usc.edu.sql.fpa.utils.APINameUtil;
import usc.edu.sql.fpa.utils.Constants.COMPONENT;

public class ICCResponsibleComponent {
	COMPONENT callerType;
	SootMethod entryPoint;
	Component component;

	public ICCResponsibleComponent(SootMethod entryPoint, COMPONENT callerType, Component comp) {
		this.entryPoint = entryPoint;
		this.callerType = callerType;
		this.component = comp;
	}

	public static COMPONENT getScenarioType(SootMethod entry) {
		if (APINameUtil.isViewAPI(entry)) {
			return COMPONENT.VIEW;
		}
		if (APINameUtil.isActivityCallback(entry)) {
			return COMPONENT.ACTIVITY;
		}
		if (APINameUtil.isServiceCallback(entry)) {
			return COMPONENT.SERVICE;
		}
		if (APINameUtil.isFragementCallback(entry)) {
			return COMPONENT.FRAGEMENT;
		}
		if (APINameUtil.isBroadCastCallback(entry)) {
			return COMPONENT.BROADCAST_RECEIVER;
		}
		if (APINameUtil.isContentProviderCallback(entry)) {
			return COMPONENT.CONTENTPROVIDER;
		}
		if (APINameUtil.isApplicationCallback(entry)) {
			return COMPONENT.APPLICATION;
		}
//		if (APINameUtil.isActivityLifecycleCallbacks(entry)) {
//			return COMPONENT.ACTIVITY_LIFECYCLE_CALLBACKS;
//		}
		if (APINameUtil.isEventListener(entry)) {
			if (APINameUtil.isBasicListener(entry)) {
				return COMPONENT.BASIC_LISTENER;
			}
			return COMPONENT.OTHER_LISTENER;
		}
		return COMPONENT.OTHER;
	}

	public Component getComponent() {
		return component;
	}
	
	public COMPONENT getType() {
		return callerType;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return callerType.toString() + ": " + entryPoint.toString();
	}
}
