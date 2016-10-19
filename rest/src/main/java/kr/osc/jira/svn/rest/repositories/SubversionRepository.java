package kr.osc.jira.svn.rest.repositories;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import kr.osc.jira.svn.rest.models.SVNElement;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
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

@Repository(value = "subversionRepository")
public class SubversionRepository implements InitializingBean {
	@Value("${svn.url}")
	private String url;
	@Value("${svn.username}")
	private String svnUsername;
	@Value("${svn.pwd}")
	private String svnPassword;
	@Value("${svn.tmp.dir}")
	private String exportPathStr;
	@Value("${svn.eol.style}")
	private String eolStyle;
	@Value("${svn.root.dir}")
	private String svnRootDir;
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
		destPath = new File(exportPathStr);

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
	 */
	public String export(String pegRev, String rev, boolean overwrite) throws SVNException {

		SVNRevision pegRevision = pegRev == null ? SVNRevision.HEAD : SVNRevision.parse(pegRev);
		SVNRevision revision = rev == null ? SVNRevision.HEAD : SVNRevision.parse(rev);
		SVNDepth dept = SVNDepth.INFINITY;
		SVNUpdateClient updateClient = clientManager.getUpdateClient();
		/*
		 * sets externals not to be ignored during the export
		 */
		updateClient.setIgnoreExternals(false);
		/*
		 * returns the number of the revision at which the working copy is 
		 */
		long exportedRevision = updateClient.doExport(svnUrl, destPath, pegRevision, revision, eolStyle, overwrite, dept);
		if (exportedRevision > 0) {
			String zipFileName = svnRootDir;
			return createZipFile(zipFileName);
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
				element.setChildNodes(listEntries(repository, (path.equals("")) ? entry.getName() : path + "/" + entry.getName()));
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
		SVNURL rootUrl = svnRepository.getRepositoryRoot(true);
		root.setResource(rootUrl.toString());
		root.setUrl(rootUrl.toString());
		root.setChildNodes(listEntries(svnRepository, ""));
		return root;
	}

	private String createZipFile(String fileName) {
		String subDirSeparator = "/";
		if (serverOs.equals("windows")) {
			subDirSeparator = "\\";
		}
		String zipFile = destPath.getPath() + subDirSeparator + fileName + ".zip";
		File sourceDir = new File(destPath.getPath() + subDirSeparator + fileName);
		ZipUtil.pack(sourceDir, new File(zipFile));
		return zipFile;
	}

	public long importSourceCodes(File sourcePath, String destinationPath, String commitMessage, boolean isRecursive) throws SVNException {
		SVNNodeKind nodeKind = svnRepository.checkPath(destinationPath, svnRepository.getLatestRevision());
		if (nodeKind == SVNNodeKind.DIR) {
			destinationPath = destinationPath + "/" + sourcePath.getName();
		}
		SVNURL dstUrl = SVNURL.parseURIEncoded(destinationPath);

		SVNCommitClient commitClient = clientManager.getCommitClient();
		SVNCommitInfo commitInfo = commitClient.doImport(sourcePath, dstUrl, commitMessage, null, true, false, SVNDepth.fromRecurse(isRecursive));
		return commitInfo.getNewRevision();
	}
}
