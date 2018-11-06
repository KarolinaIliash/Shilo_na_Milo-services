package Main.Controller;

import Main.Entity.Service;
import Main.SearchUtils.*;
import Main.Constants.*;
import Main.SearchUtils.*;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Optional;

@RestController
@RequestMapping(path="/services")
public class GetController {
    @GetMapping("/all")
    public Iterable<Service> getAllServices(@RequestParam Optional<Integer> amount, @RequestParam Optional<Integer> start) {
        HttpSolrClient solr = UtilFuncs.getSolrClient();

        SolrQuery query = new SolrQuery();
        query.set("q", "*:*");

        UtilFuncs.setDefaults(query, amount, start);

        return UtilFuncs.getByQuery(solr, query);
    }

    @GetMapping("/id")
    public Answer<Service> getServiceById(@RequestParam Integer id){
        Service result = null;

        HttpSolrClient solr = UtilFuncs.getSolrClient();

        try {
            SolrDocument doc = solr.getById(id.toString());
            if(doc == null || doc.isEmpty())
                return null;
            result = UtilFuncs.fillService(doc);
        } catch (SolrServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Answer<Service>(Codes.OK, result);
    }

    @GetMapping("/category")
    public Answer<Iterable<Service>> getServicesInCategory(@RequestParam String category,
                                                   @RequestParam Optional<Integer> amount,
                                                   @RequestParam Optional<Integer> start){
        HttpSolrClient solr = UtilFuncs.getSolrClient();

        SolrQuery query = new SolrQuery();
        query.set("q", "category:" + category);

        UtilFuncs.setDefaults(query, amount, start);

        return new Answer<>(Codes.OK, UtilFuncs.getByQuery(solr, query));
    }

    @GetMapping("/user")
    public Answer<Iterable<Service>> getServicesByUser(@RequestParam Integer user,
                                               Optional<Integer> amount,
                                               Optional<Integer> start) {
        HttpSolrClient solr = UtilFuncs.getSolrClient();

        SolrQuery query = new SolrQuery();
        query.set("q", "user_id:" + user.toString());

        UtilFuncs.setDefaults(query, amount, start);

        return new Answer<>(Codes.OK, UtilFuncs.getByQuery(solr, query));
    }

    @GetMapping("/intext")
    public Answer<Iterable<Service>> serviceByText(@RequestParam String text,
                                           Optional<Integer> amount,
                                           Optional<Integer> start){
        text = text.toLowerCase();
        HttpSolrClient solr = UtilFuncs.getSolrClient();

        SolrQuery query = new SolrQuery();
        query.set("q", "_text_:*" + text + "*");

        UtilFuncs.setDefaults(query, amount, start);

        return new Answer<>(Codes.OK, UtilFuncs.getByQuery(solr, query));
    }
}
