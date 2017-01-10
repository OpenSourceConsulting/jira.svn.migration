package kr.osc.jira.svn.rest.repositories;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import kr.osc.jira.svn.rest.models.SVNElement;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnDiffGenerator;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNRevisionRange;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc2.SvnDiff;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.zeroturnaround.zip.ZipUtil;
import org.zeroturnaround.zip.commons.FileUtils;

@Repository(value = "subversionRepository")
public class SubversionRepository implements InitializingBean {
	private static final Logger LOGGER = Logger.getLogger(SubversionRepository.class);

	@Value("${svn.url}")
	private String url;
	@Value("${svn.username}")
	private String svnUsername;
	@Value("${svn.pwd}")
	private String svnPassword;
	@Value("${svn.tmp.download.dir}")
	private String tmpDownloadDir;
	@Value("${svn.tmp.zip.dir}")
	private String tmpZipDir;
	@Value("${svn.tmp.upload.dir}")
	private String tmpUploadDir;
	@Value("${svn.tmp.workingcopy.dir}")
	private String tmpWCDir;
	@Value("${svn.eol.style}")
	private String eolStyle;
	@Value("${svn.export.zip.name}")
	private String svnExportZipName;
	@Value("${server.os}")
	private String serverOs;

	private SVNRepository svnRepository;
	private SVNClientManager clientManager;
	private SVNURL svnUrl;
	private String subDirSeparator = "/";
	ISVNAuthenticationManager authManager = null;

	@Override
	public void afterPropertiesSet() throws Exception {
		clientManager = SVNClientManager.newInstance();
		authManager = SVNWCUtil.createDefaultAuthenticationManager(svnUsername, svnPassword.toCharArray());
		svnUrl = SVNURL.parseURIEncoded(url);
		svnRepository = SVNRepositoryFactory.create(svnUrl);
		svnRepository.setAuthenticationManager(authManager);
		clientManager.setAuthenticationManager(authManager);
		//create tmp dirs
		if (!Files.exists(Paths.get(tmpWCDir))) {
			Files.createDirectories(Paths.get(this.tmpWCDir));
		}
		if (!Files.exists(Paths.get(tmpDownloadDir))) {
			Files.createDirectories(Paths.get(this.tmpDownloadDir));
		}
		if (!Files.exists(Paths.get(tmpUploadDir))) {
			Files.createDirectories(Paths.get(this.tmpUploadDir));
		}
		if (!Files.exists(Paths.get(tmpZipDir))) {
			Files.createDirectories(Paths.get(this.tmpZipDir));
		}

		if (serverOs.equals("windows")) {
			subDirSeparator = "\\";
		}
	}

	/**
	 * Gets a repository's root directory location
	 * 
	 * @param forceConn
	 *            force connection
	 * @return
	 * @return
	 * @throws SVNException
	 */
	public SVNURL getRepositoryRoot(boolean forceConn) throws SVNException {
		return svnRepository.getRepositoryRoot(forceConn);
	}

