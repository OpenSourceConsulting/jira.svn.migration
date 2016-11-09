package kr.osci.atlassian.jira.addons.services;

import javax.inject.Inject;
import javax.inject.Named;

import net.java.ao.Query;
import kr.osci.atlassian.jira.addons.models.Configuration;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

@Named
public class ConfigurationService {
	@ComponentImport
	@Inject
	private ActiveObjects ao;

	public String getSVNDataDir() {
		Configuration[] confs = ao.find(Configuration.class, Query.select().where("CONF_ID = ?", 1)); //only 1 configuration is stored.
		if (confs == null || confs.length == 0) {
			return "/var/atlassian/application-data/jira/caches/indexes/plugins/atlassian-subversion-revisions"; //default dir
		}
		return confs[0].getSVNPluginDir();
	}

	public String setSVNDataDir(Integer id, String newPath) {
		Configuration[] confs = ao.find(Configuration.class, Query.select().where("CONF_ID = ?", id));
		Configuration conf = null;
		if (confs == null || confs.length == 0) {
			conf = ao.create(Configuration.class);
			conf.setConfId(1);//only 1 config
		} else {
			conf = confs[0];
		}
		conf.setSVNPluginDir(newPath);
		conf.save();
		ao.flushAll();
		return conf.getSVNPluginDir();
	}
}
