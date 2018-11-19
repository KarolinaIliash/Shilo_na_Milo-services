package Main.Deploy;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeployRequests {
    @GetMapping("/health")
    public HttpStatus testHealth() {
        return HttpStatus.OK;
    }

    @GetMapping("/liveness")
    public HttpStatus testLiveness() {
        return HttpStatus.OK;
    }
}
