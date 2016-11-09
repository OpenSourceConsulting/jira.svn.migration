package kr.osci.atlassian.jira.addons.models;

import net.java.ao.Entity;
import net.java.ao.Preload;

@Preload
public interface Configuration extends Entity {
	String getConfId();

	void setConfId(Integer id);

	String getSVNPluginDir();

	void setSVNPluginDir(String path);
}
