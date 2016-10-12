/**
 * 
 */
package kr.osc.jira.svn.rest.controllers;

import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import kr.osc.jira.svn.common.model.GridJsonResponse;
import kr.osc.jira.svn.models.Issue;
import kr.osc.jira.svn.models.Project;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Bongjin Kwon
 * @author Tran Ho
 *
 */
@Controller
public class JiraRestProxyController implements InitializingBean {
	@Value("${jira.ip}")
	private String jiraIPAddr;
	@Value("${jira.port}")
	private int jiraPort;
	@Value("${jira.username}")
	private String username;
	@Value("${jira.password}")
	private String password;

	private HttpHost jiraHost;
	private CredentialsProvider credsProvider;
	private AuthCache authCache;

	@Override
	public void afterPropertiesSet() throws Exception {
		jiraHost = new HttpHost(jiraIPAddr, jiraPort, "http");
		credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(jiraHost.getHostName(), jiraHost.getPort()), new UsernamePasswordCredentials(username, password));
		authCache = new BasicAuthCache();
		authCache.put(jiraHost, new BasicScheme());
	}

	//private HttpHost jiraHost = new HttpHost("hhivaas_app01", 2990, "http");

	//	@RequestMapping("/jira/rest/api/2/issue")
	//	@ResponseBody
	//	public String create(@RequestParam("pkey") String pkey, @RequestParam("summary") String summary, @RequestParam("desc") String desc,
	//			@RequestParam("itype") String itype) throws Exception {
	//
	//		Issue issue = new Issue(pkey, summary, desc, itype);
	//		String json = om.writeValueAsString(issue);
	//
	//		System.out.println(json);
	//
	//		HttpUriRequest httpreq = RequestBuilder.post().setUri(new URI(jiraHost.toURI() + "/jira/rest/api/2/issue"))
	//				.setHeader("Content-Type", "application/json;charset=UTF-8").setEntity(new StringEntity(json, "UTF-8")) // set json request body.
	//				.build();
	//
	//		return callAPI(httpreq);
	//	}
	@RequestMapping("/jira/rest/api/projects")
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

	@RequestMapping("/jira/rest/api/2/search")
	@ResponseBody
	public String search(@RequestParam("jql") String jql) throws Exception {
		HttpUriRequest httpget = RequestBuilder.get().setUri(new URI(jiraHost.toURI() + "/rest/api/2/search")).addParameter("jql", jql).build();
		return callAPI(httpget);
	}

	@RequestMapping("/jira/rest/api/issues/filter")
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
			JSONObject f = obj.getJSONObject("fields");
			String summary = f.getString("summary");
			String created = f.getString("created");
			String updated = f.getString("updated");
			String status = f.getJSONObject("status").getString("name");
			issues.add(new Issue(id, key, url, summary, created, updated, status));
		}
		json.setList(issues);
		json.setTotal(issues.size());
		return json;
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
