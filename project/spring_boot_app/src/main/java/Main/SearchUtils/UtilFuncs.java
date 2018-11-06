package Main.SearchUtils;

import Main.Entity.Service;
import Main.Constants.*;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Optional;

public class UtilFuncs {
    public static Service fillService(SolrDocument doc) {
        Service service = new Service();
        service.setId(Integer.parseInt(doc.getFieldValue(StringConstants.Id).toString()));
        service.setMark(Double.parseDouble(doc.getFieldValue(StringConstants.Mark).toString()));
        service.setMark_amount(Integer.parseInt(doc.getFieldValue(StringConstants.MarkAmount).toString()));
        service.setName(doc.getFieldValue(StringConstants.Name).toString());
        service.setDescription(doc.getFieldValue(StringConstants.Description).toString());
        service.setCategory(doc.getFieldValue(StringConstants.Category).toString());
        service.setUser_id(Integer.parseInt(doc.getFieldValue(StringConstants.UserId).toString()));

        return service;
    }

    public static SolrInputDocument fillService(Service service) {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField(StringConstants.Id, service.getId());
        doc.addField(StringConstants.Mark, service.getMark());
        doc.addField(StringConstants.MarkAmount, service.getMark_amount());
        doc.addField(StringConstants.Name, service.getName());
        doc.addField(StringConstants.Category, service.getCategory());
        doc.addField(StringConstants.Description, service.getDescription());
        doc.addField(StringConstants.UserId, service.getUser_id());
        doc.addField(StringConstants.LastModified, service.getLast_modified());

        return doc;
    }

    public static void setDefaults(SolrQuery query, Optional<Integer> amount, Optional<Integer> start){
        if(amount.isPresent() && amount.get() > 0)
            query.setRows(amount.get());
        else
            query.setRows(100);

        if(start.isPresent() && start.get() > 0)
            query.setStart(start.get());
        else
            query.setStart(0);
    }

    public static Iterable<Service> getByQuery(HttpSolrClient solr, SolrQuery query){
        try {
            LinkedList<Service> result = new LinkedList<>();
            QueryResponse response = solr.query(query);
            SpellCheckResponse spr = response.getSpellCheckResponse();
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

    public static HttpSolrClient getSolrClient(){
        String urlString = "http://localhost:8983/solr/services";
        HttpSolrClient solr = new HttpSolrClient.Builder(urlString).build();
        return solr;
    }

    public static boolean UpdateDoc(Service service)
    {
        HttpSolrClient client = UtilFuncs.getSolrClient();
        SolrInputDocument doc = fillService(service);
        try {
            final UpdateResponse updateResponse = client.add(doc);
            client.commit();
            int status = updateResponse.getStatus();
        } catch (SolrServerException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
