package usc.edu.sql.fpa.model;

import org.javatuples.Triplet;

import soot.toolkits.scalar.Pair;
import usc.edu.sql.fpa.analysis.intra.IType;
import usc.edu.sql.fpa.utils.Constants.ATTRIBUTE;

import java.util.*;


public class Intent {

	public boolean isOutgoing = true;
	public CodePoint definitionPoint = null;
	public Map<Pair<String, String>, String> extraMap = new HashMap<>();
	public String action;
	public String targetComponent;
	public String packageName;
	public Set<String> categories = new LinkedHashSet<String>();
	public Set<Integer> flags = new LinkedHashSet<>();
	public String dataType;
	public String uri = null;
	public Set<Triplet<String, String, String>> extras = new LinkedHashSet<>();

	public Set<String> filteredActions = new HashSet<String>();
	public Set<String> filteredCategories = new HashSet<>();
	public Map<String, String> filteredData = new HashMap<>();
	public Boolean isVulnerable = false;
	public CodePoint iccPoint = null;
	public IType itype;
	

	public Intent(Intent intent) {

		this.extraMap.putAll(intent.extraMap);

		this.action = intent.action;

		this.targetComponent = intent.targetComponent;

		this.categories = new LinkedHashSet<>(intent.categories);

		this.iccPoint = intent.iccPoint;
//		this.originMethod = intent.originMethod;
//		this.originComponent = intent.originComponent;
		this.filteredActions = new HashSet<String>(intent.filteredActions);
		this.filteredCategories = new HashSet<String>(intent.filteredCategories);
		this.filteredData = new HashMap<String, String>(intent.filteredData);
	}

	public Intent() {
		super();
	}

	public void setExtras(Set<Triplet<String, String, String>> extraData) {

		for (Triplet<String, String, String> extra : extraData) {
			extraMap.put(new Pair<>(extra.getValue0(), extra.getValue1()), extra.getValue2());
		}

	}

	@Override
	public String toString() {
		String s = "Intent is outgoing: " + isOutgoing + " {" + "extras=" + extraMap + ", action='" + action + '\''
				+ ", targetComponent='" + targetComponent + '\'' + ", categories=" + categories + "type=" + dataType
				+ ", uri=" + uri + "}\n";
//		s += "originMethod" + originMethod + "\n";
//		s += "originComponent" + originComponent + "\n";
		s += "ICCPoint" + iccPoint + "\n";
		if (!isOutgoing) {
			s += "Filters: \n actions: " + filteredActions + "\n categories: " + categories + "\n data: " + filteredData
					+ "\n";
		}
		return s;
	}

	public boolean checkEquality(Intent in) {
		if (actionEquality(in)) {
			if (categoryEquality(in)) {
				if (dataEquality(in)) {
					return extraEquality(in);
				}

			}

		}
		return false;
	}

	private boolean extraEquality(Intent in) {
		// TODO Auto-generated method stub
		return true;
	}

	private boolean dataEquality(Intent in) {
		// TODO Auto-generated method stub
		return true;
	}

	private boolean categoryEquality(Intent in) {
		// TODO Auto-generated method stub
		return true;
	}

	private boolean actionEquality(Intent in) {
		if (in.action == null || action == null)
			return true;
		if (action != null) {
			return action.equals(in.action);
		}
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Intent other = (Intent) obj;
		if (action == null) {
			if (other.action != null)
				return false;
		} else if (!action.equals(other.action))
			return false;
		if (categories == null) {
			if (other.categories != null)
				return false;
		} else if (!categories.equals(other.categories))
			return false;
		if (dataType == null) {
			if (other.dataType != null)
				return false;
		} else if (!dataType.equals(other.dataType))
			return false;
		if (extraMap == null) {
			if (other.extraMap != null)
				return false;
		} else if (!extraMap.equals(other.extraMap))
			return false;
		if (extras == null) {
			if (other.extras != null)
				return false;
		} else if (!extras.equals(other.extras))
			return false;
		if (filteredActions == null) {
			if (other.filteredActions != null)
				return false;
		} else if (!filteredActions.equals(other.filteredActions))
			return false;
		if (filteredCategories == null) {
			if (other.filteredCategories != null)
				return false;
		} else if (!filteredCategories.equals(other.filteredCategories))
			return false;
		if (filteredData == null) {
			if (other.filteredData != null)
				return false;
		} else if (!filteredData.equals(other.filteredData))
			return false;
		if (flags == null) {
			if (other.flags != null)
				return false;
		} else if (!flags.equals(other.flags))
			return false;
		if (isOutgoing != other.isOutgoing)
			return false;
		if (isVulnerable == null) {
			if (other.isVulnerable != null)
				return false;
		} else if (!isVulnerable.equals(other.isVulnerable))
			return false;
//		if (originComponent == null) {
//			if (other.originComponent != null)
//				return false;
//		} else if (!originComponent.equals(other.originComponent))
//			return false;
//		if (originMethod == null) {
//			if (other.originMethod != null)
//				return false;
//		} else if (!originMethod.equals(other.originMethod))
//			return false;
		if (packageName == null) {
			if (other.packageName != null)
				return false;
		} else if (!packageName.equals(other.packageName))
			return false;
		if (targetComponent == null) {
			if (other.targetComponent != null)
				return false;
		} else if (!targetComponent.equals(other.targetComponent))
			return false;
		if (uri == null) {
			return other.uri == null;
		} else
			return uri.equals(other.uri);
	}

//	public Set<ATTRIBUTE> attributesWithReusedInfo() {
//		Set<ATTRIBUTE> res = new HashSet<Constants.ATTRIBUTE>();
//		if (Constants.RECEIVED_INFO.equals(action)) {
//			res.add(ATTRIBUTE.ACTION);
//		}
//		if (categories != null && categories.contains(Constants.RECEIVED_INFO)) {
//			res.add(ATTRIBUTE.CATEGOR);
//		}
//		if (Constants.RECEIVED_INFO.equals(dataType)) {
//			res.add(ATTRIBUTE.TYPE);
//		}
//		if (Constants.RECEIVED_INFO.equals(uri)) {
//			res.add(ATTRIBUTE.DATA);
//		}
//		return res;
//	}

	public void updateAttrFromReceivingExplicitIntent(Intent possibleIntent, ATTRIBUTE attr) {
		if (attr.equals(ATTRIBUTE.ACTION)) {
			this.action = possibleIntent.action;
		}
		if (attr.equals(ATTRIBUTE.CATEGOR)) {
			this.categories.addAll(possibleIntent.categories);
		}

	}

//	public Set<Intent> updateAttrFromReceivingImplicitIntent(Intent intent, IntentFilter filter, ATTRIBUTE attr) {
//		Set<Intent> res = new HashSet<>();
//		if(attr.equals(ATTRIBUTE.ACTION)) {
//			for(String filterAction: filter.actions) {
//				Intent newIntent = new Intent(intent);
//				newIntent.action = filterAction;
//				res.add(newIntent);
//			}
//		}
//		return res;
//	}

	

}
