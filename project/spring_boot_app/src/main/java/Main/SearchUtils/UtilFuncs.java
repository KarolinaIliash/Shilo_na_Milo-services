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
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

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
        service.setPrice(Double.parseDouble(doc.getFieldValue(StringConstants.Price).toString()));

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
        doc.addField(StringConstants.Price, service.getPrice());

        return doc;
    }

    public static void setDefaults(SolrQuery query, Integer amount, Integer start){
        if(amount != null && amount > 0)
            query.setRows(amount);
        else
            query.setRows(100);

        if(start != null && start > 0)
            query.setStart(start);
        else
            query.setStart(0);
    }

    public static List<Service> getByQuery(HttpSolrClient solr, SolrQuery query){
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

    public static ResultWithSuggestion getByQueryWithSuggestion(HttpSolrClient solr,
                                                                SolrQuery query){
        try {
            LinkedList<Service> result = new LinkedList<>();
            LinkedList<String> collations = new LinkedList<>();
            QueryResponse response = solr.query(query);

            SpellCheckResponse spr = response.getSpellCheckResponse();
            if(spr != null) {
                List<SpellCheckResponse.Collation> collationResult = spr.getCollatedResults();
                collationResult.sort(new Comparator<SpellCheckResponse.Collation>() {
                    @Override
                    public int compare(SpellCheckResponse.Collation o1, SpellCheckResponse.Collation o2) {
                        return Long.compare(o2.getNumberOfHits(), o1.getNumberOfHits());
                    }
                });

                for (SpellCheckResponse.Collation collation : collationResult) {
                    String colQuery = collation.getCollationQueryString();
                    if (colQuery.contains("*")) {
                        StringBuilder builder = new StringBuilder(colQuery);
                        builder.deleteCharAt(builder.length() - 1);
                        builder.deleteCharAt(0);
                        colQuery = builder.toString();
                    }
                    collations.add(colQuery);
                }
            }
            SolrDocumentList docList = response.getResults();

            for (SolrDocument doc : docList) {
                result.add(fillService(doc));
            }

            return new ResultWithSuggestion(result, collations);
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

    public static void AddParametersToQuery(SolrQuery query, Double mark,
                                            Double priceFrom, Double priceTo,
                                            String category){
        if(mark != null){
            Double from = mark - 0.5;
            Double to = mark + 0.5;
            query.addFilterQuery(StringConstants.Mark + ":[" + from.toString() + " TO " + to.toString() + "]");
        }
        if(priceFrom != null && priceTo != null){
            query.addFilterQuery(StringConstants.Price + ":[" + priceFrom.toString() + " TO "
                      + priceTo.toString() + "]");
        }
        if(category != null){
            query.addFilterQuery(StringConstants.Category + ":" + category);
        }
    }

    public static void setSort(SolrQuery query, String field, Boolean asc){
        if(field == null)
            return;
        if(asc == null)
            asc = false;
        SolrQuery.ORDER order = asc ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc;
        if(ListConstants.sortFields.contains(field.toLowerCase()))
            query.setSort(field, order);
    }
}
