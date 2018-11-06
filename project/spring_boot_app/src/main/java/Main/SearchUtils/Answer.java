package Main.SearchUtils;

public class Answer<ResultType> extends EmptyAnswer {
    private ResultType result;

    public Answer(Integer code_, ResultType result_){
        super(code_);
        result = result_;
    }

    public void setResult(ResultType result_){
        result = result_;
    }
    public ResultType getResult(){
        return result;
    }
}
