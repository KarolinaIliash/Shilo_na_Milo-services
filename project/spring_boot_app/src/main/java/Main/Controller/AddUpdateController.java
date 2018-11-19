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
        Date date = new Date();
        newService.setLast_modified(date);

        newService = serviceRepository.save(newService);

        UtilFuncs.UpdateDoc(newService);

        return new Answer<>(Codes.OK, newService);
    }

    @PostMapping("/update_service")
    public Answer<Service> replaceService(@RequestBody Service newService) {

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

    @PostMapping("/add_mark/{id}/{mark}")
    public Answer<Service> addMark(@PathVariable Integer id, @PathVariable double mark) {
        Service result_service = serviceRepository.findById(id)
                .map(service -> {
                    service.setMark_amount(service.getMark_amount() + 1);
                    service.setMark((mark + service.getMark()) / service.getMark_amount());
                    service.setLast_modified(new Date());
                    return serviceRepository.save(service);
                })
                .orElseGet(()-> {return null;});

        UtilFuncs.UpdateDoc(result_service);

        return new Answer<>(Codes.OK, result_service);
    }

    @PutMapping("/delete/{id}")
    public EmptyAnswer delete(@PathVariable Integer id) {
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
}
