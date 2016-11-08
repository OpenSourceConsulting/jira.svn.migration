package kr.osc.jira.svn.rest.repositories;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kr.osc.jira.svn.rest.models.Commit;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository(value = "commitRepository")
public class CommitRepository {
	@Value("${jira.svn.base.dir}")
	private String path;

	public List<Commit> search(String field, String[] keywords) throws IOException {
		File file = new File(path);
		Directory index = FSDirectory.open(file);
		IndexReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		List<Commit> results = new ArrayList<Commit>();
		for (String keyword : keywords) {
			results.addAll(this.search(searcher, field, keyword));
		}
		reader.close();
		index.close();
		return results;
	}

	private List<Commit> search(IndexSearcher searcher, String field, String keyword) throws IOException {
		List<Commit> results = new ArrayList<Commit>();
		Query q = new TermQuery(new Term(field, keyword));
		//search		
		TopDocs docs = searcher.search(q, 1);
		if (docs.totalHits > 0) {
			docs = searcher.search(q, docs.totalHits);
		}
		ScoreDoc[] hits = docs.scoreDocs;
		for (int i = 0; i < hits.length; ++i) {
			int docId = hits[i].doc;
			Document d = searcher.doc(docId);
			results.add(new Commit(Integer.parseInt(d.get("revision")), d.get("key"), d.get("author"), d.get("message").trim(), d.get("project"), d
					.get("repository")));
		}

		return results;
	}
}
