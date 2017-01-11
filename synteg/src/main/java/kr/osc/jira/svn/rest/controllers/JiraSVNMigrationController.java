/**
 * 
 */
package kr.osc.jira.svn.rest.controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import kr.osc.jira.svn.common.model.GridJsonResponse;
import kr.osc.jira.svn.common.model.SimpleJsonResponse;
import kr.osc.jira.svn.rest.models.Commit;
import kr.osc.jira.svn.rest.models.Issue;
import kr.osc.jira.svn.rest.models.Project;
import kr.osc.jira.svn.rest.models.SVNElement;
import kr.osc.jira.svn.rest.models.Status;
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
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.tmatesoft.svn.core.SVNException;
import org.zeroturnaround.zip.commons.IOUtils;

/**
 * @author Bongjin Kwon
 * @author Tran Ho
 *
 */
@Controller
public class JiraSVNMigrationController implements InitializingBean {
	private static final Logger LOGGER = Logger.getLogger(JiraSVNMigrationController.class);

	@Value("${jira.ip}")
	private String jiraIPAddr;
	@Value("${jira.port}")
	private int jiraPort;
	@Value("${jira.contextpath}")
	private String jiraContextPath;
	@Value("${jira.username}")
	private String username;
	@Value("${jira.password}")
	private String password;
	@Value("${server.os}")
	private String serverOs;
	@Value("${svn.tmp.upload.dir}")
	private String tmpUploadDir;
	@Value("${svn.tmp.delete}")
	private boolean deleteSVNTmp;
	@Value("${svn.export.callback.shell}")
	private String exportCallbackShell;

	private HttpHost jiraHost;
	private CredentialsProvider credsProvider;
	private AuthCache authCache;
	private String jiraRestURL;
	@Autowired
	private SubversionRepository subversionRepo;

