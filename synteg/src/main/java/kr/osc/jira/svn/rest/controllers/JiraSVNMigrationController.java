/**
 * 
 */
package kr.osc.jira.svn.rest.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
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
import kr.osc.jira.svn.rest.repositories.SubversionRepository;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.tmatesoft.svn.core.SVNException;
import org.zeroturnaround.zip.commons.FileUtils;
import org.zeroturnaround.zip.commons.IOUtils;

/**
 * @author Bongjin Kwon
 * @author Tran Ho
 *
 */
@Controller
public class JiraSVNMigrationController implements InitializingBean {
	private static final Logger LOGGER = LoggerFactory.getLogger(JiraSVNMigrationController.class);
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
	@Value("${svn.tmp.upload.dir}")
	private String tmpUploadDir;
	@Value("${jira.svn.rest.url}")
	private String jiraRestURL;
	@Value("${svn.tmp.delete}")
	private boolean deleteSVNTmp;
	private HttpHost jiraHost;
	private CredentialsProvider credsProvider;
	private AuthCache authCache;

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
	public GridJsonResponse getProjectList(GridJsonResponse json) {
		try {
			HttpUriRequest httpget = RequestBuilder.get().setUri(new URI(jiraHost.toURI() + "/rest/api/2/project")).build();
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
		HttpUriRequest httpget = RequestBuilder.get().setUri(new URI(jiraHost.toURI() + "/rest/api/2/search")).addParameter("jql", jql).build();
		return callAPI(httpget);
	}

	@RequestMapping("/api/issues/filter")
	@ResponseBody
	public GridJsonResponse search(GridJsonResponse json, String projectId, String fromDate, String toDate, String fields) {
		String jql = "project=" + projectId;
		if (StringUtils.isNotEmpty(fromDate)) {
			jql += " AND created>=\"" + fromDate + "\"";
		}
		if (StringUtils.isNotEmpty(toDate)) {
			jql += " AND created<=\"" + toDate + "\"";
		}
		try {
			jql = URLEncoder.encode(jql, "UTF-8");
			HttpUriRequest httpget = RequestBuilder.get().setUri(new URI(jiraHost.toURI() + "/rest/api/2/search?jql=" + jql + "&fields=" + fields)).build();
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

	//	@RequestMapping("/api/commits")
	//	@ResponseBody
	//	public List<Commit> getCommits(String field, String value) throws IOException {
	//		return commitRepo.search(field, value);
	//	}

	@RequestMapping("/api/svn/export")
	public void export(@RequestParam(value = "issueKeys[]") String[] issueKeys, HttpServletResponse response) {
		try {
			List<Commit> commits = getCommits("key", issueKeys);
			if (commits.size() > 0) {
				List<Integer> revisions = new ArrayList<Integer>();
				for (Commit c : commits) {
					revisions.add(c.getRevision());
				}
				String zipFile = subversionRepo.export(revisions, true, deleteSVNTmp);
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
					//delete on server
					f.delete();
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
			@RequestParam("file") MultipartFile file, String message) {
		File uploadDir = new File(tmpUploadDir);
		if (!uploadDir.exists()) {
			uploadDir.mkdir();
		}
		try {
			String uploadedFilePath = tmpUploadDir + file.getOriginalFilename();
			Path folder = Paths.get(uploadedFilePath);
			Path path = folder;
			if (!Files.exists(folder, LinkOption.values())) {
				path = Files.createFile(folder);
			}
			InputStream input = file.getInputStream();
			Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING);
			String log = subversionRepo.checkDiff(selectedPath, uploadedFilePath, file.getOriginalFilename(), isExtract, message);
			//set value for next action
			HttpSession session = req.getSession();
			session.setAttribute("tmp_file_name", file.getOriginalFilename());
			session.setAttribute("selected_svn_path", selectedPath);
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
		String sourceFileName = (String) session.getAttribute("tmp_file_name");
		String selectPath = (String) session.getAttribute("selected_svn_path");
		session.removeAttribute("tmp_file_name");
		session.removeAttribute("selected_svn_path");
		try {
			subversionRepo.importSourceCodes(sourceFileName, selectPath, message, isMultipleFiles, true);
		} catch (Exception ex) {
			json.setSuccess(false);
			json.setMsg(ex.getMessage());
			LOGGER.error(ex.getMessage());
		}
		return json;
	}

	private String callAPI(HttpUriRequest request) {
		String resJson = "";
		try {
			// Add AuthCache to the execution context
			final HttpClientContext context = HttpClientContext.create();
			context.setCredentialsProvider(credsProvider);
			context.setAuthCache(authCache);

			HttpClient httpclient = HttpClientBuilder.create().build();
			HttpResponse response = httpclient.execute(request, context);
			if (response.getStatusLine().getStatusCode() >= 400) {
				LOGGER.error(response.getStatusLine().getStatusCode() + " - Cannot get response successfully.");
			} else {
				resJson = EntityUtils.toString(response.getEntity());
			}
			return resJson;
		} catch (Exception ex) {
			LOGGER.error(ex.getMessage());
		}
		return resJson;
	}

	private List<Commit> getCommits(String field, String[] keywords) throws URISyntaxException {
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
