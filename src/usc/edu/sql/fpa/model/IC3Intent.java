package usc.edu.sql.fpa.model;

import java.io.Serializable;
import java.util.*;

import android.content.ComponentName;
import soot.jimple.infoflow.android.iccta.Ic3Data.Attribute;
import soot.jimple.infoflow.android.iccta.Ic3Data.Application.Component.IntentFilter;
import usc.edu.sql.fpa.analysis.bundle.INode;
import usc.edu.sql.fpa.utils.Constants;

public class IC3Intent implements Serializable {

	private String component;
	private String componentPackage;
	private String componentClass;
	private String res;
	private String action;
	private Set<String> categories = new HashSet<String>();
	private Map<String, String> extras = new HashMap<String, String>();
	private String dataScheme;
	private String dataHost;
	private int dataPort = -1;
	private String dataPath;
	private String data;
	private int flags;
	private String authority;
	private String dataType;
	private Object app;

	public IC3Intent() {
	}

	public boolean isImplicit() {
		if (component != null && !component.isEmpty() && !component.contains("*")
				&& !component.contains("NULL-CONSTANT") && !component.contains(Constants.NULL))
			return false;

		return true;
	}

	@Override
	public IC3Intent clone() {
		IC3Intent intent = new IC3Intent();
		intent.component = component;
		intent.componentPackage = componentPackage;
		intent.componentClass = componentClass;
		intent.res = res;
		intent.action = action;
		Set<String> tmpCategories = new HashSet<String>();
		for (String str : categories) {
			tmpCategories.add(str);
		}
		intent.categories = tmpCategories;
		Map<String, String> tmpExtras = new HashMap<String, String>();
		for (Map.Entry<String, String> entry : extras.entrySet()) {
			tmpExtras.put(entry.getKey(), entry.getValue());
		}
		intent.extras = tmpExtras;
		intent.dataType = dataType;
		intent.dataScheme = dataScheme;
		intent.dataHost = dataHost;
		intent.dataPort = dataPort;
		intent.dataPath = dataPath;
		intent.data = data;
		intent.flags = flags;
		intent.app = app;

		return intent;
	}