	@Override
	public void afterPropertiesSet() throws Exception {
		jiraHost = new HttpHost(jiraIPAddr, jiraPort, "http");
		credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(jiraHost.getHostName(), jiraHost.getPort()), new UsernamePasswordCredentials(username, password));
		authCache = new BasicAuthCache();
		authCache.put(jiraHost, new BasicScheme());
		//preprocess jiraContextPath 
		if (jiraContextPath != null && !jiraContextPath.equals("")) {
			if (!jiraContextPath.startsWith("/")) {
				jiraContextPath = "/" + jiraContextPath;
			}
		}
		// pattern of rest url: http://jira.osci.kr/rest/jira.svn/1.0/commits
		jiraRestURL = jiraHost.toURI() + jiraContextPath + "/rest/jira.svn/1.0/commits";
	}

	@RequestMapping("api/statuses")
	@ResponseBody
	public GridJsonResponse getIssueStatuses(GridJsonResponse json) {
		try {
			HttpUriRequest httpget = RequestBuilder.get().setUri(new URI(jiraHost.toURI() + jiraContextPath + "/rest/api/2/status")).build();
			String response = callAPI(httpget);
			if (!response.equals("")) {
				JSONArray array = new JSONArray(response);
				List<Status> statuses = new ArrayList<Status>();
				for (int i = 0; i < array.length(); i++) {
					JSONObject obj = array.getJSONObject(i);
					statuses.add(new Status(obj.getString("id"), obj.getString("name")));
				}
				json.setList(statuses);
				json.setTotal(statuses.size());
			} else {
				json.setSuccess(false);
			}
		} catch (Exception ex) {
			json.setSuccess(false);
			json.setMsg(ex.getMessage());
			LOGGER.error(ex.getMessage());
		}
		return json;
	}

	@RequestMapping("/api/projects")
	@ResponseBody
	public GridJsonResponse getProjectList(GridJsonResponse json) {
		try {
			HttpUriRequest httpget = RequestBuilder.get().setUri(new URI(jiraHost.toURI() + jiraContextPath + "/rest/api/2/project")).build();
			String response = callAPI(httpget);
			if (!response.equals("")) {
				JSONArray array = new JSONArray(response);
				List<Project> projects = new ArrayList<Project>();
				for (int i = 0; i < array.length(); i++) {
					JSONObject obj = array.getJSONObject(i);
					projects.add(new Project(obj.getString("id"), obj.getString("key"), obj.getString("name"), obj.getString("self")));
				}
				json.setList(projects);
				json.setTotal(projects.size());
			} else {
				json.setSuccess(false);
			}
		} catch (Exception ex) {
			json.setSuccess(false);
			json.setMsg(ex.getMessage());
			LOGGER.error(ex.getMessage());
		}
		return json;

	}

	@RequestMapping("/api/search")
	@ResponseBody
	public String search(@RequestParam("jql") String jql) throws Exception {
		HttpUriRequest httpget = RequestBuilder.get().setUri(new URI(jiraHost.toURI() + jiraContextPath + "/rest/api/2/search")).addParameter("jql", jql)
				.build();
		return callAPI(httpget);
	}

	@RequestMapping("/api/issues/filter")
	@ResponseBody
	public GridJsonResponse search(GridJsonResponse json, String projectId, String fromDate, String toDate,
			@RequestParam(value = "statuses[]", required = false) Integer[] statuses, String fields) {
		String jql = "project=" + projectId;
		if (StringUtils.isNotEmpty(fromDate)) {
			jql += " AND created>=\"" + fromDate + "\"";
		}
		if (StringUtils.isNotEmpty(toDate)) {
			jql += " AND created<=\"" + toDate + "\"";
		}
		if (statuses != null && statuses.length > 0) {
			jql += " AND status in (" + StringUtils.join(statuses, ",") + ")";
		}
		try {
			jql = URLEncoder.encode(jql, "UTF-8");
			HttpUriRequest httpget = RequestBuilder.get()
					.setUri(new URI(jiraHost.toURI() + jiraContextPath + "/rest/api/2/search?jql=" + jql + "&fields=" + fields)).build();
			String response = callAPI(httpget);
			if (!response.equals("")) {
				JSONObject object = new JSONObject(response);
				JSONArray array = object.getJSONArray("issues");
				List<Issue> issues = new ArrayList<Issue>();
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
					String[] keywords = new String[1];
					keywords[0] = key;

					List<Commit> commits = getCommits("key", keywords);

					issues.add(new Issue(id, key, url, summary, created, updated, status, commits));
				}
				json.setList(issues);
				json.setTotal(issues.size());
			}
		} catch (Exception ex) {
			json.setSuccess(false);
			json.setMsg(ex.getMessage());
			LOGGER.error(ex.getMessage());
		}
		return json;
	}

	// @RequestMapping("/api/commits")
	// @ResponseBody
	// public List<Commit> getCommits(String field, String value) throws
	// IOException {
	// return commitRepo.search(field, value);
	// }

	@RequestMapping("/api/svn/export")
	public void export(@RequestParam(value = "issueKeys[]") String[] issueKeys, HttpServletResponse response) {
		try {
			List<Commit> commits = getCommits("key", issueKeys);
			if (commits.size() > 0) {
				List<Integer> revisions = new ArrayList<Integer>();
				for (Commit c : commits) {
					revisions.add(c.getRevision());
				}
				//there are two results returned: zip file path and tmp dir contains exported files.
				String[] result = subversionRepo.export(revisions, true, deleteSVNTmp);
				String zipFile = result[0];
				String tempExportDir = result[1];
				if (StringUtils.isNotEmpty(zipFile)) {
					String subDirSeparator = "/";
					if (serverOs.equals("windows")) {
						subDirSeparator = "\\";
					}
					response.setContentType("application/zip");
					String fileName = zipFile.substring(zipFile.lastIndexOf(subDirSeparator) + 1);
					response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
					File f = new File(zipFile);
					InputStream is = new FileInputStream(f);
					IOUtils.copy(is, response.getOutputStream());
					response.flushBuffer();
					is.close();
					// delete on server
					f.delete();

					//run callback shell script
					if (!exportCallbackShell.equals("")) {
						ProcessBuilder pb = new ProcessBuilder(exportCallbackShell, tempExportDir);
						Process p = pb.start();
						BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
						String line = null;
						while ((line = reader.readLine()) != null) {
							System.out.println("[Export Shell]" + line);
							LOGGER.info("[Export Shell]" + line);
						}
					}
				}
			}
		} catch (Exception ex) {
			LOGGER.error(ex.getMessage());
		}
	}

	@RequestMapping("/api/svn/tree")
	@ResponseBody
	public SimpleJsonResponse getSVNTree(SimpleJsonResponse json) {
		SVNElement tree = null;
		try {
			tree = subversionRepo.getSVNTree();
			json.setData(tree);
		} catch (SVNException ex) {
			json.setSuccess(false);
			json.setMsg(ex.getMessage());
			LOGGER.error(ex.getMessage());
		}
		return json;

	}

	@RequestMapping("/api/svn/tree/load")
	@ResponseBody
	public GridJsonResponse getSVNTree(GridJsonResponse json, String parent) {
		try {
			json.setList(subversionRepo.getSVNChildNodes(parent));
		} catch (Exception ex) {
			json.setSuccess(false);
			json.setMsg(ex.getMessage());
			LOGGER.error(ex.getMessage());
		}
		return json;

	}

	@RequestMapping(value = "/api/svn/checkdiff", method = RequestMethod.POST)
	@ResponseBody
	public SimpleJsonResponse checkDiff(SimpleJsonResponse json, HttpServletRequest req, String selectedPath, boolean isExtract,
			@RequestPart(value = "file", required = false) MultipartFile file, String fileLocation, String serverFilePath) {
		String uploadedFilePath = "";
		boolean isLocalFile = false;
		try {
			if (fileLocation.equals("local")) {
				isLocalFile = true;
				File uploadDir = new File(tmpUploadDir);
				if (!uploadDir.exists()) {
					uploadDir.mkdir();
				}
				uploadedFilePath = tmpUploadDir + file.getOriginalFilename();
				File f = new File(uploadedFilePath);
				Path path = f.toPath();
				if (!Files.exists(path, LinkOption.values())) {
					path = Files.createFile(path);
				}
				InputStream input = file.getInputStream();
				Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING);
			} else {
				isLocalFile = false;
				uploadedFilePath = serverFilePath;
				File f = new File(uploadedFilePath);
				Path path = f.toPath();
				if (!Files.exists(path, LinkOption.values())) {
					json.setSuccess(false);
					json.setMsg("The file does not exist on server.");
					return json;
				}
			}

			String log = subversionRepo.checkDiff(selectedPath, isLocalFile, uploadedFilePath, isExtract);
			// set value for next action
			HttpSession session = req.getSession();
			session.setAttribute("filePath", uploadedFilePath);
			session.setAttribute("isLocalFile", isLocalFile);
			session.setAttribute("selectedSVNPath", selectedPath);
			if (log.equals("")) {
				log = "There is no changes.";
			}
			json.setData(log);
		} catch (Exception ex) {
			json.setSuccess(false);
			json.setMsg(ex.getMessage());
			LOGGER.error(ex.getMessage());
		}
		return json;
	}

	@RequestMapping(value = "/api/svn/import", method = RequestMethod.POST)
	@ResponseBody
	public SimpleJsonResponse importSources(SimpleJsonResponse json, HttpServletRequest req, String message, boolean isMultipleFiles) {
		HttpSession session = req.getSession();
		String filePath = (String) session.getAttribute("filePath");
		String selectPath = (String) session.getAttribute("selectedSVNPath");
		boolean isLocalFile = (boolean) session.getAttribute("isLocalFile");
		session.removeAttribute("filePath");
		session.removeAttribute("isLocalFile");
		session.removeAttribute("selectedSVNPath");
		try {
			subversionRepo.importSourceCodes(filePath, isLocalFile, selectPath, message, isMultipleFiles, true);
		} catch (Exception ex) {
			json.setSuccess(false);
			json.setMsg(ex.getMessage());
			LOGGER.error(ex.getMessage());
		}
		return json;
	}

	private String callAPI(HttpUriRequest request) throws Exception {
		String resJson = "";
		// Add AuthCache to the execution context
		final HttpClientContext context = HttpClientContext.create();
		context.setCredentialsProvider(credsProvider);
		context.setAuthCache(authCache);

		HttpClient httpclient = HttpClientBuilder.create().build();
		HttpResponse response = httpclient.execute(request, context);
		if (response.getStatusLine().getStatusCode() >= 400) {
			LOGGER.error(response.getStatusLine().getStatusCode() + " - Cannot get response successfully.");
			throw new Exception(response.getStatusLine().getStatusCode() + " - Connection Error to " + request.getURI().toString());
		} else {
			resJson = EntityUtils.toString(response.getEntity());
		}
		return resJson;
	}

	private List<Commit> getCommits(String field, String[] keywords) throws Exception {
		List<Commit> commits = new ArrayList<Commit>();

		HttpUriRequest httpget = RequestBuilder.get().setUri(new URI(jiraRestURL)).addParameter("field", "key")
				.addParameter("issueKeys", StringUtils.join(keywords, ",")).build();
		String response = callAPI(httpget);
		if (!response.equals("")) {
			JSONArray array = new JSONArray(response);
			for (int i = 0; i < array.length(); i++) {
				JSONObject obj = array.getJSONObject(i);
				int revision = obj.getInt("revision");
				String key = obj.getString("key");
				String author = obj.getString("author");
				String message = obj.getString("message");
				String project = obj.getString("project");
				String repository = obj.getString("repository");
				commits.add(new Commit(revision, key, author, message, project, repository));
			}
		}
		return commits;

	}
}