	/**
	 * Exports a clean directory or single file from a repository.
	 * 
	 * @param url
	 * @param destPath
	 * @param revisions
	 * @param eolStyle
	 * @param overwrite
	 * @param dept
	 * @return
	 * @throws SVNException
	 * @throws IOException
	 */
	public String[] export(List<Integer> revisions, boolean overwrite, boolean deleteTmp) throws SVNException, IOException {
		String[] result = new String[2];
		SVNDepth dept = SVNDepth.INFINITY;
		SVNUpdateClient updateClient = clientManager.getUpdateClient();
		List<Pair> changedPaths = getChangedPaths(revisions);

		//gen temp dir name 
		String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
		if (!Files.exists(Paths.get(this.tmpDownloadDir + subDirSeparator + timeStamp))) {
			Files.createDirectories(Paths.get(this.tmpDownloadDir + subDirSeparator + timeStamp));
		}
		String tmpPath = this.tmpDownloadDir + subDirSeparator + timeStamp;
		for (Pair<String, Long> path : changedPaths) {
			String subtmpPath = "";
			SVNURL svnurl = SVNURL.parseURIEncoded(url + path.getLeft());
			SVNNodeKind node = this.checkPath(svnurl, path.getRight());
			if (node == SVNNodeKind.NONE || node == SVNNodeKind.UNKNOWN) {
				continue;
			}
			if (node == SVNNodeKind.FILE) {
				subtmpPath = this.createSubDirs(tmpPath, path.getLeft());
				if (!Files.exists(Paths.get(subtmpPath))) {
					Files.createDirectories(Paths.get(subtmpPath));
				}
			}
			updateClient.setIgnoreExternals(false);
			SVNRevision revision = SVNRevision.create(path.getRight());
			updateClient.doExport(svnurl, new File(subtmpPath), revision, revision, eolStyle, overwrite, dept);
		}

		String zipFileName = svnExportZipName;
		result[0] = createZipFile(new File(tmpPath).getPath(), zipFileName, deleteTmp);
		result[1] = tmpPath;
		return result;
	}

	private String createSubDirs(String tmpPath, String svnFilePath) {
		String path = tmpPath;
		String[] arr = svnFilePath.split("/");
		//the last item of arr is a file name
		for (int i = 0; i < arr.length - 1; i++) {
			path += subDirSeparator + arr[i];
		}
		path += subDirSeparator;
		return path;
	}

	public List<SVNElement> listEntries(SVNRepository repository, String path) throws SVNException {
		List<SVNElement> children = new ArrayList<SVNElement>();
		Collection entries = repository.getDir(path, -1, null, (Collection) null);
		Iterator iterator = entries.iterator();
		while (iterator.hasNext()) {
			SVNDirEntry entry = (SVNDirEntry) iterator.next();
			SVNElement element = new SVNElement();
			element.setLastAuthor(entry.getAuthor());
			element.setLastChanged(entry.getDate());
			element.setRevision(entry.getRevision());
			element.setUrl(entry.getURL().toString());
			element.setResource(entry.getName());
			if (entry.getKind() == SVNNodeKind.DIR) {
				element.setType("dir");
				element.setChildNodes(null);
			} else if (entry.getKind() == SVNNodeKind.FILE) {
				element.setType("file");
			}
			children.add(element);
		}

		Collections.sort(children);
		return children;
	}

	public SVNElement getSVNTree() throws SVNException {

		//add the root element by svn url
		SVNElement root = new SVNElement();
		SVNURL rootUrl = svnRepository.getLocation();
		root.setResource(rootUrl.toString());
		root.setUrl(rootUrl.toString());
		root.setChildNodes(listEntries(svnRepository, ""));
		return root;
	}

	public List<SVNElement> getSVNChildNodes(String parentPath) throws SVNException {
		SVNURL url = SVNURL.parseURIEncoded(parentPath);
		String subPath = parentPath.substring(svnRepository.getLocation().toString().length());
		if (checkPath(url, -1) == SVNNodeKind.DIR) {
			return listEntries(svnRepository, subPath);
		}
		return null;
	}

	private String createZipFile(String path, String fileName, boolean deleteTemp) throws IOException {

		String zipFile = this.tmpZipDir + subDirSeparator + fileName + ".zip";
		File sourceDir = new File(path);
		File zip = new File(zipFile);
		ZipUtil.pack(sourceDir, zip);
		if (deleteTemp) {
			FileUtils.deleteQuietly(sourceDir);
		}
		return zipFile;
	}

