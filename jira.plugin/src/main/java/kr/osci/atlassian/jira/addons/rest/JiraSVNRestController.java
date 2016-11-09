package kr.osci.atlassian.jira.addons.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.osci.atlassian.jira.addons.models.Commit;
import kr.osci.atlassian.jira.addons.repositories.CommitRepository;
import kr.osci.atlassian.jira.addons.services.ConfigurationService;

@Path("/")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Named("CustomGroupRest")
public class JiraSVNRestController {
	@Inject
	private CommitRepository commitRepository;
	private static final Logger log = LoggerFactory.getLogger(JiraSVNRestController.class);
	@Inject
	private ConfigurationService confService;

	@GET
	@Path("/commits")
	public Response getCommits(@QueryParam("field") String field, @QueryParam("issueKeys") String issueKeys) throws IOException {
		commitRepository.setPath(confService.getSVNDataDir());

		String[] keys = issueKeys.split(",");
		List<Commit> result = new ArrayList<Commit>();
		result = commitRepository.search(field, keys);
		return Response.ok(result).build();
	}
}
