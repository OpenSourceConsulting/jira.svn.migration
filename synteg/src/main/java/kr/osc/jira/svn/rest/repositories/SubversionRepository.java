package kr.osc.jira.svn.rest.repositories;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import kr.osc.jira.svn.rest.models.SVNElement;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.zeroturnaround.zip.ZipUtil;
import org.zeroturnaround.zip.commons.FileUtils;

@Repository(value = "subversionRepository")
public class SubversionRepository implements InitializingBean {
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
	@Value("${svn.eol.style}")
	private String eolStyle;
	@Value("${svn.export.zip.name}")
	private String svnExportZipName;
	@Value("${server.os}")
	private String serverOs;

	private SVNRepository svnRepository;
	private SVNClientManager clientManager;
	private SVNURL svnUrl;
	private File destPath;

	@Override
	public void afterPropertiesSet() throws Exception {
		clientManager = SVNClientManager.newInstance();
		ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(svnUsername, svnPassword.toCharArray());
		svnUrl = SVNURL.parseURIEncoded(url);
		svnRepository = SVNRepositoryFactory.create(svnUrl);
		svnRepository.setAuthenticationManager(authManager);
		clientManager.setAuthenticationManager(authManager);
		destPath = new File(tmpDownloadDir);

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
	 * Checks out a working copy of url at revision, looked up at pegRevision, using dstPath as the root directory of the newly checked out working copy.
	 * 
	 * @param url
	 * @param destPath
	 * @param pegRevision
	 * @param revision
	 * @param dept
	 * @param allowUnversionObstruction
	 * @return
	 * @throws SVNException
	 */

	public long checkout(String pegRev, String rev, boolean allowUnversionObstruction) throws SVNException {
		SVNRevision pegRevision = SVNRevision.parse(pegRev);
		SVNRevision revision = SVNRevision.parse(rev);
		SVNDepth dept = SVNDepth.INFINITY;
		SVNUpdateClient updateClient = clientManager.getUpdateClient();
		/*
		 * sets externals not to be ignored during the checkout
		 */
		updateClient.setIgnoreExternals(false);
		/*
		 * returns the number of the revision at which the working copy is 
		 */
		return updateClient.doCheckout(svnUrl, destPath, pegRevision, revision, dept, allowUnversionObstruction);

	}

	/**
	 * Exports a clean directory or single file from a repository.
	 * 
	 * @param url
	 * @param destPath
	 * @param pegRevision
	 * @param revision
	 * @param eolStyle
	 * @param overwrite
	 * @param dept
	 * @return
	 * @throws SVNException
	 * @throws IOException
	 */
	public String export(String pegRev, String rev, boolean overwrite, boolean deleteTmp) throws SVNException, IOException {

		SVNRevision pegRevision = pegRev == null ? SVNRevision.HEAD : SVNRevision.parse(pegRev);
		SVNRevision revision = rev == null ? SVNRevision.HEAD : SVNRevision.parse(rev);
		SVNDepth dept = SVNDepth.INFINITY;
		SVNUpdateClient updateClient = clientManager.getUpdateClient();
		SVNNodeKind node = this.checkPath(svnUrl, revision.getNumber());
		if (node == SVNNodeKind.NONE || node == SVNNodeKind.UNKNOWN) {
			return StringUtils.EMPTY;
		}
		/*
		 * sets externals not to be ignored during the export
		 */
		updateClient.setIgnoreExternals(false);
		/*
		 * returns the number of the revision at which the working copy is 
		 */
		long exportedRevision = updateClient.doExport(svnUrl, destPath, pegRevision, revision, eolStyle, overwrite, dept);
		if (exportedRevision > 0) {
			String zipFileName = svnExportZipName;
			return createZipFile(zipFileName, deleteTmp);
		}
		return StringUtils.EMPTY;
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

	private String createZipFile(String fileName, boolean deleteTemp) throws IOException {
		String subDirSeparator = "/";
		if (serverOs.equals("windows")) {
			subDirSeparator = "\\";
		}
		if (!Files.exists(Paths.get(tmpZipDir))) {
			Files.createDirectories(Paths.get(this.tmpZipDir));
		}
		String zipFile = this.tmpZipDir + subDirSeparator + fileName + ".zip";
		File sourceDir = new File(destPath.getPath());
		File zip = new File(zipFile);
		ZipUtil.pack(sourceDir, zip);
		if (deleteTemp) {
			FileUtils.deleteQuietly(sourceDir);
		}
		return zipFile;
	}

	public long importSourceCodes(File sourcePath, String destinationPath, String commitMessage, boolean isUnzip, boolean isRecursive) throws SVNException {
		String subDirSeparator = "/";
		if (serverOs.equals("windows")) {
			subDirSeparator = "\\";
		}
		File unzipDir = null;
		SVNCommitClient commitClient = clientManager.getCommitClient();
		SVNURL dstUrl = SVNURL.parseURIEncoded(destinationPath);
		SVNNodeKind kind = checkPath(dstUrl, -1);
		SVNCommitInfo commitInfo = null;
		List<SVNURL> importSVNURLs = new ArrayList<SVNURL>();
		List<File> sources = new ArrayList<File>();
		if (kind == SVNNodeKind.DIR) {
			//import a foder with multiple files to dir
			if (sourcePath.getName().endsWith(".zip") && isUnzip) {
				unzipDir = new File(tmpUploadDir + sourcePath.getName().replace(".zip", ""));
				ZipUtil.unpack(sourcePath, unzipDir);
				for (String l : unzipDir.list()) {
					importSVNURLs.add(dstUrl.appendPath(l, false));
					sources.add(new File(unzipDir.getPath() + subDirSeparator + l));
				}

			} else {
				//import single file to dir
				importSVNURLs.add(dstUrl.appendPath(sourcePath.getName(), false));
				sources.add(sourcePath);
			}

		} else if (kind == SVNNodeKind.FILE) {
			importSVNURLs.add(dstUrl);
			sources.add(sourcePath);
		} else if (kind == SVNNodeKind.NONE) {
			commitInfo = commitClient.doImport(sourcePath, dstUrl, commitMessage, null, true, false, SVNDepth.fromRecurse(isRecursive));
			sourcePath.delete();
		}
		if (kind == SVNNodeKind.DIR || kind == SVNNodeKind.FILE) {
			List<SVNURL> deletingUrls = new ArrayList<SVNURL>();
			for (SVNURL u : importSVNURLs) {
				SVNNodeKind n = checkPath(u, -1);
				if (n != SVNNodeKind.UNKNOWN && n != SVNNodeKind.NONE) {
					deletingUrls.add(u);
				}
			}
			SVNURL[] urls = new SVNURL[deletingUrls.size()];
			urls = deletingUrls.toArray(urls);
			if (urls.length > 0) {
				commitClient.doDelete(urls, "[Deleted]" + commitMessage);
			}
			for (int i = 0; i < importSVNURLs.size(); i++) {
				commitInfo = commitClient.doImport(sources.get(i), importSVNURLs.get(i), commitMessage, null, true, false, SVNDepth.fromRecurse(isRecursive));
			}
		}
		//delete uploaded file from tmp dir
		sourcePath.delete();
		if (unzipDir != null) {
			FileUtils.deleteQuietly(unzipDir);
		}
		return commitInfo == null ? -1 : commitInfo.getNewRevision();
	}

	private SVNNodeKind checkPath(SVNURL path, long revision) throws SVNException {
		SVNRepository repo = SVNRepositoryFactory.create(path);
		ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(svnUsername, svnPassword.toCharArray());
		repo.setAuthenticationManager(authManager);
		return repo.checkPath("", revision);
	}

}