	public long importSourceCodes(String filePath, boolean isLocalFiles, String destinationPath, String commitMessage, boolean isMultipleFiles,
			boolean isRecursive) throws SVNException, IOException {

		SVNCommitClient commitClient = clientManager.getCommitClient();
		File workingCopy = new File(tmpWCDir);
		SVNCommitInfo commitInfo = null;
		List<SVNURL> svnURLs = new ArrayList<SVNURL>();
		List<File> sources = new ArrayList<File>();
		SVNDiffClient diffClient = clientManager.getDiffClient();
		//look for changed files & dir
		File serverPath = new File(filePath);
		if (isLocalFiles) {
			if (filePath.contains(".zip") && isMultipleFiles) {
				File unzipDir = new File(filePath.replace(".zip", ""));
				String[] fileAndSubDirs = unzipDir.list();
				for (String f : fileAndSubDirs) {
					sources.add(new File(workingCopy.getPath() + subDirSeparator + f));
					svnURLs.add(SVNURL.parseURIEncoded(destinationPath + "/" + f));
				}
			} else {
				sources.add(new File(workingCopy.getPath() + subDirSeparator + serverPath.getName()));
				svnURLs.add(SVNURL.parseURIEncoded(destinationPath + "/" + serverPath.getName()));
			}
		} else {
			if (serverPath.isDirectory()) {
				String[] fileAndSubDirs = serverPath.list();
				for (String f : fileAndSubDirs) {
					sources.add(new File(workingCopy.getPath() + subDirSeparator + f));
					svnURLs.add(SVNURL.parseURIEncoded(destinationPath + "/" + f));
				}
			} else {
				sources.add(new File(workingCopy.getPath() + subDirSeparator + serverPath.getName()));
				svnURLs.add(SVNURL.parseURIEncoded(destinationPath + "/" + serverPath.getName()));
			}
		}
		//merge
		for (int i = 0; i < svnURLs.size(); i++) {
			diffClient.doMerge(svnURLs.get(i), SVNRevision.HEAD, Collections.singleton(new SVNRevisionRange(SVNRevision.HEAD, SVNRevision.HEAD)),
					sources.get(i), SVNDepth.INFINITY, true, false, false, false);
		}
		//commit
		commitClient.doCommit(sources.toArray(new File[0]), false, commitMessage, null, null, true, false, SVNDepth.INFINITY);
		//empty upload dir & working copy
		if (Files.exists((new File(tmpUploadDir)).toPath())) {
			FileUtils.cleanDirectory(new File(tmpUploadDir));
		}
		FileUtils.cleanDirectory(workingCopy);

		return commitInfo == null ? -1 : commitInfo.getNewRevision();
	}

	/**
	 * Check diff between file in svn repo and new one.
	 * 
	 * @param selectedSVNPath
	 * @param isLocalFile
	 * @param filePath
	 * @param isExtract
	 * @return
	 * @throws SVNException
	 * @throws IOException
	 */
	public String checkDiff(String selectedSVNPath, boolean isLocalFile, String filePath, boolean isExtract) throws SVNException, IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		//init for check diff
		SvnDiffGenerator diffGenerator = new SvnDiffGenerator();
		SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
		svnOperationFactory.setAuthenticationManager(authManager);
		SVNUpdateClient updateClient = clientManager.getUpdateClient();
		SVNWCClient wcClient = clientManager.getWCClient();
		File[] addFiles = null;

