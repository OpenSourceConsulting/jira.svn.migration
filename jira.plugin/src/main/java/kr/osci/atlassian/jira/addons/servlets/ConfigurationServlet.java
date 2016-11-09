package kr.osci.atlassian.jira.addons.servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.templaterenderer.TemplateRenderer;

import kr.osci.atlassian.jira.addons.models.Configuration;
import kr.osci.atlassian.jira.addons.services.ConfigurationService;

@Named
public class ConfigurationServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(ConfigurationServlet.class);
	@Inject
	private ConfigurationService confService;
	@Inject
	@ComponentImport
	private TemplateRenderer renderer;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		String svnDataDir = confService.getSVNDataDir();

		Map<String, Object> context = new HashMap<String, Object>();
		context.put("currentDir", svnDataDir);
		renderer.render("ui/main.vm", context, res.getWriter());
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		String path = req.getParameter("newDir");
		//there is only one configuration with Id = 1
		String newPath = confService.setSVNDataDir(1, path);
		Map<String, Object> context = new HashMap<String, Object>();
		context.put("result", 1);
		context.put("currentDir", newPath);
		renderer.render("ui/main.vm", context, res.getWriter());
	}
}
