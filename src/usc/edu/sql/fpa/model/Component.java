package usc.edu.sql.fpa.model;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

import usc.edu.sql.fpa.utils.Constants.COMPONENT;

public class Component implements Serializable {
    String name = "";
	String app;
    Set<IntentFilter> intentFilters = new LinkedHashSet<IntentFilter>();
	private COMPONENT type;
    
    public Component(String name, String app, COMPONENT type) {
    	this.name = name;
    	this.type = type;
    	this.app = app;
		
	}
    private boolean isExported = false;
	private boolean isMainActivity = false;

    public void addIntentFilter(IntentFilter intentFilter) {
        intentFilters.add(intentFilter);
    }

    public Set<IntentFilter> getIntentFilters() {
        return intentFilters;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getName() {
        return this.name;
    }
    
    public COMPONENT getType() {
        return this.type;
    }

    public String toString() {
        return name;
    }
	public boolean isMainActivity() {
		return isMainActivity;
	}
	
    public void setIsMain(boolean isMain) {
        this.isMainActivity = isMain;
    }

    public boolean isExported() {
        return isExported;
    }

    public void setExported(boolean isExported) {
        this.isExported = isExported;
    }

	public int getIntentFiltersCount() {
		
		return intentFilters.size();
	}
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((app == null) ? 0 : app.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Component other = (Component) obj;
		if (app == null) {
			if (other.app != null)
				return false;
		} else if (!app.equals(other.app))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type != other.type)
			return false;
		return true;
	}


}
