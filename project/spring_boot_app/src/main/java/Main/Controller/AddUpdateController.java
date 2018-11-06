package Main.Controller;

import Main.Constants.Codes;
import Main.Entity.Service;
import Main.Repository.ServiceRepository;
import Main.SearchUtils.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
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
        return new Answer<>(Codes.OK, serviceRepository.save(newService));
    }

    @PostMapping("/update_service")
    public Answer<Service> replaceService(@RequestBody Service newService) {

        return new Answer<>(Codes.OK,
                serviceRepository.findById(newService.getId())
                .map(service -> {
                    service.setName(newService.getName());
                    service.setDescription(newService.getDescription());
                    service.setCategory(newService.getCategory());
                    service.setMark(newService.getMark());
                    service.setMark_amount(newService.getMark_amount());
                    service.setLast_modified(new Date());
                    return serviceRepository.save(service);
                })
                .orElseGet(() -> {
                    newService.setLast_modified(new Date());
                    return serviceRepository.save(newService);
                }));
    }

    @PostMapping("/add_mark/{id}/{mark}")
    public Answer<Service> addMark(@PathVariable Integer id, @PathVariable double mark) {
        return new Answer<>(Codes.OK,
                serviceRepository.findById(id)
                .map(service -> {
                    service.setMark_amount(service.getMark_amount() + 1);
                    service.setMark((mark + service.getMark()) / service.getMark_amount());
                    service.setLast_modified(new Date());
                    return serviceRepository.save(service);
                })
                .orElseGet(()-> {return null;}));
    }
}