	@Override
	public String toString() {
		return "Intent [component=" + component + ", componentPackage=" + componentPackage + ", componentClass="
				+ componentClass + ", action=" + action + ", categories=" + categories + ", extras=" + extras
				+ ", dataScheme=" + dataScheme + ", dataHost=" + dataHost + ", dataPort=" + dataPort + ", dataPath="
				+ dataPath + ", data=" + data + ", dataType=" + dataType + ", res=" + res + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;

		return this.toString().equals(o.toString());
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	public String getComponent() {
		return component;
	}

	public void setComponent(String component) {
		if (component == null) {
			this.component = null;
			return;
		}
		component = component.replace(";", "");
		this.component = component;
		if (component.contains("/") && !component.startsWith("/")) {
			setComponentPackage(component.split("/")[0]);
			if (!component.endsWith("/"))
				setComponentClass(component.split("/")[1]);
		}
	}

	public String getComponentPackage() {
		return componentPackage;
	}

	public void setComponentPackage(String componentPackage) {
		this.componentPackage = componentPackage;
	}

	public String getComponentClass() {
		return componentClass;
	}

	public void setComponentClass(String componentClass) {
		this.componentClass = componentClass;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		if (Constants.NULL.equals(action))
			action = null;
		else
			this.action = action;
	}

	public Set<String> getCategories() {
		return categories;
	}

	public void setCategories(Set<String> categories) {
		if (!categories.isEmpty())
			this.categories = categories;
	}

	public void addCategory(String category) {
		if (categories.contains(Constants.NULL)) {
			categories.remove(Constants.NULL);
		}
		this.categories.add(category);
	}

	public Map<String, String> getExtras() {
		return extras;
	}

	public void setExtras(Map<String, String> extras) {
		this.extras = extras;
	}

	public void addExtras(String key, String val) {
		extras.put(key, val);
	}

	public String getDataScheme() {
		return dataScheme;
	}

	public void setDataScheme(String dataScheme) {
//		if (dataScheme.equals("(.*)"))
//			return;
		this.dataScheme = dataScheme;
	}

	public String getDataHost() {
		return dataHost;
	}

	public void setDataHost(String dataHost) {
		if (dataHost.equals("(.*)"))
			return;
		this.dataHost = dataHost;
	}

	public int getDataPort() {
		return dataPort;
	}

	public void setDataPort(int dataPort) {
		if (dataPort == 0)
			dataPort = -1;
		this.dataPort = dataPort;
	}

	public String getDataPath() {
		return dataPath;
	}

	public void setDataPath(String dataPath) {
		if (dataPath.equals("(.*)"))
			return;
		this.dataPath = dataPath;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
		if (data == null)
			return;
		if (data.contains("://")) {
			if (dataScheme == null) {
				dataScheme = data.substring(0, data.indexOf("://"));
				data = data.substring(data.indexOf("://") + 3);
				if (dataScheme.contains(".*"))
					dataScheme = null;
			}
			if (!data.isEmpty()) {
				if (dataHost == null) {
					if (data.contains("/"))
						dataHost = data.substring(0, data.indexOf("/"));
					else
						dataHost = data;
					if (dataHost.contains(".*"))
						dataHost = null;
				}
				if (dataPath == null && !data.isEmpty()) {
					if (data.contains("/"))
						dataPath = data.substring(data.indexOf("/") + 1);
					else
						dataPath = data;
					if (dataPath.contains(".*"))
						dataPath = null;
				}
			}
		}
	}

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public void setAuthority(String value) {
		authority = value;
	}

	public String getAuthority() {
		return authority;
	}

	public boolean hasImpreciseValues() {
		String string = toString();

		return isImprecise(string);
	}

	private boolean isImprecise(String string) {
		if (string == null)
			return false;

		// This should not occur in intents, except due to effects of Harvester.
		if (string.toLowerCase().contains("harvester"))
			return true;

		if (string.contains(".*"))
			return true;

		return false;
	}

	public boolean hasImportantImpreciseValues() {

		return isImprecise(getAction()) || isImprecise(authority);
	}

	public Boolean resolve(Component component, String app, INode snode) {
		boolean isImplicit = isImplicit();
		String packageName = getComponentPackage();
		String componentName = getComponent();

		if (componentName != null && componentName.contains("/")) {
			if (packageName == null)
				packageName = componentName.substring(0, componentName.indexOf("/"));
			componentName = componentName.substring(componentName.indexOf("/") + 1);
		}
//		if (packageName != null && packageName.contains(".*"))
//			packageName = null;
//		if (componentName != null && componentName.contains(".*"))
//			componentName = null;
		if (componentName != null && componentName.contains("NULL-CONSTANT"))
			componentName = null;

		if (component == null) {
			return false;
		}

		if (componentName != null) {
			if (componentName.equals(component.getName()) || regexMatch(componentName, component.getName())) {
				return true;
			}
		}

		boolean exported;
		/*
		 * The default value depends on whether the activity contains intent filters.
		 * The absence of any filters means that the activity can be invoked only by
		 * specifying its exact class name. This implies that the activity is intended
		 * only for application-internal use (since others would not know the class
		 * name). So in this case, the default value is "false". On the other hand, the
		 * presence of at least one filter implies that the activity is intended for
		 * external use, so the default value is "true".
		 */

		if (component.getIntentFiltersCount() > 0)
			exported = true;
		else
			exported = false;

		if (component.isExported())
			exported = true;

		if (!exported) {
			// Whether or not the activity can be launched by components of other
			// applications - "true" if it can be, and "false" if not. If "false", the
			// activity can be launched only by components of the same application or
			// applications with the same user ID.
			if (!app.startsWith(component.app) && regexMatch(app, component.app)) {
				return false;
			}

		}
		if (packageName != null) {
			// (Usually optional) Set an explicit application package name that limits the
			// components this Intent will resolve to. If left to the default value of null,
			// all components in all applications will considered. If non-null, the Intent
			// can only match the components in the given application package.
//				if (!component.getApp().getAppName().equals(packageName))
//					continue;
			if (!regexMatch(app, packageName))
				return false;
		}
		if (isImplicit) {
			// Android automatically applies the the CATEGORY_DEFAULT category to all
			// implicit intents passed to startActivity() and startActivityForResult(). So
			// if you want your activity to receive implicit intents, it must include a
			// category for "android.intent.category.DEFAULT" in its intent filters (as
			// shown in the previous <intent-filter> example.
			// However, it might be due to imprecision reasonse we assume it's implicit
			// although it's not...
			// Therefore we assume that this intent might be explicit nevertheless...

			if (snode.getInvokedExpr().contains("startActivit"))
				categories.add("android.intent.category.DEFAULT");

			for (usc.edu.sql.fpa.model.IntentFilter filter : component.intentFilters) {
				boolean hasSpecifiedAnAction = false;
				boolean passedAction = false;
				boolean passedCategory = false;
				boolean passedData = false;
				// String scheme = null, host = null, path = null, authority = null;

				// See the <data> tag in the SDK help
				/*
				 * All the <data> elements contained within the same <intent-filter> element
				 * contribute to the same filter. So, for example, the following filter
				 * specification, <intent-filter . . . > <data android:scheme="something"
				 * android:host="project.example.com" /> . . . </intent-filter>
				 * 
				 * is equivalent to this one: <intent-filter . . . > <data
				 * android:scheme="something" /> <data android:host="project.example.com" /> . .
				 * . </intent-filter>
				 */
				// Comment by me:
				// Thus, the attributes seem to be independent of each other and I can save each
				// one
				// in a list, as the nesting of data tags do not matter:
				List<String> schemes = new ArrayList<String>();
				List<String> hosts = new ArrayList<String>();
				List<String> paths = new ArrayList<String>();
				List<Integer> ports = new ArrayList<Integer>();
				List<String> authorities = new ArrayList<String>();
				List<String> types = new ArrayList<String>();

				if (getAction() == null)
					passedAction = true;

				boolean categoryVisited = false;

				hasSpecifiedAnAction = true;
				if (filter.actions.contains(getAction()) || regexMatch(new ArrayList<>(filter.actions), getAction()))
					passedAction = true;
				passedCategory = filter.categories.containsAll(getCategories());
				if (!passedCategory) {
					passedCategory = true;
					for (String cat : getCategories()) {
						if (!regexMatch(new ArrayList<>(filter.categories), cat))
							passedCategory = false;
					}
				}
				categoryVisited = true;

				for (String attribute : filter.data.keySet()) {
					if (attribute.toUpperCase().contains("HOST")) {
						hosts.add(filter.data.get(attribute));
						if (dataScheme != null)
							System.out.println();
					}
					if (attribute.toUpperCase().contains("SCHEME")) {
						schemes.add(filter.data.get(attribute));

					}
					if (attribute.toUpperCase().contains("PORT")) {
						ports.add(Integer.parseInt(filter.data.get(attribute)));
					}
					if (attribute.toUpperCase().contains("PATH")) {
						paths.add(filter.data.get(attribute));
					}
					if (attribute.toUpperCase().contains("AUTHORITY")) {
						authorities.add(filter.data.get(attribute));
					}
					if (attribute.toUpperCase().contains("TYPE")) {
						types.add(filter.data.get(attribute));
					}

				}

				if (!categoryVisited) {
					if (getCategories().isEmpty()) {
						passedCategory = true;
					}
				}

				// If both the scheme and host are not specified, the path is ignored.
				if (schemes.isEmpty() && hosts.isEmpty())
					paths.clear();
				// If a scheme is not specified, the host is ignored.
				if (schemes.isEmpty())
					hosts.clear();
				// If a host is not specified, the port is ignored.
				if (hosts.isEmpty())
					ports.clear();

				// When the URI in an intent is compared to a URI specification in a filter,
				// it's compared only to the parts of the URI included in the filter
				boolean matchesURI = false;
				if (!schemes.isEmpty())
					matchesURI |= (schemes.contains(getDataScheme()) || regexMatch(schemes, getDataScheme()));
				if (!authorities.isEmpty())
					matchesURI &= (authorities.contains(getAuthority()) || regexMatch(authorities, getAuthority()));
				if (!paths.isEmpty()) {
					for (String input : paths) {
						String regex = ("\\Q" + input + "\\E").replace("*", "\\E.*\\Q");
						String s = getDataPath();
						if (s == null)
							s = "";
						matchesURI |= s.matches(regex);
					}
				}

				if (!hosts.isEmpty()) {
					System.out.println(hosts.contains(getDataHost()));
					System.out.println(regexMatch(hosts, getDataHost()));
					matchesURI &= (hosts.contains(getDataHost()) || regexMatch(hosts, getDataHost()));
				}
				if (!ports.isEmpty())
					matchesURI &= ports.contains(getDataPort());
				if (!types.isEmpty())
					matchesURI |= (types.contains(getType()) || regexMatch(types, getType()));

				// TODO: When does it "contain a URI"?
				boolean containsURI = getDataScheme() != null;
				boolean intentFilterSpecifiesURI = !schemes.isEmpty();

				// An intent that contains neither a URI nor a MIME type passes the test only if
				// the filter does not specify any URIs or MIME types.
				if (getType() == null && getAuthority() == null && getDataScheme() == null && getDataPort() == -1
						&& getDataPath() == null && getDataHost() == null) {
					passedData = ports.isEmpty() && paths.isEmpty() && hosts.isEmpty() && authorities.isEmpty()
							&& schemes.isEmpty() && types.isEmpty();
				}

				// An intent that contains a URI but no MIME type (neither explicit nor
				// inferable from the URI) passes the test only if its URI matches the filter's
				// URI format and the filter likewise does not specify a MIME type.
				if (containsURI && getType() == null) {
					passedData = matchesURI && types.isEmpty();
				}

				// An intent that contains a MIME type but not a URI passes the test only if the
				// filter lists the same MIME type and does not specify a URI format.
				if (getType() != null && !containsURI) {
					if (getType() == null)
						passedData = !intentFilterSpecifiesURI;
					else
						passedData = (types.contains(getType()) || regexMatch(types, getType()))
								&& !intentFilterSpecifiesURI;
				}

				/*
				 * An intent that contains both a URI and a MIME type (either explicit or
				 * inferable from the URI) passes the MIME type part of the test only if that
				 * type matches a type listed in the filter. It passes the URI part of the test
				 * either if its URI matches a URI in the filter or if it has a content: or
				 * file: URI and the filter does not specify a URI. In other words, a component
				 * is presumed to support content: and file: data if its filter lists only a
				 * MIME type.
				 */
				if (getType() != null && containsURI) {
					boolean mimetype = types.contains(getType()) || regexMatch(types, getType());
					boolean urlpart = matchesURI;
					if (getDataScheme() != null)
						urlpart = urlpart || ((getDataScheme().equals("content") || getDataScheme().equals("file"))
								&& !intentFilterSpecifiesURI);

					passedData = mimetype && urlpart;
				}
				boolean passedActionPart = passedAction || !hasSpecifiedAnAction;

				// Imprecision in IC3: Assume, we have passed the data test...
				/*
				 * if (getData() == null && getDataHost() == null && getDataScheme() == null &&
				 * getDataPath() == null) passedData = true;
				 */
				if (passedActionPart && passedCategory && passedData) {
					// This intent filter succeeded
					return true;
				}
			}

		}

		return false;
	}

	public List<Component> resolve(List<Component> list, String app, INode snode) {
		boolean isImplicit = isImplicit();
		String packageName = getComponentPackage();
		String componentName = getComponent();

		if (componentName != null && componentName.contains("/")) {
			if (packageName == null)
				packageName = componentName.substring(0, componentName.indexOf("/"));
			componentName = componentName.substring(componentName.indexOf("/") + 1);
		}
//		if (packageName != null && packageName.contains(".*"))
//			packageName = null;
//		if (componentName != null && componentName.contains(".*"))
//			componentName = null;
		if (componentName != null && componentName.contains("NULL-CONSTANT"))
			componentName = null;
		List<Component> results = new ArrayList<Component>();
		for (Component component : list) {
			if (component == null) {
				continue;
			}
			if (packageName != null) {
				// (Usually optional) Set an explicit application package name that limits the
				// components this Intent will resolve to. If left to the default value of null,
				// all components in all applications will considered. If non-null, the Intent
				// can only match the components in the given application package.
//				if (!component.getApp().getAppName().equals(packageName))
//					continue;
				if (!regexMatch(app, packageName))
					continue;
			}

			if (componentName != null) {
				if (componentName.equals(component.getName()) || regexMatch(componentName, component.getName())) {
					results.add(component);
					continue;
				}
			}

			boolean exported;
			/*
			 * The default value depends on whether the activity contains intent filters.
			 * The absence of any filters means that the activity can be invoked only by
			 * specifying its exact class name. This implies that the activity is intended
			 * only for application-internal use (since others would not know the class
			 * name). So in this case, the default value is "false". On the other hand, the
			 * presence of at least one filter implies that the activity is intended for
			 * external use, so the default value is "true".
			 */

			if (component.getIntentFiltersCount() > 0)
				exported = true;
			else
				exported = false;

			if (component.isExported())
				exported = true;

			if (!exported) {
				// Whether or not the activity can be launched by components of other
				// applications - "true" if it can be, and "false" if not. If "false", the
				// activity can be launched only by components of the same application or
				// applications with the same user ID.
				if (!app.startsWith(component.app) && regexMatch(app, component.app)) {
					continue;
				}

			}
			if (isImplicit) {
				// Android automatically applies the the CATEGORY_DEFAULT category to all
				// implicit intents passed to startActivity() and startActivityForResult(). So
				// if you want your activity to receive implicit intents, it must include a
				// category for "android.intent.category.DEFAULT" in its intent filters (as
				// shown in the previous <intent-filter> example.
				// However, it might be due to imprecision reasonse we assume it's implicit
				// although it's not...
				// Therefore we assume that this intent might be explicit nevertheless...

				if (snode.getInvokedExpr().contains("startActivit"))
					categories.add("android.intent.category.DEFAULT");

				for (usc.edu.sql.fpa.model.IntentFilter filter : component.intentFilters) {
					boolean hasSpecifiedAnAction = false;
					boolean passedAction = false;
					boolean passedCategory = false;
					boolean passedData = false;
					// String scheme = null, host = null, path = null, authority = null;

					// See the <data> tag in the SDK help
					/*
					 * All the <data> elements contained within the same <intent-filter> element
					 * contribute to the same filter. So, for example, the following filter
					 * specification, <intent-filter . . . > <data android:scheme="something"
					 * android:host="project.example.com" /> . . . </intent-filter>
					 * 
					 * is equivalent to this one: <intent-filter . . . > <data
					 * android:scheme="something" /> <data android:host="project.example.com" /> . .
					 * . </intent-filter>
					 */
					// Comment by me:
					// Thus, the attributes seem to be independent of each other and I can save each
					// one
					// in a list, as the nesting of data tags do not matter:
					List<String> schemes = new ArrayList<String>();
					List<String> hosts = new ArrayList<String>();
					List<String> paths = new ArrayList<String>();
					List<Integer> ports = new ArrayList<Integer>();
					List<String> authorities = new ArrayList<String>();
					List<String> types = new ArrayList<String>();

					if (getAction() == null)
						passedAction = true;

					boolean categoryVisited = false;

					hasSpecifiedAnAction = true;
					if (filter.actions.contains(getAction())
							|| regexMatch(new ArrayList<>(filter.actions), getAction()))
						passedAction = true;
					passedCategory = filter.categories.containsAll(getCategories());
					if (!passedCategory) {
						passedCategory = true;
						for (String cat : getCategories()) {
							if (!regexMatch(new ArrayList<>(filter.categories), cat))
								passedCategory = false;
						}
					}
					categoryVisited = true;

					for (String attribute : filter.data.keySet()) {
						if (attribute.toUpperCase().contains("HOST")) {
							hosts.add(filter.data.get(attribute));
							if (dataScheme != null)
								System.out.println();
						}
						if (attribute.toUpperCase().contains("SCHEME")) {
							schemes.add(filter.data.get(attribute));

						}
						if (attribute.toUpperCase().contains("PORT")) {
							ports.add(Integer.parseInt(filter.data.get(attribute)));
						}
						if (attribute.toUpperCase().contains("PATH")) {
							paths.add(filter.data.get(attribute));
						}
						if (attribute.toUpperCase().contains("AUTHORITY")) {
							authorities.add(filter.data.get(attribute));
						}
						if (attribute.toUpperCase().contains("TYPE")) {
							types.add(filter.data.get(attribute));
						}

					}

					if (!categoryVisited) {
						if (getCategories().isEmpty()) {
							passedCategory = true;
						}
					}

					// If both the scheme and host are not specified, the path is ignored.
					if (schemes.isEmpty() && hosts.isEmpty())
						paths.clear();
					// If a scheme is not specified, the host is ignored.
					if (schemes.isEmpty())
						hosts.clear();
					// If a host is not specified, the port is ignored.
					if (hosts.isEmpty())
						ports.clear();

					// When the URI in an intent is compared to a URI specification in a filter,
					// it's compared only to the parts of the URI included in the filter
					boolean matchesURI = false;
					if (!schemes.isEmpty())
						matchesURI |= (schemes.contains(getDataScheme()) || regexMatch(schemes, getDataScheme()));
					if (!authorities.isEmpty())
						matchesURI |= (authorities.contains(getAuthority()) || regexMatch(authorities, getAuthority()));
					if (!paths.isEmpty()) {
						for (String input : paths) {
							String regex = ("\\Q" + input + "\\E").replace("*", "\\E.*\\Q");
							String s = getDataPath();
							if (s == null)
								s = "";
							matchesURI |= s.matches(regex);
						}
					}

					if (!hosts.isEmpty()) {
						System.out.println(hosts.contains(getDataHost()));
						System.out.println(regexMatch(hosts, getDataHost()));
						matchesURI |= (hosts.contains(getDataHost()) || regexMatch(hosts, getDataHost()));
					}
					if (!ports.isEmpty())
						matchesURI |= ports.contains(getDataPort());
					if (!types.isEmpty())
						matchesURI |= (types.contains(getType()) || regexMatch(types, getType()));

					// TODO: When does it "contain a URI"?
					boolean containsURI = getDataScheme() != null;
					boolean intentFilterSpecifiesURI = !schemes.isEmpty();

					// An intent that contains neither a URI nor a MIME type passes the test only if
					// the filter does not specify any URIs or MIME types.
					if (getType() == null && getAuthority() == null && getDataScheme() == null && getDataPort() == -1
							&& getDataPath() == null && getDataHost() == null) {
						passedData = ports.isEmpty() && paths.isEmpty() && hosts.isEmpty() && authorities.isEmpty()
								&& schemes.isEmpty() && types.isEmpty();
					}

					// An intent that contains a URI but no MIME type (neither explicit nor
					// inferable from the URI) passes the test only if its URI matches the filter's
					// URI format and the filter likewise does not specify a MIME type.
					if (containsURI && getType() == null) {
						passedData = matchesURI && types.isEmpty();
					}

					// An intent that contains a MIME type but not a URI passes the test only if the
					// filter lists the same MIME type and does not specify a URI format.
					if (getType() != null && !containsURI) {
						if (getType() == null)
							passedData = !intentFilterSpecifiesURI;
						else
							passedData = (types.contains(getType()) || regexMatch(types, getType()))
									&& !intentFilterSpecifiesURI;
					}

					/*
					 * An intent that contains both a URI and a MIME type (either explicit or
					 * inferable from the URI) passes the MIME type part of the test only if that
					 * type matches a type listed in the filter. It passes the URI part of the test
					 * either if its URI matches a URI in the filter or if it has a content: or
					 * file: URI and the filter does not specify a URI. In other words, a component
					 * is presumed to support content: and file: data if its filter lists only a
					 * MIME type.
					 */
					if (getType() != null && containsURI) {
						boolean mimetype = types.contains(getType()) || regexMatch(types, getType());
						boolean urlpart = matchesURI;
						if (getDataScheme() != null)
							urlpart = urlpart || ((getDataScheme().equals("content") || getDataScheme().equals("file"))
									&& !intentFilterSpecifiesURI);

						passedData = mimetype && urlpart;
					}
					boolean passedActionPart = passedAction || !hasSpecifiedAnAction;

					// Imprecision in IC3: Assume, we have passed the data test...
					/*
					 * if (getData() == null && getDataHost() == null && getDataScheme() == null &&
					 * getDataPath() == null) passedData = true;
					 */
					if (passedActionPart && passedCategory && passedData) {
						// This intent filter succeeded
						results.add(component);

						break;
					}
				}

			}

		}
		return results;
	}

	private boolean regexMatch(List<String> schemes, String in) {
		for (String val : schemes) {
			if (regexMatch(val, in)) {
				return true;
			}
		}
		return false;
	}

	private boolean regexMatch(String in1, String in2) {

		if (in1 != null) {
			in1 = in1.replace(".*", " sfjghasbvhayrie92#$#!@ ");
			in1 = in1.replace("*", " .* ");
			in1 = in1.replace(" sfjghasbvhayrie92#$#!@ ", ".*");
		}
		if (in2 != null) {
			in2 = in2.replace(".*", " sfjghasbvhayrie92#$#!@ ");
			in2 = in2.replace("*", " .* ");
			in2 = in2.replace(" sfjghasbvhayrie92#$#!@ ", ".*");
		}

		if (in1 != null && in2 != null && in1.matches(in2))
			return true;
		if (in2 != null && in1 != null && in2.matches(in1))
			return true;
		if (in1 == null && in2 == null)
			return true;
		return false;
	}

	public Map<String, List<Component>> resolve(Map<String, Set<Component>> componentsPerApp, INode snode) {
		Map<String, List<Component>> result = new HashMap<String, List<Component>>();
		for (String app : componentsPerApp.keySet()) {
			List<Component> resPerApp = resolve(new ArrayList<>(componentsPerApp.get(app)), app, snode);
			if (resPerApp.size() > 0) {
				result.put(app, resPerApp);
			}
		}
		return result;
	}

	public void setType(String value) {
		this.dataType = value;
	}

	public String getType() {
		return dataType;
	}

	public void setRES(String str) {
		this.res = str;

	}

	public String getRes() {
		return this.res;
	}

}