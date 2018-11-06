package Main.SearchUtils;

import Main.Entity.Service;

public class ResultWithSuggestion {
    private Iterable<Service> result;
    private Iterable<String> suggestion;

    public void setResult(Iterable<Service> result_){
        result = result_;
    }
    public Iterable<Service> getResult(){
        return result;
    }
    public void setSuggestion(Iterable<String> suggestion_){
        suggestion = suggestion_;
    }
    public Iterable<String> getSuggestion(){
        return suggestion;
    }
}
