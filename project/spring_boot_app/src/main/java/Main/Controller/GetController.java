package Main.Controller;

import Main.Entity.Service;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
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
        HttpSolrClient solr = getSolrClient();

        SolrQuery query = new SolrQuery();
        query.set("q", "*:*");

        setDefaults(query, amount, start);

        return getByQuery(solr, query);
    }

    @GetMapping("/id")
    public Service getServiceById(@RequestParam Integer id){
        Service result = null;

        HttpSolrClient solr = getSolrClient();

        try {
            SolrDocument doc = solr.getById(id.toString());
            if(doc == null || doc.isEmpty())
                return null;
            result = fillService(doc);
        } catch (SolrServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    @GetMapping("/category")
    public Iterable<Service> getServicesInCategory(@RequestParam String category,
                                                   @RequestParam Optional<Integer> amount,
                                                   @RequestParam Optional<Integer> start){
        HttpSolrClient solr = getSolrClient();

        SolrQuery query = new SolrQuery();
        query.set("q", "category:" + category);

        setDefaults(query, amount, start);

        return getByQuery(solr, query);
    }

    @GetMapping("/user")
    public Iterable<Service> getServicesByUser(@RequestParam Integer user,
                                               Optional<Integer> amount,
                                               Optional<Integer> start) {
        HttpSolrClient solr = getSolrClient();

        SolrQuery query = new SolrQuery();
        query.set("q", "user_id:" + user.toString());

        setDefaults(query, amount, start);

        return getByQuery(solr, query);
    }

    @GetMapping("/intext")
    public Iterable<Service> serviceByText(@RequestParam String text,
                                           Optional<Integer> amount,
                                           Optional<Integer> start){
        HttpSolrClient solr = getSolrClient();

        SolrQuery query = new SolrQuery();
        query.set("q", "_text_:*" + text + "*");

        setDefaults(query, amount, start);

        return getByQuery(solr, query);
    }

    private Service fillService(SolrDocument doc) {
        Service service = new Service();
        service.setId(Integer.parseInt(doc.getFieldValue("id").toString()));
        service.setMark(Double.parseDouble(doc.getFieldValue("mark").toString()));
        service.setMark_amount(Integer.parseInt(doc.getFieldValue("mark_amount").toString()));
        service.setName(doc.getFieldValue("name").toString());
        service.setDescription(doc.getFieldValue("description").toString());
        service.setCategory(doc.getFieldValue("category").toString());
        service.setUser_id(Integer.parseInt(doc.getFieldValue("user_id").toString()));

        return service;
    }

    private void setDefaults(SolrQuery query, Optional<Integer> amount, Optional<Integer> start){
        if(amount.isPresent() && amount.get() > 0)
            query.setRows(amount.get());
        else
            query.setRows(Integer.MAX_VALUE);

        if(start.isPresent() && start.get() > 0)
            query.setStart(start.get());
        else
            query.setStart(0);
    }

    private Iterable<Service> getByQuery(HttpSolrClient solr, SolrQuery query){
        try {
            LinkedList<Service> result = new LinkedList<>();
            QueryResponse response = solr.query(query);

            SolrDocumentList docList = response.getResults();

            for (SolrDocument doc : docList) {
                Service service = fillService(doc);
                result.add(service);
            }

            return result;
        } catch (SolrServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private HttpSolrClient getSolrClient(){
        String urlString = "http://localhost:8983/solr/services";
        HttpSolrClient solr = new HttpSolrClient.Builder(urlString).build();
        return solr;
    }
}
