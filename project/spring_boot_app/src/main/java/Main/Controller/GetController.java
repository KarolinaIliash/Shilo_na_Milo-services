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
                                            @RequestParam(required=false) Integer start) {
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
                                                   @RequestParam(required=false) Integer amount,
                                                   @RequestParam(required=false) Integer start){
        HttpSolrClient solr = UtilFuncs.getSolrClient();

        SolrQuery query = new SolrQuery();
        query.set("q", "category:" + category);

        UtilFuncs.setDefaults(query, amount, start);

        return new Answer<>(Codes.OK, UtilFuncs.getByQuery(solr, query));
    }

    @GetMapping("/user")
    public Answer<Iterable<Service>> getServicesByUser(@RequestParam Integer user,
                                                       @RequestParam(required=false) Integer amount,
                                                       @RequestParam(required=false) Integer start) {
        HttpSolrClient solr = UtilFuncs.getSolrClient();

        SolrQuery query = new SolrQuery();
        query.set("q", "user_id:" + user.toString());

        UtilFuncs.setDefaults(query, amount, start);

        return new Answer<>(Codes.OK, UtilFuncs.getByQuery(solr, query));
    }

    //TODO add limits by category
    @GetMapping("/intext")
    public Answer<ResultWithSuggestion> serviceByText(@RequestParam String text,
                                                      @RequestParam(required=false) Integer amount,
                                                      @RequestParam(required=false) Integer start,
                                                      @RequestParam(required=false) Double mark,
                                                      @RequestParam(required=false) Double priceFrom,
                                                      @RequestParam(required=false) Double priceTo,
                                                      @RequestParam(required=false) String category){
        HttpSolrClient solr = UtilFuncs.getSolrClient();
        SolrQuery query = new SolrQuery();
        query.setRequestHandler("/spell");

        UtilFuncs.setDefaults(query, amount, start);
        UtilFuncs.AddParametersToQuery(query, mark, priceFrom, priceTo, category);
        text = text.toLowerCase();
        //TODO think about digits in regex
        String[] words = text.split("[^a-z]+");

        if(words.length > 1) {
            // TODO maybe firstly search with full phrase then with 'and'
            // and then what we have now
            query.set("q", toDelimetedString(words, "", " "));
            ResultWithSuggestion result = UtilFuncs.getByQueryWithSuggestion(solr, query);
            // return if we have some results
            if(!result.getResult().isEmpty()){
                return new Answer<>(Codes.OK, result);
            }
            
            query.setRequestHandler("/select");
            query.set("q", toDelimetedString(words, "*", "* "));
            List<Service> services = UtilFuncs.getByQuery(solr, query);
            if(!services.isEmpty() || result.getSuggestion().isEmpty()) {
                return new Answer<>(Codes.OK,
                        new ResultWithSuggestion(services, result.getSuggestion()));
            }

            // get the best suggestion
            words = result.getSuggestion().get(0).split("//s+");
            query.set("q", toDelimetedString(words, "*", "* "));
            services = UtilFuncs.getByQuery(solr, query);
            return new Answer<>(Codes.OK,
                    new ResultWithSuggestion(services, result.getSuggestion()));
        }
        else{
            query.set("q", "*" + text + "*");

            ResultWithSuggestion result = UtilFuncs.getByQueryWithSuggestion(solr, query);
            if(result.getResult().isEmpty() && !result.getSuggestion().isEmpty()){
                SolrQuery additionalQuery = new SolrQuery();
                additionalQuery.setRequestHandler("/spell");
                // collational has full query text
                additionalQuery.set("q", result.getSuggestion().get(0));
                List<Service> additionalRes = UtilFuncs.getByQuery(solr, additionalQuery);
                return new Answer<>(Codes.OK,
                        new ResultWithSuggestion(additionalRes, result.getSuggestion()));
            }
            else{
                return new Answer<>(Codes.OK, result);
            }
        }
    }

    @GetMapping("/suggest")
    public Answer<List<String>> serviceByText(@RequestParam String text){
        HttpSolrClient solr = UtilFuncs.getSolrClient();
        SolrQuery query = new SolrQuery();
        query.setRequestHandler("/suggest");

        text = text.toLowerCase();
        query.set("q", text);
        try {
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

            for(SpellCheckResponse.Collation collation : collationResult){
                result.add(collation.getCollationQueryString());
            }
            return new Answer<>(Codes.OK, result);
        } catch (SolrServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
   
    private String toDelimetedString(String[] words, String prefix, String suffix){
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            builder.append(prefix + word + suffix);
        }
        // delete last whitespace
        builder.deleteCharAt(builder.length() - 1);
        
        return builder.toString();
    }
}
