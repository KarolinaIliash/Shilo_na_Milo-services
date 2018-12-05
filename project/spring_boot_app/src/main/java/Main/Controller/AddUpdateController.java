package Main.Controller;

import Main.Constants.Codes;
import Main.Entity.Service;
import Main.Repository.ServiceRepository;
import Main.SearchUtils.Answer;
import Main.SearchUtils.EmptyAnswer;
import Main.SearchUtils.UtilFuncs;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Date;


@RestController
@RequestMapping(path="/services")
public class AddUpdateController {
    @Autowired
    private ServiceRepository serviceRepository;

    @PostMapping("/add_service")
    public Answer<Service> addNewService(@RequestBody Service newService) {
        try {
            Date date = new Date();
            newService.setLast_modified(date);
            newService.setMark_amount(0);
            newService.setMark(0d);
            if (newService.getPrice() == null) {
                newService.setPrice(0d);
            }
            if(newService.getName() == null){
                newService.setName("");
            }
            if(newService.getCategory() == null){
                newService.setCategory("");
            }
            if(newService.getDescription() == null){
                newService.setDescription("");
            }
            if(newService.getUser_id() == null){
                newService.setUser_id("");
            }
            newService = serviceRepository.save(newService);

            UtilFuncs.UpdateDoc(newService);

            return new Answer<>(Codes.OK, newService);
        }
        catch (Exception e){
            Service errorService = new Service();
            errorService.setName("error");
            errorService.setDescription(e.getMessage());
            return new Answer<>(Codes.InternalServerError, errorService);
        }
    }

    @PostMapping("/update_service")
    public Answer<Service> replaceService(@RequestBody Service newService) {
        try {
            Service result_service =
                    serviceRepository.findById(newService.getId())
                            .map(service -> {
                                service.setName(newService.getName());
                                service.setDescription(newService.getDescription());
                                service.setCategory(newService.getCategory());
                                service.setMark(newService.getMark());
                                service.setMark_amount(newService.getMark_amount());
                                service.setPrice(newService.getPrice());
                                service.setLast_modified(new Date());
                                return serviceRepository.save(service);
                            })
                            .orElseGet(() -> {
                                newService.setLast_modified(new Date());
                                return serviceRepository.save(newService);
                            });

            UtilFuncs.UpdateDoc(result_service);

            return new Answer<>(Codes.OK, result_service);
        }
        catch (Exception e){
            Service errorService = new Service();
            errorService.setName("error");
            errorService.setDescription(e.getMessage());
            return new Answer<>(Codes.InternalServerError, errorService);
        }
    }

    @PostMapping("/add_mark/{id}/{mark}")
    public Answer<Service> addMark(@PathVariable Integer id, @PathVariable double mark) {
        try {
            Service result_service = serviceRepository.findById(id)
                    .map(service -> {
                        service.setMark_amount(service.getMark_amount() + 1);
                        service.setMark((mark + service.getMark()) / service.getMark_amount());
                        service.setLast_modified(new Date());
                        return serviceRepository.save(service);
                    })
                    .orElseGet(() -> {
                        return null;
                    });

            UtilFuncs.UpdateDoc(result_service);

            return new Answer<>(Codes.OK, result_service);
        }
        catch (Exception e){
            Service errorService = new Service();
            errorService.setName("error");
            errorService.setDescription(e.getMessage());
            return new Answer<>(Codes.InternalServerError, errorService);
        }
    }

    @PutMapping("/delete/{id}")
    public EmptyAnswer delete(@PathVariable Integer id) {
        try {
            serviceRepository.deleteById(id);

            boolean ok = true;

            HttpSolrClient client = UtilFuncs.getSolrClient();
            try {
                client.deleteById(id.toString());
                client.commit();
            } catch (SolrServerException e) {
                e.printStackTrace();
                ok = false;
            } catch (IOException e) {
                e.printStackTrace();
                ok = false;
            }
            //TODO implement bad codes support
            //Integer code = ok ? Codes.OK : Codes.

            return new EmptyAnswer(Codes.OK);
        }
        catch (Exception e){
            return new EmptyAnswer(Codes.InternalServerError);
        }
    }
}
