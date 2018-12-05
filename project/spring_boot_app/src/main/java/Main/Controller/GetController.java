package Main.Controller;

import Main.Entity.Service;
import Main.SearchUtils.*;
import Main.Constants.*;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.apache.solr.common.SolrDocument;

import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping(path="/services")
public class GetController {
    @GetMapping("/all")
    public Iterable<Service> getAllServices(@RequestParam(required=false) Integer amount,
                                            @RequestParam(required=false) Integer start,
                                            @RequestParam(required=false) String fieldToSort,
                                            @RequestParam(required=false) Boolean asc) {
        try {
            HttpSolrClient solr = UtilFuncs.getSolrClient();

            SolrQuery query = new SolrQuery();
            query.set("q", "*:*");

            UtilFuncs.setSort(query, fieldToSort, asc);

            UtilFuncs.setDefaults(query, amount, start);

            return UtilFuncs.getByQuery(solr, query);
        }
        catch (Exception e){
            Service error = new Service();
            error.setName("error");
            error.setDescription(e.getMessage());
            List<Service> l = new ArrayList<>();
            l.add(error);
            return l;
        }
    }

    @GetMapping("/id")
    public Answer<Service> getServiceById(@RequestParam Integer id){
        Service result = null;
        try {
            HttpSolrClient solr = UtilFuncs.getSolrClient();

            // try {
            SolrDocument doc = solr.getById(id.toString());
            if (doc == null || doc.isEmpty())
                return null;
            result = UtilFuncs.fillService(doc);
            //} catch (SolrServerException e) {
            //    e.printStackTrace();
            //} catch (IOException e) {
            //    e.printStackTrace();
            //}

            return new Answer<Service>(Codes.OK, result);
        }
        catch (Exception e){
            Service error = new Service();
            error.setName("error");
            error.setDescription(e.getMessage());
            return new Answer<>(Codes.InternalServerError, error);
        }
    }

    @GetMapping("/category")
    public Answer<Iterable<Service>> getServicesInCategory(@RequestParam String category,
                                                   @RequestParam(required=false) Integer amount,
                                                   @RequestParam(required=false) Integer start,
                                                   @RequestParam(required=false) String fieldToSort,
                                                   @RequestParam(required=false) Boolean asc){
        try {
            HttpSolrClient solr = UtilFuncs.getSolrClient();

            SolrQuery query = new SolrQuery();
            query.set("q", "category:" + category);
            UtilFuncs.setSort(query, fieldToSort, asc);

            UtilFuncs.setDefaults(query, amount, start);

            return new Answer<>(Codes.OK, UtilFuncs.getByQuery(solr, query));
        }
        catch (Exception e){
            Service error = new Service();
            error.setName("error");
            error.setDescription(e.getMessage());
            List<Service> l = new ArrayList<>();
            l.add(error);
            return new Answer<>(Codes.InternalServerError, l);
        }
    }

    @GetMapping("/user")
    public Answer<Iterable<Service>> getServicesByUser(@RequestParam String user,
                                                       @RequestParam(required=false) Integer amount,
                                                       @RequestParam(required=false) Integer start,
                                                       @RequestParam(required=false) String fieldToSort,
                                                       @RequestParam(required=false) Boolean asc) {
        try {
            HttpSolrClient solr = UtilFuncs.getSolrClient();

            SolrQuery query = new SolrQuery();
            query.set("q", "user_id:" + user);

            UtilFuncs.setDefaults(query, amount, start);
            UtilFuncs.setSort(query, fieldToSort, asc);

            return new Answer<>(Codes.OK, UtilFuncs.getByQuery(solr, query));
        }
        catch (Exception e){
            Service error = new Service();
            error.setName("error");
            error.setDescription(e.getMessage());
            List<Service> l = new ArrayList<>();
            l.add(error);
            return new Answer<>(Codes.InternalServerError, l);
        }
    }

