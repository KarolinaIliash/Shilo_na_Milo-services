package Main.Controller;

import Main.Entity.Service;
import Main.SearchUtils.*;
import Main.Constants.*;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocument;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
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

    //TODO add limits by category
    @GetMapping("/intext")
    public Answer<ResultWithSuggestion> serviceByText(@RequestParam String text,
                                           Optional<Integer> amount,
                                           Optional<Integer> start){

        HttpSolrClient solr = UtilFuncs.getSolrClient();
        SolrQuery query = new SolrQuery();
        query.setRequestHandler("/spell");

        text = text.toLowerCase();
        //TODO think about digits in regex
        String[] words = text.split("[^a-z]+");

        if(words.length > 1) {
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
            if(!result.getResult().isEmpty()){
                return new Answer<>(Codes.OK, result);
            }
            builder = new StringBuilder();
            for (String word : words){
                builder.append("*" + word + "* ");
            }
            // delete last whitespace
            builder.deleteCharAt(builder.length() - 1);
            queryText = builder.toString();
            query.setRequestHandler("/select");
            query.set("q", queryText);
            List<Service> services = UtilFuncs.getByQuery(solr, query);
            if(!services.isEmpty() || result.getSuggestion().isEmpty()) {
                return new Answer<>(Codes.OK,
                        new ResultWithSuggestion(services, result.getSuggestion()));
            }

            // get the best suggestion
            String collational = result.getSuggestion().get(0);
            String[] collationalWords = collational.split("//s+");
            builder = new StringBuilder();
            for (String word : collationalWords){
                builder.append("*" + word + "* ");
            }
            // delete last whitespace
            builder.deleteCharAt(builder.length() - 1);

            queryText = builder.toString();
            query.set("q", queryText);
            services = UtilFuncs.getByQuery(solr, query);
            return new Answer<>(Codes.OK,
                    new ResultWithSuggestion(services, result.getSuggestion()));
        }
        else{
            query.set("q", "*" + text + "*");

            UtilFuncs.setDefaults(query, amount, start);
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
}