		File workingCopy = new File(tmpWCDir);
		//in order to check different between current source and new one, it should be working on working copy dir of svn. 
		//check out to tmp working copy dir
		if (Files.exists((new File(tmpWCDir)).toPath())) {
			FileUtils.cleanDirectory(new File(tmpWCDir));
		}
		updateClient.doCheckout(SVNURL.parseURIEncoded(selectedSVNPath), workingCopy, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, false);
		if (isLocalFile) {
			File file = new File(filePath);
			String fileName = file.getName();
			//copy changed files to working copy
			if (fileName.endsWith(".zip") && isExtract) {
				File unzipDir = new File(tmpUploadDir + fileName.replace(".zip", ""));
				ZipUtil.unpack(file, unzipDir);
				addFiles = new File[unzipDir.list().length];
				FileUtils.copyDirectory(unzipDir, workingCopy);
				int index = 0; //index for loop
				for (String f : unzipDir.list()) {
					addFiles[index] = new File(workingCopy + subDirSeparator + f);
					index++;
				}
			} else {
				addFiles = new File[1];
				FileUtils.copyFileToDirectory(new File(tmpUploadDir + subDirSeparator + fileName), workingCopy);
				addFiles[0] = new File(workingCopy + subDirSeparator + fileName);
			}
		} else { //copy file from server to working copy dir
			File serverPath = new File(filePath);
			if (serverPath.isFile()) {
				addFiles = new File[1];
				FileUtils.copyFileToDirectory(serverPath, workingCopy);
				addFiles[0] = new File(workingCopy + subDirSeparator + serverPath.getName());
			} else if (serverPath.isDirectory()) {
				FileUtils.copyDirectory(serverPath, workingCopy);
				int index = 0; //index for loop
				addFiles = new File[serverPath.list().length];
				for (String f : serverPath.list()) {
					addFiles[index] = new File(workingCopy + subDirSeparator + f);
					index++;
				}
			}

		}
		//add addfiles to working copy (in client repo only)
		wcClient.doAdd(addFiles, true, false, false, SVNDepth.INFINITY, false, false, false);
		//check diff
		diffGenerator.setBasePath(workingCopy);
		SvnDiff diff = svnOperationFactory.createDiff();
		diff.setSources(SvnTarget.fromFile(workingCopy, SVNRevision.HEAD), SvnTarget.fromFile(workingCopy, SVNRevision.WORKING));
		diff.setDiffGenerator(diffGenerator);
		diff.setOutput(os);
		diff.run();
		svnOperationFactory.dispose();
		return os.toString("UTF-8");
	}

	private List<Pair> getChangedPaths(List<Integer> revisions) throws SVNException {
		List<Pair> paths = new ArrayList<Pair>();
		for (Integer r : revisions) {
			Collection logEntries = svnRepository.log(new String[] { "" }, null, Long.valueOf(r), Long.valueOf(r), true, true);
			for (Iterator entries = logEntries.iterator(); entries.hasNext();) {
				paths = getChangedPaths(paths, (SVNLogEntry) entries.next());
			}
		}

		return paths;
	}

	private List<Pair> getChangedPaths(List<Pair> paths, SVNLogEntry entry) {
		if (entry.getChangedPaths().size() > 0) {
			/*
			 * keys are changed paths
			 */
			Set changedPathsSet = entry.getChangedPaths().keySet();

			for (Iterator<?> changedPaths = changedPathsSet.iterator(); changedPaths.hasNext();) {
				List<Pair> clonedPaths = new ArrayList<Pair>(paths);
				/*
				 * obtains a next SVNLogEntryPath
				 */
				SVNLogEntryPath entryPath = (SVNLogEntryPath) entry.getChangedPaths().get(changedPaths.next());
				Pair<String, Long> path = Pair.of(entryPath.getPath(), entry.getRevision());
				if (clonedPaths.size() > 0) {
					int idx = checkExistingChangedPath(path, clonedPaths);
					if (idx == -1) {
						paths.add(path);
					} else {
						Pair p = clonedPaths.get(idx);
						if (entry.getRevision() > Long.valueOf(p.getRight().toString())) {
							paths.set(idx, path);
						}
					}
				} else {

					paths.add(path);
				}
			}
		}
		return paths;
	}

	private Integer checkExistingChangedPath(Pair pair, List<Pair> paths) {
		for (int i = 0; i < paths.size(); i++) {
			Pair p = paths.get(i);
			if (p.getLeft().toString().equals(pair.getLeft().toString())) {
				return i;
			}
		}
		return -1;
	}

	private SVNNodeKind checkPath(SVNURL path, long revision) throws SVNException {
		SVNRepository repo = SVNRepositoryFactory.create(path);
		ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(svnUsername, svnPassword.toCharArray());
		repo.setAuthenticationManager(authManager);
		return repo.checkPath("", revision);
	}

}
