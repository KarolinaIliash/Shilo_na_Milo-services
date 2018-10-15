package Main.Entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "services_table")
public class Service {
    @Id
    //@GeneratedValue(strategy=GenerationType.AUTO)
    @Column(columnDefinition = "serial")
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Integer id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer user_id;

    private String category;

    private Double mark;

    private Integer mark_amount;

    @Temporal(value = TemporalType.TIMESTAMP)
    private Date last_modified;

    public void setId(Integer id_) {
        id = id_;
    }
    public Integer getId() {
        return  id;
    }
    public void setName(String name_) {
        name = name_;
    }
    public String getName(){
        return name;
    }
    public void setDescription(String description_) {
        description = description_;
    }
    public String getDescription(){
        return description;
    }
    public void setUser_id(Integer user_id_) {
        user_id = user_id_;
    }
    public Integer getUser_id(){
        return user_id;
    }
    public void setCategory(String category_){
        category = category_;
    }
    public String getCategory(){
        return category;
    }
    public void setMark(Double mark_)
    {
        mark = mark_;
    }
    public Double getMark(){
        return mark;
    }
    public void setMark_amount(Integer amount)
    {
        mark_amount = amount;
    }
    public Integer getMark_amount(){
        return mark_amount;
    }
    public void setLast_modified(Date date){
        last_modified = date;
    }
    public Date getLast_modified(){
        return last_modified;
    }
}
