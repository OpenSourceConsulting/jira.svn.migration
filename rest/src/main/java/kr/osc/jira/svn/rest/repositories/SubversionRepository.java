package kr.osc.jira.svn.rest.repositories;

import java.io.File;
import java.sql.Timestamp;
import java.util.Date;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
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
	@Value("${svn.tmp.export.dir}")
	private String exportPathStr;
	@Value("${svn.eol.style}")
	private String eolStyle;

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
	public void export(String pegRev, String rev, boolean overwrite) throws SVNException {

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
			createZipFile(String.valueOf((new Date().getTime())) + ".zip");
		}

	}

	private String createZipFile(String fileName) {
		String zipFile = destPath.getPath() + "\\" + fileName;
		ZipUtil.pack(destPath, new File(zipFile));
		return zipFile;
	}
}
