package Main.SearchUtils;

import Main.Entity.Service;

import java.util.List;

public class ResultWithSuggestion {
    private List<Service> result;
    private List<String> suggestion;

    public ResultWithSuggestion(List<Service> result_, List<String> suggestion_){
        result = result_;
        suggestion = suggestion_;
    }

    public void setResult(List<Service> result_){
        result = result_;
    }
    public List<Service> getResult(){
        return result;
    }
    public void setSuggestion(List<String> suggestion_){
        suggestion = suggestion_;
    }
    public List<String> getSuggestion(){
        return suggestion;
    }
}