    @GetMapping("/filter")
    public Answer<ResultWithSuggestion> serviceByText(@RequestParam(required=false) String text,
                                                      @RequestParam(required=false) Integer amount,
                                                      @RequestParam(required=false) Integer start,
                                                      @RequestParam(required=false) Double mark,
                                                      @RequestParam(required=false) Double priceFrom,
                                                      @RequestParam(required=false) Double priceTo,
                                                      @RequestParam(required=false) List<String> category,
                                                      @RequestParam(required=false) String fieldToSort,
                                                      @RequestParam(required=false) Boolean asc){
        try {
            HttpSolrClient solr = UtilFuncs.getSolrClient();
            SolrQuery query = new SolrQuery();
            query.setRequestHandler("/spell");

            UtilFuncs.setDefaults(query, amount, start);
            UtilFuncs.setSort(query, fieldToSort, asc);
            UtilFuncs.AddParametersToQuery(query, mark, priceFrom, priceTo, category);
            if (text == null)
                text = "";
            text = text.toLowerCase();
            //TODO think about digits in regex
            String[] words = text.split("[^a-z]+");

            if (words.length > 1) {
                // TODO maybe firstly search with full phrase then with 'and'
                // and then what we have now
                StringBuilder builder = new StringBuilder();
                for (String word : words) {
                    builder.append(word + " ");
                }
                // delete last whitespace
                builder.deleteCharAt(builder.length() - 1);

                String queryText = builder.toString();
                query.set("q", queryText);
                ResultWithSuggestion result = UtilFuncs.getByQueryWithSuggestion(solr, query);
                // return if we have some results
                if (!result.getResult().isEmpty()) {
                    return new Answer<>(Codes.OK, result);
                }
                builder = new StringBuilder();
                for (String word : words) {
                    builder.append("*" + word + "* ");
                }
                // delete last whitespace
                builder.deleteCharAt(builder.length() - 1);
                queryText = builder.toString();
                query.setRequestHandler("/select");
                query.set("q", queryText);
                List<Service> services = UtilFuncs.getByQuery(solr, query);
                if (!services.isEmpty() || result.getSuggestion().isEmpty()) {
                    return new Answer<>(Codes.OK,
                            new ResultWithSuggestion(services, result.getSuggestion()));
                }

                // get the best suggestion
                String collational = result.getSuggestion().get(0);
                String[] collationalWords = collational.split("//s+");
                builder = new StringBuilder();
                for (String word : collationalWords) {
                    builder.append("*" + word + "* ");
                }
                // delete last whitespace
                builder.deleteCharAt(builder.length() - 1);

                queryText = builder.toString();
                query.set("q", queryText);
                services = UtilFuncs.getByQuery(solr, query);
                return new Answer<>(Codes.OK,
                        new ResultWithSuggestion(services, result.getSuggestion()));
            } else if (words.length == 1 && words[0] != "") {
                query.set("q", "*" + text + "*");

                ResultWithSuggestion result = UtilFuncs.getByQueryWithSuggestion(solr, query);
                if (result.getResult().isEmpty() && !result.getSuggestion().isEmpty()) {
                    query.setRequestHandler("/spell");
                    // collational has full query text
                    query.set("q", result.getSuggestion().get(0));
                    List<Service> additionalRes = UtilFuncs.getByQuery(solr, query);
                    return new Answer<>(Codes.OK,
                            new ResultWithSuggestion(additionalRes, result.getSuggestion()));
                } else {
                    return new Answer<>(Codes.OK, result);
                }
            } else {
                query.set("q", "*:*");
                ResultWithSuggestion result = UtilFuncs.getByQueryWithSuggestion(solr, query);
                return new Answer<>(Codes.OK, result);
            }
        }
        catch (Exception e){
            Service error = new Service();
            error.setName("error");
            error.setDescription(e.getMessage());
            List<Service> l = new ArrayList<>();
            l.add(error);
            ResultWithSuggestion r = new ResultWithSuggestion(l, new ArrayList<>());
            return new Answer<>(Codes.InternalServerError, r);
        }
    }

    @GetMapping("/suggest")
    public Answer<List<String>> serviceByText(@RequestParam String text){
        try {
            HttpSolrClient solr = UtilFuncs.getSolrClient();
            SolrQuery query = new SolrQuery();
            query.setRequestHandler("/suggest");

            text = text.toLowerCase();
            query.set("q", text);
            //try {
            List<String> result = new LinkedList<>();
            QueryResponse response = solr.query(query);
            SpellCheckResponse suggesterResponse = response.getSpellCheckResponse();
            List<SpellCheckResponse.Collation> collationResult = suggesterResponse.getCollatedResults();
            collationResult.sort(new Comparator<SpellCheckResponse.Collation>() {
                @Override
                public int compare(SpellCheckResponse.Collation o1, SpellCheckResponse.Collation o2) {
                    return Long.compare(o2.getNumberOfHits(), o1.getNumberOfHits());
                }
            });

            for (SpellCheckResponse.Collation collation : collationResult) {
                result.add(collation.getCollationQueryString());
            }
            return new Answer<>(Codes.OK, result);
            //} catch (SolrServerException e) {
            //    e.printStackTrace();
            //} catch (IOException e) {
            //    e.printStackTrace();
            //}
            //return null;
        }
        catch (Exception e){
            //Service error = new Service();
            //error.setName("error");
            //error.setDescription(e.getMessage());
            List<String> l = new ArrayList<>();
            l.add(e.getMessage());
            return new Answer<>(Codes.InternalServerError, l);
        }
    }
}
