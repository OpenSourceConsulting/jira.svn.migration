/**
 * 
 */
package kr.osc.jira.svn.rest.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import kr.osc.jira.svn.common.model.GridJsonResponse;
import kr.osc.jira.svn.rest.models.Commit;
import kr.osc.jira.svn.rest.models.Issue;
import kr.osc.jira.svn.rest.models.Project;
import kr.osc.jira.svn.rest.models.SVNElement;
import kr.osc.jira.svn.rest.repositories.CommitRepository;
import kr.osc.jira.svn.rest.repositories.SubversionRepository;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.tmatesoft.svn.core.SVNException;
import org.zeroturnaround.zip.commons.IOUtils;

/**
 * @author Bongjin Kwon
 * @author Tran Ho
 *
 */
@Controller
public class JiraSVNMigrationController implements InitializingBean {
	@Value("${jira.ip}")
	private String jiraIPAddr;
	@Value("${jira.port}")
	private int jiraPort;
	@Value("${jira.username}")
	private String username;
	@Value("${jira.password}")
	private String password;
	@Value("${server.os}")
	private String serverOs;

	private HttpHost jiraHost;
	private CredentialsProvider credsProvider;
	private AuthCache authCache;
	@Autowired
	private CommitRepository commitRepo;
	@Autowired
	private SubversionRepository subversionRepo;

	@Override
	public void afterPropertiesSet() throws Exception {
		jiraHost = new HttpHost(jiraIPAddr, jiraPort, "http");
		credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(jiraHost.getHostName(), jiraHost.getPort()), new UsernamePasswordCredentials(username, password));
		authCache = new BasicAuthCache();
		authCache.put(jiraHost, new BasicScheme());
	}

	@RequestMapping("/api/projects")
	@ResponseBody
	public GridJsonResponse getProjectList(GridJsonResponse json) throws Exception {
		HttpUriRequest httpget = RequestBuilder.get().setUri(new URI(jiraHost.toURI() + "/rest/api/2/project")).build();
		String response = callAPI(httpget);

		JSONArray array = new JSONArray(response);
		List<Project> projects = new ArrayList();
		for (int i = 0; i < array.length(); i++) {
			JSONObject obj = array.getJSONObject(i);
			projects.add(new Project(obj.getString("id"), obj.getString("key"), obj.getString("name"), obj.getString("self")));
		}
		json.setList(projects);
		json.setTotal(projects.size());
		return json;
	}

	@RequestMapping("/api/search")
	@ResponseBody
	public String search(@RequestParam("jql") String jql) throws Exception {
		HttpUriRequest httpget = RequestBuilder.get().setUri(new URI(jiraHost.toURI() + "/rest/api/2/search")).addParameter("jql", jql).build();
		return callAPI(httpget);
	}

	@RequestMapping("/api/issues/filter")
	@ResponseBody
	public GridJsonResponse search(GridJsonResponse json, String projectId, String fromDate, String toDate, String fields) throws Exception {
		String jql = "project=" + projectId;
		if (StringUtils.isNotEmpty(fromDate)) {
			jql += " AND created>=\"" + fromDate + "\"";
		}
		if (StringUtils.isNotEmpty(toDate)) {
			jql += " AND created<=\"" + toDate + "\"";
		}

		jql = URLEncoder.encode(jql, "UTF-8");
		System.out.println("jql:" + jql);
		HttpUriRequest httpget = RequestBuilder.get().setUri(new URI(jiraHost.toURI() + "/rest/api/2/search?jql=" + jql + "&fields=" + fields)).build();
		String response = callAPI(httpget);
		JSONObject object = new JSONObject(response);
		JSONArray array = object.getJSONArray("issues");
		List<Issue> issues = new ArrayList();
		for (int i = 0; i < array.length(); i++) {
			JSONObject obj = array.getJSONObject(i);
			int id = obj.getInt("id");
			String key = obj.getString("key");
			String url = obj.getString("self");
			String summary = "";
			String created = "";
			String updated = "";
			String status = "";
			try {
				JSONObject f = obj.getJSONObject("fields");
				summary = f.getString("summary");
				created = f.getString("created").substring(0, "yyyy-MM-dd".length());
				updated = f.getString("updated").substring(0, "yyyy-MM-dd".length());
				status = f.getJSONObject("status").getString("name");
			} catch (Exception e) {

			}
			List<Commit> commits = commitRepo.search("key", key);
			issues.add(new Issue(id, key, url, summary, created, updated, status, commits));
		}
		json.setList(issues);
		json.setTotal(issues.size());
		return json;
	}

	@RequestMapping("/api/commits")
	@ResponseBody
	public List<Commit> getCommits(String field, String value) throws IOException {
		return commitRepo.search(field, value);
	}

	@RequestMapping("/api/svn/export")
	@ResponseBody
	public void export(@RequestParam(value = "issueKeys[]") String[] issueKeys, HttpServletResponse response) throws IOException, SVNException {
		List<Commit> commits = commitRepo.search("key", issueKeys);
		if (commits.size() == 0) {
			subversionRepo.export(null, null, true);
		} else {
			int latestRevision = -1;
			for (Commit c : commits) {
				if (c.getRevision() > latestRevision) {
					latestRevision = c.getRevision();
				}
			}
			String zipFile = subversionRepo.export(null, String.valueOf(latestRevision), true);
			if (StringUtils.isNotEmpty(zipFile)) {
				String subDirSeparator = "/";
				if (serverOs.equals("windows")) {
					subDirSeparator = "\\";
				}
				response.setContentType("application/zip");
				String fileName = zipFile.substring(zipFile.lastIndexOf(subDirSeparator) + 1);
				response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
				InputStream is = new FileInputStream(new File(zipFile));
				IOUtils.copy(is, response.getOutputStream());
				response.flushBuffer();
			}
		}
	}

	@RequestMapping("/api/svn/tree")
	@ResponseBody
	public SVNElement getSVNTree() throws SVNException {
		return subversionRepo.getSVNTree();

	}

	private String callAPI(HttpUriRequest request) throws Exception {
		// Add AuthCache to the execution context
		final HttpClientContext context = HttpClientContext.create();
		context.setCredentialsProvider(credsProvider);
		context.setAuthCache(authCache);

		HttpClient httpclient = HttpClientBuilder.create().build();

		System.out.println("Executing request " + request.getRequestLine());
		HttpResponse response = httpclient.execute(request, context);
		System.out.println("----------------------------------------");
		System.out.println(response.getStatusLine());

		String resJson = EntityUtils.toString(response.getEntity());
		System.out.println(resJson);

		return resJson;
	}

}
